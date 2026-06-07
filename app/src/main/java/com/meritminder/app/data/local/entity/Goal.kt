package com.meritminder.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goals",
    indices = [Index("practiceId")],
    foreignKeys = [ForeignKey(
        entity = Practice::class,
        parentColumns = ["id"],
        childColumns = ["practiceId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val practiceId: Int,
    val targetType: String,
    val targetValue: Long,
    val periodType: String,
    val deadlineDate: String? = null,
    val personalTargetDate: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_COUNT = "COUNT"
        const val TYPE_TIME = "TIME_MINUTES"
        const val TYPE_CHECKIN = "CHECKIN"
        const val TYPE_COURSE = "COURSE"
        const val PERIOD_DAILY = "DAILY"
        const val PERIOD_LONG_TERM = "LONG_TERM"
    }
}
