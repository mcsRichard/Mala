package com.meritminder.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transmissions")
data class Transmission(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val name: String,
    val teacher: String = "",
    val date: String = "",
    val place: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
