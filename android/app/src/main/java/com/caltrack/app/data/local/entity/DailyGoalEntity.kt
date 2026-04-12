package com.caltrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_goals")
data class DailyGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val calorieGoal: Int = 2000,
    val proteinGoal: Int = 150,
    val carbsGoal: Int = 250,
    val fatGoal: Int = 65
)
