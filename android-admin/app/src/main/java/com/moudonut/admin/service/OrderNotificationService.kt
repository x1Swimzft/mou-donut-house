package com.moudonut.admin.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.moudonut.admin.data.CredentialStore
import com.moudonut.admin.data.SupabaseHolder
import com.moudonut.admin.util.NotificationNotifier
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OrderNotificationService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var realtimeChannel: RealtimeChannel? = null
    private var isListenerRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationNotifier.createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. Immediately start as foreground service to comply with Android 8+ (must call within 5s)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NotificationNotifier.SERVICE_NOTIFICATION_ID,
                    NotificationNotifier.buildServiceNotification(this),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(
                    NotificationNotifier.SERVICE_NOTIFICATION_ID,
                    NotificationNotifier.buildServiceNotification(this)
                )
            }
        } catch (_: Throwable) {
            // Fallback if background start restriction triggers
        }

        // 2. Start resilient realtime order listener
        startRealtimeListener()

        return START_STICKY
    }

    private fun startRealtimeListener() {
        if (isListenerRunning) return
        if (!SupabaseHolder.configured) return
        isListenerRunning = true

        serviceScope.launch {
            val client = SupabaseHolder.client
            while (isActive) {
                try {
                    // Step A: Ensure authenticated admin session so Realtime passes RLS on public.orders
                    if (client.auth.currentSessionOrNull() == null) {
                        val creds = CredentialStore.load(applicationContext)
                        if (creds == null) {
                            // User is not logged in / credentials cleared
                            delay(10_000)
                            continue
                        }
                        val (email, password) = creds
                        try {
                            client.auth.signInWith(Email) {
                                this.email = email
                                this.password = password
                            }
                        } catch (_: Throwable) {
                            // Network not ready yet (e.g. immediately after boot); retry after 5s
                            delay(5_000)
                            continue
                        }
                    }

                    // Step B: Connect to Realtime channel filtered strictly to 'orders' table
                    val channel = client.channel("admin_order_notification_service")
                    val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = "orders"
                    }

                    realtimeChannel = channel
                    channel.subscribe()

                    // Step C: Collect new orders
                    changes.collect { action ->
                        if (action is PostgresAction.Insert) {
                            NotificationNotifier.notifyNewOrder(
                                context = applicationContext,
                                title = "มีออเดอร์ใหม่เข้ามา! 🍩",
                                message = "ลูกค้าส่งออเดอร์ใหม่เข้ามาแล้ว แตะเพื่อเปิดดูบิล"
                            )
                        }
                    }
                } catch (_: Throwable) {
                    // On connection drop or network switch, handle retry with backoff
                } finally {
                    try {
                        realtimeChannel?.unsubscribe()
                    } catch (_: Throwable) {}
                    realtimeChannel = null
                    delay(5_000)
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Keep foreground service alive when app is swiped from Recents
    }

    override fun onDestroy() {
        super.onDestroy()
        isListenerRunning = false
        serviceScope.launch {
            try {
                realtimeChannel?.unsubscribe()
            } catch (_: Throwable) {}
        }
        serviceScope.cancel()
    }
}
