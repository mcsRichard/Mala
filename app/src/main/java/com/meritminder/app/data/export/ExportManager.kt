package com.meritminder.app.data.export

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.meritminder.app.data.local.AppDatabase
import com.meritminder.app.data.local.entity.Goal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ExportManager {

    suspend fun exportToCsv(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext
        val db = AppDatabase.getInstance(context)
        val sb = StringBuilder()

        sb.appendLine("# Mala 修行记录导出")
        sb.appendLine("# 导出时间：${LocalDate.now()}")
        sb.appendLine()

        // ── SECTION 1: 功课记录 ────────────────────────────────────────────────
        val pwgs = db.practiceDao().getPracticesWithGoals(userId).first()
        val totalsMap = db.dailyRecordDao().getAllTotals().first()
            .associate { it.practiceId to it.total }

        appendSection(sb, "功课记录",
            listOf("功课名称", "类型", "目标类型", "目标数量", "截止日期", "累计完成次数", "创建时间"))
        pwgs.forEach { pwg ->
            val goal = pwg.goals.firstOrNull { it.isActive }
            val total = totalsMap[pwg.practice.id] ?: 0L
            sb.appendLine(row(listOf(
                pwg.practice.name,
                pwg.practice.type.toPracticeTypeLabel(),
                goal?.toGoalTypeLabel() ?: "",
                goal?.let { if (it.targetType != Goal.TYPE_CHECKIN) it.targetValue.toString() else "" } ?: "",
                goal?.deadlineDate ?: "",
                total.toString(),
                pwg.practice.createdAt.epochToDate()
            )))
        }
        sb.appendLine()

        // ── SECTION 2: 传承灌顶 ────────────────────────────────────────────────
        val transmissions = db.transmissionDao().getAll(userId).first()

        appendSection(sb, "传承灌顶",
            listOf("名称", "上师", "时间", "地点", "备注"))
        transmissions.forEach { t ->
            sb.appendLine(row(listOf(t.name, t.teacher, t.date, t.place, t.notes)))
        }
        sb.appendLine()

        // ── 新记录类型在此处继续追加 Section ──────────────────────────────────

        context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())) // UTF-8 BOM for Excel
            os.write(sb.toString().toByteArray(Charsets.UTF_8))
        }
    }

    private fun appendSection(sb: StringBuilder, title: String, headers: List<String>) {
        sb.appendLine("## $title")
        sb.appendLine(row(headers))
    }

    private fun row(fields: List<String>): String =
        fields.joinToString(",") { it.csvEscape() }

    private fun String.csvEscape(): String =
        if (contains(',') || contains('"') || contains('\n') || contains('\r'))
            "\"${replace("\"", "\"\"")}\""
        else this

    private fun String.toPracticeTypeLabel(): String = when (this) {
        "CHANTING" -> "念诵"
        "MEDITATION" -> "打坐"
        else -> "其他"
    }

    private fun Goal.toGoalTypeLabel(): String = when {
        targetType == Goal.TYPE_CHECKIN -> "每日打卡"
        targetType == Goal.TYPE_COURSE -> "课程进度"
        deadlineDate != null -> "限期完成"
        periodType == Goal.PERIOD_LONG_TERM -> "终生累计"
        else -> "每日数量"
    }

    private fun Long.epochToDate(): String = try {
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().toString()
    } catch (_: Exception) { "" }
}
