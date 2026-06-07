package com.meritminder.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.meritminder.app.data.local.entity.DailyRecord
import com.meritminder.app.data.local.entity.PracticeTotalEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyRecordDao {

    @Query("SELECT * FROM daily_records WHERE practiceId = :practiceId AND date = :date LIMIT 1")
    suspend fun getRecordForDate(practiceId: Int, date: String): DailyRecord?

    @Query("SELECT * FROM daily_records WHERE date = :date")
    fun getRecordsForDate(date: String): Flow<List<DailyRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: DailyRecord)

    @Query("SELECT * FROM daily_records WHERE practiceId = :practiceId ORDER BY date DESC")
    fun getRecordsByPractice(practiceId: Int): Flow<List<DailyRecord>>

    @Query("SELECT SUM(completedValue) FROM daily_records WHERE practiceId = :practiceId")
    suspend fun getTotalCompleted(practiceId: Int): Long?

    @Query("SELECT practiceId, SUM(completedValue) as total FROM daily_records GROUP BY practiceId")
    fun getAllTotals(): Flow<List<PracticeTotalEntry>>

    @Query("SELECT * FROM daily_records ORDER BY date DESC")
    fun getAllRecordsFlow(): Flow<List<DailyRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<DailyRecord>)
}
