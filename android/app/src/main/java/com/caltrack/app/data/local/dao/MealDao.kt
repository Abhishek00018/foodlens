package com.caltrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.caltrack.app.data.local.entity.MealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meal: MealEntity): Long

    @Delete
    suspend fun delete(meal: MealEntity)

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM meals WHERE id = :id LIMIT 1")
    fun getMealById(id: Long): Flow<MealEntity?>

    @Query("SELECT * FROM meals WHERE id = :id LIMIT 1")
    suspend fun getMealByIdOnce(id: Long): MealEntity?

    @Query("SELECT * FROM meals WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getMealByRemoteId(remoteId: String): MealEntity?

    @Query("SELECT * FROM meals WHERE date = :date ORDER BY timestamp DESC")
    fun getMealsByDate(date: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE date BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getMealsForDateRange(startDate: String, endDate: String): Flow<List<MealEntity>>

    @Query("SELECT SUM(calories) FROM meals WHERE date = :date")
    fun getTotalCaloriesByDate(date: String): Flow<Int?>

    @Query("SELECT * FROM meals WHERE synced = 0")
    suspend fun getUnsyncedMeals(): List<MealEntity>
}
