package com.meritminder.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.meritminder.app.MainActivity
import com.meritminder.app.R

object NotificationHelper {

    const val CHANNEL_ID = "mala_reminders"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun sendReminder(context: Context, notifId: Int, pendingPractices: List<String>) {
        val title = if (pendingPractices.isEmpty()) "今日功课已全部完成 ✓" else "修行提醒（还有 ${pendingPractices.size} 项）"
        val shortText = when {
            pendingPractices.isEmpty() -> "坚持修行，随喜赞叹！"
            pendingPractices.size == 1 -> "待完成：${pendingPractices[0]}"
            else -> "待完成：${pendingPractices.take(2).joinToString("、")}${if (pendingPractices.size > 2) " 等" else ""}"
        }
        val longText = if (pendingPractices.isEmpty()) shortText
        else "待完成：\n" + pendingPractices.joinToString("\n") { "• $it" }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            context, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(shortText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(longText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }
}
