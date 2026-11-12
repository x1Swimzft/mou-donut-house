package com.moudonut.admin.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.moudonut.admin.MainActivity

object NotificationNotifier {
    const val CHANNEL_NEW_ORDERS = "mou_donut_new_orders"
    const val CHANNEL_SERVICE = "mou_donut_service"
    const val SERVICE_NOTIFICATION_ID = 9001

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. High-importance channel for incoming customer orders (heads-up, sound, vibrate)
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val orderChannel = NotificationChannel(
                CHANNEL_NEW_ORDERS,
                "ออเดอร์ใหม่ (New Orders)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "แจ้งเตือนทันทีเมื่อลูกค้าส่งออเดอร์เข้ามา"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(orderChannel)

            // 2. Low-importance ongoing channel for the background realtime order listener service
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "สถานะระบบรับออเดอร์ (Order Listener Service)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "แสดงสถานะการเชื่อมต่อรับออเดอร์แบบเรียลไทม์เบื้องหลัง"
                setShowBadge(false)
            }
            manager.createNotificationChannel(serviceChannel)
        }
    }

    fun buildServiceNotification(context: Context): Notification {
        createChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("หมู-บ้านโดนัท: ระบบรับออเดอร์เปิดทำงาน")
            .setContentText("กำลังรอรับออเดอร์ใหม่แบบเรียลไทม์...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun notifyNewOrder(
        context: Context,
        title: String = "มีออเดอร์ใหม่เข้ามา! 🍩",
        message: String = "มีลูกค้าส่งออเดอร์ใหม่เข้ามา แตะเพื่อตรวจสอบบิล"
    ) {
        createChannels(context)

        // 1. Post Android System Notification
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                (System.currentTimeMillis() % 100000).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notification = NotificationCompat.Builder(context, CHANNEL_NEW_ORDERS)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setVibrate(longArrayOf(0, 300, 150, 300))
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            val notificationId = (System.currentTimeMillis() % 100000).toInt() + 1000
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: Throwable) {
            // Permission not granted or notification disabled
        }

        // 2. Also play in-app sound & haptics as immediate feedback
        playDirectSound(context)
        playDirectVibration(context)
    }

    private fun playDirectSound(context: Context) {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notificationUri)
            ringtone?.play()
        } catch (_: Throwable) {
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
            }
        }
    }

    private fun playDirectVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 200, 100, 200), -1)
                }
            }
        } catch (_: Throwable) {}
    }
}
