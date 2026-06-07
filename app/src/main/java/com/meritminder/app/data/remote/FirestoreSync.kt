package com.meritminder.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.meritminder.app.data.local.entity.DailyRecord
import com.meritminder.app.data.local.entity.Goal
import com.meritminder.app.data.local.entity.Practice
import kotlinx.coroutines.tasks.await

data class SyncData(
    val practices: List<Practice>,
    val goals: List<Goal>,
    val records: List<DailyRecord>
)

class FirestoreSync(private val userId: String) {

    private val db = FirebaseFirestore.getInstance()
    private fun practices() = db.collection("users/$userId/practices")
    private fun goals() = db.collection("users/$userId/goals")
    private fun records() = db.collection("users/$userId/records")

    suspend fun pushPractice(practice: Practice) = runCatching {
        practices().document(practice.id.toString()).set(
            mapOf("id" to practice.id, "userId" to practice.userId,
                  "name" to practice.name, "type" to practice.type,
                  "createdAt" to practice.createdAt, "sortOrder" to practice.sortOrder)
        ).await()
    }

    suspend fun pushGoal(goal: Goal) = runCatching {
        goals().document(goal.id.toString()).set(
            mapOf("id" to goal.id, "practiceId" to goal.practiceId,
                  "targetType" to goal.targetType, "targetValue" to goal.targetValue,
                  "periodType" to goal.periodType, "deadlineDate" to goal.deadlineDate,
                  "personalTargetDate" to goal.personalTargetDate,
                  "isActive" to goal.isActive, "createdAt" to goal.createdAt)
        ).await()
    }

    suspend fun pushRecord(record: DailyRecord) = runCatching {
        records().document("${record.practiceId}_${record.date}").set(
            mapOf("id" to record.id, "practiceId" to record.practiceId,
                  "date" to record.date, "completedValue" to record.completedValue,
                  "updatedAt" to record.updatedAt)
        ).await()
    }

    suspend fun deletePractice(practiceId: Int) = runCatching {
        practices().document(practiceId.toString()).delete().await()
        val goalDocs = goals().whereEqualTo("practiceId", practiceId).get().await()
        goalDocs.documents.forEach { it.reference.delete().await() }
        val recDocs = records().whereEqualTo("practiceId", practiceId).get().await()
        recDocs.documents.forEach { it.reference.delete().await() }
    }

    suspend fun isEmpty(): Boolean = runCatching {
        practices().limit(1).get().await().isEmpty
    }.getOrDefault(true)

    suspend fun pullAll(): SyncData? = runCatching {
        val practiceList = practices().get().await().documents.mapNotNull { doc ->
            runCatching {
                Practice(
                    id = (doc.getLong("id") ?: return@mapNotNull null).toInt(),
                    userId = doc.getString("userId") ?: userId,
                    name = doc.getString("name") ?: return@mapNotNull null,
                    type = doc.getString("type") ?: return@mapNotNull null,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    sortOrder = doc.getLong("sortOrder")?.toInt() ?: 0
                )
            }.getOrNull()
        }
        val goalList = goals().get().await().documents.mapNotNull { doc ->
            runCatching {
                Goal(
                    id = (doc.getLong("id") ?: return@mapNotNull null).toInt(),
                    practiceId = (doc.getLong("practiceId") ?: return@mapNotNull null).toInt(),
                    targetType = doc.getString("targetType") ?: return@mapNotNull null,
                    targetValue = doc.getLong("targetValue") ?: return@mapNotNull null,
                    periodType = doc.getString("periodType") ?: return@mapNotNull null,
                    deadlineDate = doc.getString("deadlineDate"),
                    personalTargetDate = doc.getString("personalTargetDate"),
                    isActive = doc.getBoolean("isActive") ?: true,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }.getOrNull()
        }
        val recordList = records().get().await().documents.mapNotNull { doc ->
            runCatching {
                DailyRecord(
                    id = (doc.getLong("id") ?: return@mapNotNull null).toInt(),
                    practiceId = (doc.getLong("practiceId") ?: return@mapNotNull null).toInt(),
                    date = doc.getString("date") ?: return@mapNotNull null,
                    completedValue = doc.getLong("completedValue") ?: return@mapNotNull null,
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            }.getOrNull()
        }
        SyncData(practiceList, goalList, recordList)
    }.getOrNull()
}
