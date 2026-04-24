package com.caltrack.app.data.repository

import com.caltrack.app.data.local.dao.DailyGoalDao
import com.caltrack.app.data.local.dao.MealDao
import com.caltrack.app.data.local.entity.DailyGoalEntity
import com.caltrack.app.data.local.entity.MealEntity
import com.caltrack.app.data.remote.CaltrackApi
import com.caltrack.app.data.remote.MealRequest
import com.caltrack.app.data.remote.MealResponse
import com.caltrack.app.data.remote.ScanRequest
import com.caltrack.app.data.remote.ScanResponse
import com.caltrack.app.data.remote.UploadUrlResponse
import com.caltrack.app.data.remote.WeeklyResponse
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealRepository @Inject constructor(
    private val mealDao: MealDao,
    private val dailyGoalDao: DailyGoalDao,
    private val api: CaltrackApi
) {

    // ─── Local (offline-first) ────────────────────────────────────────────────

    fun getMealsByDate(date: String): Flow<List<MealEntity>> =
        mealDao.getMealsByDate(date)

    fun getMealById(id: Long): Flow<MealEntity?> =
        mealDao.getMealById(id)

    suspend fun getMealByLocalId(id: Long): MealEntity? =
        mealDao.getMealByIdOnce(id)

    fun getMealsForDateRange(startDate: String, endDate: String): Flow<List<MealEntity>> =
        mealDao.getMealsForDateRange(startDate, endDate)

    fun getTotalCaloriesByDate(date: String): Flow<Int?> =
        mealDao.getTotalCaloriesByDate(date)

    suspend fun insertMeal(meal: MealEntity): Long =
        mealDao.insert(meal)

    suspend fun deleteMeal(meal: MealEntity) =
        mealDao.delete(meal)

    suspend fun deleteMealById(id: Long) =
        mealDao.deleteById(id)

    // Goals
    fun getGoal(): Flow<DailyGoalEntity?> =
        dailyGoalDao.getGoal()

    suspend fun insertGoal(goal: DailyGoalEntity): Long =
        dailyGoalDao.insert(goal)

    suspend fun updateGoal(goal: DailyGoalEntity) =
        dailyGoalDao.update(goal)

    suspend fun getUnsyncedMeals(): List<MealEntity> = mealDao.getUnsyncedMeals()

    // ─── Scan: Step 1 ─────────────────────────────────────────────────────────

    /**
     * Request a presigned S3 PUT URL from the backend.
     * Returns [UploadUrlResponse] containing the uploadUrl and the imageKey to reuse in Step 3.
     */
    suspend fun getScanUploadUrl(contentType: String = "image/jpeg"): Result<UploadUrlResponse> {
        return try {
            val response = api.getScanUploadUrl(contentType)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val msg = response.body()?.error?.message ?: "Failed to get upload URL (${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Scan: Step 3 ─────────────────────────────────────────────────────────

    /**
     * Tell the backend to read the already-uploaded image from S3, run Bedrock analysis,
     * and return the scan result. Call this after the direct S3 PUT succeeds.
     */
    suspend fun triggerScan(imageKey: String, contentType: String = "image/jpeg"): Result<ScanResponse> {
        return try {
            val response = api.triggerScan(ScanRequest(imageKey, contentType))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val msg = response.body()?.error?.message ?: "Scan failed (${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Meal CRUD ────────────────────────────────────────────────────────────

    /** Log a meal on the backend and mark it synced in Room. */
    suspend fun logMealRemote(meal: MealEntity): Result<MealResponse> {
        return try {
            val request = MealRequest(
                name = meal.name,
                calories = meal.calories.toDouble(),
                protein = meal.protein.toDouble(),
                carbs = meal.carbs.toDouble(),
                fat = meal.fat.toDouble(),
                imageKey = meal.imageKey,
                date = meal.date,
                mealTime = meal.mealTime
            )
            val response = api.logMeal(request)
            if (response.isSuccessful && response.body()?.data != null) {
                val remote = response.body()!!.data!!
                mealDao.insert(meal.copy(remoteId = remote.id, synced = true))
                Result.success(remote)
            } else {
                val msg = response.body()?.error?.message ?: "Log failed (${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Delete meal from backend then remove from Room. */
    suspend fun deleteMealRemote(meal: MealEntity): Result<Unit> {
        return try {
            if (meal.remoteId != null) {
                val response = api.deleteMeal(meal.remoteId)
                if (!response.isSuccessful) {
                    val msg = response.body()?.error?.message ?: "Delete failed (${response.code()})"
                    return Result.failure(Exception(msg))
                }
            }
            mealDao.delete(meal)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Pull meals for a date from the backend and upsert into Room. */
    suspend fun syncMealsForDate(date: String): Result<List<MealResponse>> {
        return try {
            val response = api.getMealsByDate(date)
            if (response.isSuccessful && response.body()?.data != null) {
                val remoteMeals = response.body()!!.data!!
                remoteMeals.forEach { remote ->
                    val existing = mealDao.getMealByRemoteId(remote.id)
                    mealDao.insert(remote.toEntity(existing?.id ?: 0))
                }
                Result.success(remoteMeals)
            } else {
                val msg = response.body()?.error?.message ?: "Sync failed (${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchWeeklySummary(): Result<WeeklyResponse> {
        return try {
            val response = api.getWeeklySummary()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val msg = response.body()?.error?.message ?: "Fetch failed (${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Map a server [MealResponse] to a local [MealEntity].
     * [imageUrl] is a short-lived presigned URL — stored in [MealEntity.imageUri] so Coil
     * can load it within the current session. It will be null after expiry (5 min), at
     * which point the placeholder shows until the next sync refreshes it.
     */
    private fun MealResponse.toEntity(localId: Long): MealEntity = MealEntity(
        id = localId,
        remoteId = id,
        name = name,
        calories = totalCalories.toInt(),
        protein = totalProtein.toInt(),
        carbs = totalCarbs.toInt(),
        fat = totalFat.toInt(),
        imageUri = imageUrl,   // presigned GET URL — valid for ~5 min, used by Coil
        imageKey = imageKey,   // permanent S3 key — used for future URL generation
        timestamp = System.currentTimeMillis(),
        date = mealDate,
        mealTime = mealTime,
        allergenWarnings = allergenWarnings.joinToString(","),
        synced = true
    )
}
