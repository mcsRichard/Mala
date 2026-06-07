package com.meritminder.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_records",
    indices = [Index(value = ["practiceId", "date"], unique = true)],
    foreignKeys = [ForeignKey(
        entity = Practice::class,
        parentColumns = ["id"],
        childColumns = ["practiceId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class DailyRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val practiceId: Int,
    val date: String,
    val completedValue: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
