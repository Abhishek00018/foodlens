package com.caltrack.app.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.caltrack.app.data.repository.MealRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val mealRepository: MealRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val unsynced = mealRepository.getUnsyncedMeals()
            unsynced.forEach { meal ->
                runCatching { mealRepository.logMealRemote(meal) }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
