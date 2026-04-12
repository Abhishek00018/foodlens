package com.caltrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: String? = null,    // UUID from server; null until synced
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val imageUri: String? = null,    // local camera file path
    val imageKey: String? = null,    // S3 key from backend
    val timestamp: Long,
    val date: String,                // "2026-04-11"
    val mealTime: String? = null,    // e.g. "BREAKFAST"
    val allergenWarnings: String = "",  // comma-separated allergen names
    val synced: Boolean = false
)
