package com.meritminder.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import com.meritminder.app.data.local.entity.Goal

@Dao
interface GoalDao {

    @Insert
    suspend fun insert(goal: Goal): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<Goal>)

    @Update
    suspend fun update(goal: Goal)
}
