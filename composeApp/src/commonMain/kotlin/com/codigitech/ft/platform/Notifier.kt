package com.codigitech.ft.platform

/** Posts a local notification. Permission handling is the platform's responsibility. */
interface Notifier {
    fun notify(title: String, body: String)
}

/**
 * Best-effort OS-level reminder for the next recurring due date. The app-launch sync
 * is the primary mechanism; this only nudges the user to open the app.
 */
interface RecurringScheduler {
    /** [epochDay] is the next due date in epoch days, or null to cancel. */
    fun schedule(epochDay: Long?)
}
