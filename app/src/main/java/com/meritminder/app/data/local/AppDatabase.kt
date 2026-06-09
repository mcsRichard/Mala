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
import com.meritminder.app.data.local.dao.ReminderDao
import com.meritminder.app.data.local.dao.TransmissionDao
import com.meritminder.app.data.local.entity.DailyRecord
import com.meritminder.app.data.local.entity.Goal
import com.meritminder.app.data.local.entity.Practice
import com.meritminder.app.data.local.entity.Reminder
import com.meritminder.app.data.local.entity.Transmission

@Database(
    entities = [Practice::class, Goal::class, DailyRecord::class, Transmission::class, Reminder::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun practiceDao(): PracticeDao
    abstract fun goalDao(): GoalDao
    abstract fun dailyRecordDao(): DailyRecordDao
    abstract fun transmissionDao(): TransmissionDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE practices ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS transmissions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        teacher TEXT NOT NULL DEFAULT '',
                        date TEXT NOT NULL DEFAULT '',
                        place TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transmissions ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reminders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        hour INTEGER NOT NULL,
                        minute INTEGER NOT NULL,
                        enabled INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mala_db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build().also { INSTANCE = it }
            }
    }
}
