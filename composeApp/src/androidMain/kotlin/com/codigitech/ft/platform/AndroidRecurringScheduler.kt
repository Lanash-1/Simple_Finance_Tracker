package com.codigitech.ft.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Best-effort: an inexact alarm at 09:00 on the next due date that runs the sync and posts the summary notification. */
class AndroidRecurringScheduler(private val context: Context) : RecurringScheduler {
    override fun schedule(epochDay: Long?) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent()
        if (epochDay == null) {
            alarmManager.cancel(pending)
            return
        }
        val triggerAt = LocalDate.fromEpochDays(epochDay)
            .atTime(9, 0)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
        val now = System.currentTimeMillis()
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, maxOf(triggerAt, now + 60_000L), pending)
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, RecurringAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

class RecurringAlarmReceiver : BroadcastReceiver(), KoinComponent {
    private val coordinator: RecurringSyncCoordinator by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        try {
            runBlocking { coordinator.sync(notify = true) }
        } finally {
            result.finish()
        }
    }
}
