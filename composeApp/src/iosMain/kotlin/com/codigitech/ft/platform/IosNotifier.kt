package com.codigitech.ft.platform

import com.codigitech.ft.Brand
import kotlinx.datetime.LocalDate
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

private val center get() = UNUserNotificationCenter.currentNotificationCenter()

fun requestNotificationPermission() {
    center.requestAuthorizationWithOptions(
        UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
    ) { _, _ -> }
}

class IosNotifier : Notifier {
    override fun notify(title: String, body: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound)
        }
        // nil trigger = deliver immediately.
        val request = UNNotificationRequest.requestWithIdentifier("recurring-summary", content, null)
        center.addNotificationRequest(request) { _ -> }
    }
}

/** Schedules a local reminder at 09:00 on the next due date. */
class IosRecurringScheduler : RecurringScheduler {
    override fun schedule(epochDay: Long?) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(REMINDER_ID))
        if (epochDay == null) return
        val date = LocalDate.fromEpochDays(epochDay)
        val components = NSDateComponents().apply {
            year = date.year.toLong()
            month = (date.month.ordinal + 1).toLong()
            day = date.day.toLong()
            hour = 9
            minute = 0
            calendar = NSCalendar.currentCalendar
        }
        val content = UNMutableNotificationContent().apply {
            setTitle(Brand.APP_NAME)
            setBody("Recurring transactions are due today. Open the app to record them.")
            setSound(UNNotificationSound.defaultSound)
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false)
        val request = UNNotificationRequest.requestWithIdentifier(REMINDER_ID, content, trigger)
        center.addNotificationRequest(request) { _ -> }
    }

    private companion object {
        const val REMINDER_ID = "recurring-reminder"
    }
}
