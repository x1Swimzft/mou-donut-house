package com.moudonut.admin.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.moudonut.admin.data.CredentialStore
import com.moudonut.admin.service.OrderNotificationService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val creds = CredentialStore.load(context)
            if (creds != null) {
                try {
                    val serviceIntent = Intent(context, OrderNotificationService::class.java)
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (_: Throwable) {
                    // Fail gracefully if device system policy blocks service start
                }
            }
        }
    }
}
