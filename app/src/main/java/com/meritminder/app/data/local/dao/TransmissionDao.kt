package com.meritminder.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.meritminder.app.data.local.entity.Transmission
import kotlinx.coroutines.flow.Flow

@Dao
interface TransmissionDao {
    @Query("SELECT * FROM transmissions WHERE userId = :userId ORDER BY date DESC, createdAt DESC")
    fun getAll(userId: String): Flow<List<Transmission>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(t: Transmission)

    @Update
    suspend fun update(t: Transmission)

    @Delete
    suspend fun delete(t: Transmission)
}
