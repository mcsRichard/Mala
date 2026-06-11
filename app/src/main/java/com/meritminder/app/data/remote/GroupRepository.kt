package com.meritminder.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

data class Group(
    val id: String = "",
    val name: String = "",
    val practiceName: String = "",
    val targetType: String = TYPE_CHECKIN,
    val targetValue: Long = 0L,
    val creatorId: String = "",
    val creatorName: String = "",
    val createdAt: Long = 0L
) {
    companion object {
        const val TYPE_CHECKIN = "CHECKIN"   // 当日完成（每日打卡）
        const val TYPE_TOTAL = "TOTAL"       // 总目标（累计数量）
    }
}

data class GroupMember(
    val userId: String = "",
    val displayName: String = "",
    val total: Long = 0L,
    val doneToday: Boolean = false,
    val todayValue: Long = 0L
)

/** 小组列表卡片所需的汇总状态 */
data class GroupStatus(
    val group: Group,
    val memberCount: Int,
    val todayDoneCount: Int,
    val myDoneToday: Boolean,
    val myTotal: Long
)

class GroupRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val uid: String get() = auth.currentUser?.uid ?: ""
    private val myName: String
        get() = auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.email?.substringBefore("@") ?: "佚名"

    private fun groupDoc(groupId: String) = firestore.collection("groups").document(groupId)
    private fun pointerDoc(groupId: String) =
        firestore.collection("users").document(uid).collection("groups").document(groupId)

    private fun today(): String = LocalDate.now().toString()

    // ── 创建 ──────────────────────────────────────────────────────────────

    suspend fun createGroup(
        name: String,
        practiceName: String,
        targetType: String,
        targetValue: Long
    ): String {
        var code = generateCode()
        repeat(5) {
            if (!groupDoc(code).get().await().exists()) return@repeat
            code = generateCode()
        }
        groupDoc(code).set(
            mapOf(
                "name" to name,
                "practiceName" to practiceName,
                "targetType" to targetType,
                "targetValue" to targetValue,
                "creatorId" to uid,
                "creatorName" to myName,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        addMembership(code)
        return code
    }

    private fun generateCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    // ── 查询 ──────────────────────────────────────────────────────────────

    suspend fun getGroup(groupId: String): Group? {
        val doc = groupDoc(groupId).get().await()
        if (!doc.exists()) return null
        return Group(
            id = doc.id,
            name = doc.getString("name") ?: "",
            practiceName = doc.getString("practiceName") ?: "",
            targetType = doc.getString("targetType") ?: Group.TYPE_CHECKIN,
            targetValue = doc.getLong("targetValue") ?: 0L,
            creatorId = doc.getString("creatorId") ?: "",
            creatorName = doc.getString("creatorName") ?: "",
            createdAt = doc.getLong("createdAt") ?: 0L
        )
    }

    suspend fun getMembers(groupId: String): List<GroupMember> {
        val todayStr = today()
        val memberDocs = groupDoc(groupId).collection("members").get().await()
        val checkinDocs = groupDoc(groupId).collection("checkins")
            .whereEqualTo("date", todayStr).get().await()
        val todayMap = checkinDocs.documents.associate {
            (it.getString("userId") ?: "") to (it.getLong("value") ?: 0L)
        }
        return memberDocs.documents.map { d ->
            val todayValue = todayMap[d.id] ?: 0L
            GroupMember(
                userId = d.id,
                displayName = d.getString("displayName") ?: "",
                total = d.getLong("total") ?: 0L,
                doneToday = todayValue > 0L,
                todayValue = todayValue
            )
        }.sortedByDescending { it.total }
    }

    suspend fun isMember(groupId: String): Boolean =
        groupDoc(groupId).collection("members").document(uid).get().await().exists()

    /** 我加入的所有小组及状态；自动清理已被解散小组的指针 */
    suspend fun getMyGroups(): List<GroupStatus> {
        if (uid.isEmpty()) return emptyList()
        val pointers = firestore.collection("users").document(uid)
            .collection("groups").get().await()
        return pointers.documents.mapNotNull { p ->
            val group = getGroup(p.id)
            if (group == null) {
                p.reference.delete()
                return@mapNotNull null
            }
            val members = getMembers(p.id)
            val me = members.find { it.userId == uid }
            GroupStatus(
                group = group,
                memberCount = members.size,
                todayDoneCount = members.count { it.doneToday },
                myDoneToday = me?.doneToday == true,
                myTotal = me?.total ?: 0L
            )
        }
    }

    // ── 加入 / 退出 ───────────────────────────────────────────────────────

    suspend fun joinGroup(groupId: String) {
        addMembership(groupId)
    }

    private suspend fun addMembership(groupId: String) {
        groupDoc(groupId).collection("members").document(uid).set(
            mapOf(
                "displayName" to myName,
                "joinedAt" to System.currentTimeMillis(),
                "total" to 0L
            ),
            SetOptions.merge()
        ).await()
        pointerDoc(groupId).set(mapOf("joinedAt" to System.currentTimeMillis())).await()
    }

    suspend fun leaveGroup(groupId: String) {
        groupDoc(groupId).collection("members").document(uid).delete().await()
        pointerDoc(groupId).delete().await()
    }

    // ── 管理员操作 ────────────────────────────────────────────────────────

    suspend fun updateGoal(groupId: String, targetType: String, targetValue: Long) {
        groupDoc(groupId).update(
            mapOf("targetType" to targetType, "targetValue" to targetValue)
        ).await()
    }

    suspend fun deleteGroup(groupId: String) {
        val members = groupDoc(groupId).collection("members").get().await()
        members.documents.forEach { it.reference.delete().await() }
        val checkins = groupDoc(groupId).collection("checkins").get().await()
        checkins.documents.forEach { it.reference.delete().await() }
        groupDoc(groupId).delete().await()
        pointerDoc(groupId).delete().await()
    }

    // ── 打卡 ──────────────────────────────────────────────────────────────

    /** value: 打卡类型传 1，总目标类型传本次完成数量（累加） */
    suspend fun checkIn(groupId: String, value: Long) {
        val todayStr = today()
        groupDoc(groupId).collection("checkins").document("${uid}_$todayStr").set(
            mapOf(
                "userId" to uid,
                "date" to todayStr,
                "value" to FieldValue.increment(value),
                "displayName" to myName
            ),
            SetOptions.merge()
        ).await()
        groupDoc(groupId).collection("members").document(uid)
            .update("total", FieldValue.increment(value)).await()
    }
}
