package com.mobilitytracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mobilitytracker.MainActivity
import com.mobilitytracker.R

object NotificationHelper {

    private const val CHANNEL_ID = "reminder_channel"
    private const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Przypomnienia treningowe",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Codzienne przypomnienie o treningu mobilności"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun showReminder(context: Context, title: String, workoutTitle: String, cycleNumber: Int, videoOrder: Int) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markDoneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_MARK_DONE
            putExtra(ReminderReceiver.EXTRA_CYCLE, cycleNumber)
            putExtra(ReminderReceiver.EXTRA_VIDEO_ORDER, videoOrder)
        }
        val markDonePending = PendingIntent.getBroadcast(
            context, 1, markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(workoutTitle)
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .addAction(0, "Oznacz jako zrobione", markDonePending)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }
}
