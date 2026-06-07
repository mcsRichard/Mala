package com.meritminder.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.meritminder.app.data.local.entity.Practice
import com.meritminder.app.data.local.entity.PracticeWithGoals
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeDao {

    @Transaction
    @Query("SELECT * FROM practices WHERE userId = :userId ORDER BY sortOrder ASC, createdAt DESC")
    fun getPracticesWithGoals(userId: String): Flow<List<PracticeWithGoals>>

    @Insert
    suspend fun insert(practice: Practice): Long

    @Update
    suspend fun updateAll(practices: List<Practice>)

    @Transaction
    @Query("SELECT * FROM practices WHERE id = :id LIMIT 1")
    fun getPracticeWithGoals(id: Int): Flow<PracticeWithGoals?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(practices: List<Practice>)

    @Delete
    suspend fun delete(practice: Practice)
}
