package com.meritminder.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "practices")
data class Practice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val name: String,
    val type: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_CHANTING = "CHANTING"
        const val TYPE_MEDITATION = "MEDITATION"
        const val TYPE_OTHER = "OTHER"
    }
}
