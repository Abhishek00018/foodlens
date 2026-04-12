package com.caltrack.app.ui.camera

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.caltrack.app.ui.theme.NeonLime
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onPhotoTaken: (Uri) -> Unit = {}
) {
    var hasCameraPermission by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            CameraContent(
                isCapturing = isCapturing,
                onCapture = { isCapturing = true },
                onCaptured = { uri ->
                    isCapturing = false
                    onPhotoTaken(uri)
                },
                onCaptureFailed = { isCapturing = false },
                onBack = onBack
            )
        } else {
            PermissionDeniedView(onBack = onBack)
        }
    }
}

@Composable
private fun CameraContent(
    isCapturing: Boolean,
    onCapture: () -> Unit,
    onCaptured: (Uri) -> Unit,
    onCaptureFailed: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = surfaceProvider
                        }
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Scan overlay
        FoodScanOverlay()

        // Top bar with glassmorphism effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
                .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "Scan Meal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Flash toggle placeholder
                IconButton(
                    onClick = { /* TODO: toggle flash */ },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Bottom controls area
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(bottom = 40.dp, top = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hint text
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Position your meal in the frame",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Bottom row: Gallery — Capture — spacer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery button
                    IconButton(
                        onClick = { /* TODO: open gallery picker */ },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // CAPTURE BUTTON — the hero element
                    CaptureButton(
                        isCapturing = isCapturing,
                        onClick = {
                            onCapture()
                            takePhoto(context, imageCapture, onCaptured, onCaptureFailed)
                        }
                    )

                    // Spacer for symmetry
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}

@Composable
private fun CaptureButton(
    isCapturing: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(100),
        label = "capture-scale"
    )

    // Pulsing glow animation when not capturing
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow-alpha"
    )

    Box(
        modifier = Modifier
            .size(84.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow ring
        if (!isCapturing) {
            Canvas(modifier = Modifier.size(84.dp)) {
                drawCircle(
                    color = Color(0xFF80FF00).copy(alpha = glowAlpha * 0.4f),
                    radius = size.minDimension / 2,
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }

        // Outer ring
        Box(
            modifier = Modifier
                .size(76.dp)
                .border(
                    width = 3.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF80FF00),
                            Color(0xFF40CC00),
                            Color(0xFF80FF00),
                            Color(0xFFB0FF60),
                            Color(0xFF80FF00),
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCapturing) {
                // Loading spinner
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = NeonLime,
                    strokeWidth = 3.dp
                )
            } else {
                // Inner filled button
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = NeonLime.copy(alpha = 0.3f),
                            spotColor = NeonLime.copy(alpha = 0.5f)
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFB0FF60),
                                    Color(0xFF80FF00),
                                    Color(0xFF60CC00)
                                )
                            )
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    // Camera icon in the center
                    Canvas(modifier = Modifier.size(28.dp)) {
                        val c = size.width / 2
                        val color = Color(0xFF0D0D14)
                        // Camera body
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(2.dp.toPx(), 6.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(24.dp.toPx(), 18.dp.toPx()),
                            cornerRadius = CornerRadius(3.dp.toPx())
                        )
                        // Camera lens
                        drawCircle(
                            color = Color(0xFF80FF00),
                            radius = 5.dp.toPx(),
                            center = Offset(c, 15.dp.toPx())
                        )
                        drawCircle(
                            color = color,
                            radius = 3.dp.toPx(),
                            center = Offset(c, 15.dp.toPx())
                        )
                        // Flash bump
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(9.dp.toPx(), 3.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(10.dp.toPx(), 5.dp.toPx()),
                            cornerRadius = CornerRadius(1.5f.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodScanOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val overlayColor = Color.Black.copy(alpha = 0.45f)
        val frameW = size.width * 0.78f
        val frameH = size.height * 0.38f
        val frameLeft = (size.width - frameW) / 2
        val frameTop = (size.height - frameH) / 2 - 40.dp.toPx()
        val cornerR = 24.dp.toPx()

        val cutoutPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(frameLeft, frameTop, frameLeft + frameW, frameTop + frameH),
                    cornerRadius = CornerRadius(cornerR)
                )
            )
        }

        clipPath(cutoutPath, clipOp = ClipOp.Difference) {
            drawRect(overlayColor)
        }

        // Corner brackets
        val bracketLen = 36.dp.toPx()
        val strokeW = 3.5.dp.toPx()
        val limeColor = Color(0xFF80FF00)

        // Top-left
        drawLine(limeColor, Offset(frameLeft, frameTop + cornerR), Offset(frameLeft, frameTop + bracketLen + cornerR), strokeW)
        drawLine(limeColor, Offset(frameLeft + cornerR, frameTop), Offset(frameLeft + bracketLen + cornerR, frameTop), strokeW)

        // Top-right
        drawLine(limeColor, Offset(frameLeft + frameW, frameTop + cornerR), Offset(frameLeft + frameW, frameTop + bracketLen + cornerR), strokeW)
        drawLine(limeColor, Offset(frameLeft + frameW - cornerR, frameTop), Offset(frameLeft + frameW - bracketLen - cornerR, frameTop), strokeW)

        // Bottom-left
        drawLine(limeColor, Offset(frameLeft, frameTop + frameH - cornerR), Offset(frameLeft, frameTop + frameH - bracketLen - cornerR), strokeW)
        drawLine(limeColor, Offset(frameLeft + cornerR, frameTop + frameH), Offset(frameLeft + bracketLen + cornerR, frameTop + frameH), strokeW)

        // Bottom-right
        drawLine(limeColor, Offset(frameLeft + frameW, frameTop + frameH - cornerR), Offset(frameLeft + frameW, frameTop + frameH - bracketLen - cornerR), strokeW)
        drawLine(limeColor, Offset(frameLeft + frameW - cornerR, frameTop + frameH), Offset(frameLeft + frameW - bracketLen - cornerR, frameTop + frameH), strokeW)
    }
}

@Composable
private fun PermissionDeniedView(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Camera permission required",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Please grant camera access in settings to scan meals",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        androidx.compose.material3.OutlinedButton(onClick = onBack) {
            Text("Go Back")
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onSuccess: (Uri) -> Unit,
    onFailure: () -> Unit
) {
    val photoFile = File(
        context.cacheDir,
        "meal_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
    )
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onSuccess(Uri.fromFile(photoFile))
            }

            override fun onError(exception: ImageCaptureException) {
                onFailure()
            }
        }
    )
}
