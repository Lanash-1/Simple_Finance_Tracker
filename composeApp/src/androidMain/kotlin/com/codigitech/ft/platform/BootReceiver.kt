package com.codigitech.ft.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Alarms are cleared on reboot; re-arm the reminder for the next due date. */
class BootReceiver : BroadcastReceiver(), KoinComponent {
    private val coordinator: RecurringSyncCoordinator by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val result = goAsync()
        try {
            runBlocking { coordinator.rescheduleReminder() }
        } finally {
            result.finish()
        }
    }
}
