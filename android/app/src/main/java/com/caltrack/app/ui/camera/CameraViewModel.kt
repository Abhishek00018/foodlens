package com.caltrack.app.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caltrack.app.data.local.entity.MealEntity
import com.caltrack.app.data.remote.ScanResponse
import com.caltrack.app.data.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject

sealed class ScanUiState {
    data object Idle : ScanUiState()
    data object Scanning : ScanUiState()
    data class Success(val scanResponse: ScanResponse, val localImageUri: String?) : ScanUiState()
    data object LoggingMeal : ScanUiState()
    data object MealLogged : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val mealRepository: MealRepository
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    // Dedicated OkHttpClient for direct S3 uploads — no auth interceptor, no logging of bodies.
    private val s3Client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)   // large file upload
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Full 3-step scan flow:
     *  1. Compress captured image (target ≤500KB).
     *  2. GET presigned S3 PUT URL from backend → [UploadUrlResponse].
     *  3. PUT compressed bytes directly to S3.
     *  4. POST imageKey to backend → backend reads from S3, runs Bedrock → [ScanResponse].
     */
    fun scanImage(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _scanState.update { ScanUiState.Scanning }
            try {
                // Step 1 — compress
                val (imageBytes, tempFile) = withContext(Dispatchers.IO) {
                    compressImage(context, imageUri)
                }

                // Step 2 — get presigned upload URL
                val uploadUrlResult = mealRepository.getScanUploadUrl("image/jpeg")
                val uploadInfo = uploadUrlResult.getOrElse { e ->
                    _scanState.update { ScanUiState.Error(e.message ?: "Failed to get upload URL") }
                    tempFile.delete()
                    return@launch
                }

                // Step 3 — PUT directly to S3 (no auth header, no Retrofit)
                withContext(Dispatchers.IO) {
                    uploadToS3(uploadInfo.uploadUrl, imageBytes, "image/jpeg")
                }
                tempFile.delete()

                // Step 4 — trigger backend analysis
                val scanResult = mealRepository.triggerScan(uploadInfo.imageKey)
                scanResult.onSuccess { scan ->
                    _scanState.update {
                        ScanUiState.Success(scan, imageUri.toString())
                    }
                }.onFailure { e ->
                    _scanState.update { ScanUiState.Error(e.message ?: "Analysis failed") }
                }

            } catch (e: Exception) {
                _scanState.update { ScanUiState.Error(e.message ?: "Unexpected error") }
            }
        }
    }

    /**
     * Log the scanned meal — save to Room immediately, then sync to backend.
     */
    fun logMeal(scanResponse: ScanResponse, localImageUri: String?) {
        viewModelScope.launch {
            _scanState.update { ScanUiState.LoggingMeal }
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val entity = MealEntity(
                name = scanResponse.foodName,
                calories = scanResponse.calories,
                protein = scanResponse.protein.toInt(),
                carbs = scanResponse.carbs.toInt(),
                fat = scanResponse.fat.toInt(),
                imageUri = scanResponse.imageUrl ?: localImageUri,  // prefer server presigned URL
                imageKey = scanResponse.imageKey,
                timestamp = System.currentTimeMillis(),
                date = today,
                allergenWarnings = scanResponse.allergenWarnings.joinToString(","),
                synced = false
            )
            val localId = mealRepository.insertMeal(entity)
            // Best-effort remote sync — non-blocking, failure is OK (unsynced meals sync later)
            mealRepository.logMealRemote(entity.copy(id = localId))
            _scanState.update { ScanUiState.MealLogged }
        }
    }

    fun reset() {
        _scanState.update { ScanUiState.Idle }
    }

    fun clearError() {
        _scanState.update { ScanUiState.Idle }
    }

    // ─── Image compression ────────────────────────────────────────────────────

    /**
     * Compress the image URI to JPEG bytes.
     * Target: ≤500KB. Max input size accepted: 10MB.
     * Returns the compressed bytes and a temp file (caller must delete it).
     */
    private fun compressImage(context: Context, uri: Uri): Pair<ByteArray, File> {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open image URI")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val scaled = scaleBitmap(bitmap, maxDimension = 1280)

        // Compress to ByteArray, reduce quality until ≤500KB
        val out = ByteArrayOutputStream()
        var quality = 90
        do {
            out.reset()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            quality -= 10
        } while (out.size() > 500_000 && quality > 10)

        val bytes = out.toByteArray()

        // Write to cache for temp file reference (deleted after S3 upload)
        val tempFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
        FileOutputStream(tempFile).use { it.write(bytes) }

        return bytes to tempFile
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDimension && h <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
    }

    // ─── Direct S3 upload ─────────────────────────────────────────────────────

    /**
     * PUT [imageBytes] directly to the S3 presigned [uploadUrl].
     * Must NOT use the Retrofit client (which adds our Auth header — S3 rejects extra headers).
     * Throws [IOException] on non-2xx response.
     */
    private fun uploadToS3(uploadUrl: String, imageBytes: ByteArray, contentType: String) {
        val body = imageBytes.toRequestBody(contentType.toMediaType())
        val request = Request.Builder()
            .url(uploadUrl)
            .put(body)
            .header("Content-Type", contentType)
            .build()
        val response = s3Client.newCall(request).execute()
        response.use {
            if (!it.isSuccessful) {
                throw IOException("S3 upload failed with HTTP ${it.code}")
            }
        }
    }
}
