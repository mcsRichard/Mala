package com.meritminder.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.meritminder.app.data.local.dao.DailyRecordDao
import com.meritminder.app.data.local.dao.GoalDao
import com.meritminder.app.data.local.dao.PracticeDao
import com.meritminder.app.data.local.entity.DailyRecord
import com.meritminder.app.data.local.entity.Goal
import com.meritminder.app.data.local.entity.Practice

@Database(
    entities = [Practice::class, Goal::class, DailyRecord::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun practiceDao(): PracticeDao
    abstract fun goalDao(): GoalDao
    abstract fun dailyRecordDao(): DailyRecordDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE practices ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mala_db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}
