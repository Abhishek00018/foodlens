package com.caltrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.caltrack.app.data.local.entity.DailyGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: DailyGoalEntity): Long

    @Update
    suspend fun update(goal: DailyGoalEntity)

    @Query("SELECT * FROM daily_goals LIMIT 1")
    fun getGoal(): Flow<DailyGoalEntity?>
}
