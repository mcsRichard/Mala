package com.meritminder.app.data.repository

import com.meritminder.app.data.local.AppDatabase
import com.meritminder.app.data.local.entity.DailyRecord
import com.meritminder.app.data.local.entity.Goal
import com.meritminder.app.data.local.entity.Practice
import com.meritminder.app.data.local.entity.PracticeWithGoals
import com.meritminder.app.data.remote.FirestoreSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PracticeRepository(db: AppDatabase, private val userId: String? = null) {

    private val practiceDao = db.practiceDao()
    private val goalDao = db.goalDao()
    private val dailyRecordDao = db.dailyRecordDao()
    private val sync = userId?.takeIf { it.isNotEmpty() }?.let { FirestoreSync(it) }
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun getPracticesWithGoals(userId: String): Flow<List<PracticeWithGoals>> =
        practiceDao.getPracticesWithGoals(userId)

    fun getPracticeWithGoals(practiceId: Int): Flow<PracticeWithGoals?> =
        practiceDao.getPracticeWithGoals(practiceId)

    fun getRecordsByPractice(practiceId: Int): Flow<List<DailyRecord>> =
        dailyRecordDao.getRecordsByPractice(practiceId)

    fun getTodayRecordsFlow(date: String): Flow<List<DailyRecord>> =
        dailyRecordDao.getRecordsForDate(date)

    fun getAllRecordsFlow(): Flow<List<DailyRecord>> = dailyRecordDao.getAllRecordsFlow()

    fun getAllTotals(): Flow<Map<Int, Long>> =
        dailyRecordDao.getAllTotals().map { list -> list.associate { it.practiceId to it.total } }

    suspend fun getRecordForDate(practiceId: Int, date: String): DailyRecord? =
        dailyRecordDao.getRecordForDate(practiceId, date)

    suspend fun addPractice(practice: Practice, goal: Goal) {
        val practiceId = practiceDao.insert(practice).toInt()
        val goalId = goalDao.insert(goal.copy(practiceId = practiceId)).toInt()
        ioScope.launch {
            sync?.pushPractice(practice.copy(id = practiceId))
            sync?.pushGoal(goal.copy(id = goalId, practiceId = practiceId))
        }
    }

    suspend fun deletePractice(practice: Practice) {
        practiceDao.delete(practice)
        ioScope.launch { sync?.deletePractice(practice.id) }
    }

    suspend fun updatePractice(practice: Practice, goal: Goal) {
        practiceDao.updateAll(listOf(practice))
        goalDao.update(goal)
        ioScope.launch {
            sync?.pushPractice(practice)
            sync?.pushGoal(goal)
        }
    }

    suspend fun updateGoalTarget(practiceId: Int, newTarget: Long) {
        val pwg = getPracticeWithGoals(practiceId).first() ?: return
        val activeGoal = pwg.goals.firstOrNull { it.isActive } ?: return
        val updated = activeGoal.copy(targetValue = newTarget)
        goalDao.update(updated)
        ioScope.launch { sync?.pushGoal(updated) }
    }

    suspend fun updateSortOrders(practices: List<Practice>) {
        val updated = practices.mapIndexed { index, p -> p.copy(sortOrder = index) }
        practiceDao.updateAll(updated)
    }

    suspend fun logProgress(practiceId: Int, date: String, additionalValue: Long) {
        val existing = dailyRecordDao.getRecordForDate(practiceId, date)
        val newValue = (existing?.completedValue ?: 0L) + additionalValue
        val record = DailyRecord(
            id = existing?.id ?: 0,
            practiceId = practiceId,
            date = date,
            completedValue = newValue
        )
        dailyRecordDao.insertOrUpdate(record)
        ioScope.launch { sync?.pushRecord(record) }
    }

    suspend fun setProgress(practiceId: Int, date: String, value: Long) {
        val existing = dailyRecordDao.getRecordForDate(practiceId, date)
        val record = DailyRecord(
            id = existing?.id ?: 0,
            practiceId = practiceId,
            date = date,
            completedValue = value
        )
        dailyRecordDao.insertOrUpdate(record)
        ioScope.launch { sync?.pushRecord(record) }
    }

    suspend fun isLocalEmpty(userId: String): Boolean =
        practiceDao.getPracticesWithGoals(userId).first().isEmpty()

    suspend fun importFromFirestore() {
        val data = sync?.pullAll() ?: return
        practiceDao.insertAll(data.practices)
        goalDao.insertAll(data.goals)
        dailyRecordDao.insertAll(data.records)
    }

    suspend fun syncWithFirestore() {
        val s = sync ?: return
        val uid = userId ?: return
        // Push all local data to Firestore
        val localPractices = practiceDao.getPracticesWithGoals(uid).first()
        localPractices.forEach { pwg ->
            s.pushPractice(pwg.practice)
            pwg.goals.forEach { goal -> s.pushGoal(goal) }
        }
        val localRecords = dailyRecordDao.getAllRecordsFlow().first()
        localRecords.forEach { record -> s.pushRecord(record) }
        // Pull Firestore data and merge into local
        val cloudData = s.pullAll() ?: return
        practiceDao.insertAll(cloudData.practices)
        goalDao.insertAll(cloudData.goals)
        // Only insert records whose practice exists (avoids FK constraint crash)
        val validPracticeIds = cloudData.practices.map { it.id }.toSet()
        val validRecords = cloudData.records.filter { it.practiceId in validPracticeIds }
        dailyRecordDao.insertAll(validRecords)
    }
}
