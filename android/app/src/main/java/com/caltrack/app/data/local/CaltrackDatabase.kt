package com.caltrack.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.caltrack.app.data.local.dao.DailyGoalDao
import com.caltrack.app.data.local.dao.MealDao
import com.caltrack.app.data.local.entity.DailyGoalEntity
import com.caltrack.app.data.local.entity.MealEntity

@Database(
    entities = [MealEntity::class, DailyGoalEntity::class],
    version = 2,
    exportSchema = false
)
abstract class CaltrackDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun dailyGoalDao(): DailyGoalDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meals ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE meals ADD COLUMN imageKey TEXT")
                db.execSQL("ALTER TABLE meals ADD COLUMN mealTime TEXT")
                db.execSQL("ALTER TABLE meals ADD COLUMN allergenWarnings TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
