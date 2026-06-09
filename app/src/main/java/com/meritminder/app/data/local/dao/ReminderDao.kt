package com.meritminder.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.meritminder.app.data.local.entity.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY hour ASC, minute ASC")
    fun getAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders ORDER BY hour ASC, minute ASC")
    suspend fun getAllSync(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Reminder?

    @Insert
    suspend fun insert(r: Reminder): Long

    @Update
    suspend fun update(r: Reminder)

    @Delete
    suspend fun delete(r: Reminder)
}
