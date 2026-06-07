package com.meritminder.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PracticeWithGoals(
    @Embedded val practice: Practice,
    @Relation(
        parentColumn = "id",
        entityColumn = "practiceId"
    )
    val goals: List<Goal>
)
