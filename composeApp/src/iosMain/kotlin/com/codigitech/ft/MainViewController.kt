package com.codigitech.ft

import androidx.compose.ui.window.ComposeUIViewController
import com.codigitech.ft.data.db.DatabaseDriverFactory
import com.codigitech.ft.di.initKoin
import com.codigitech.ft.platform.AppLockManager
import com.codigitech.ft.platform.BiometricAuthenticator
import com.codigitech.ft.platform.FileSharer
import com.codigitech.ft.platform.IosBiometricAuthenticator
import com.codigitech.ft.platform.IosFileSharer
import com.codigitech.ft.platform.IosNotifier
import com.codigitech.ft.platform.IosRecurringScheduler
import com.codigitech.ft.platform.Notifier
import com.codigitech.ft.platform.RecurringScheduler
import com.codigitech.ft.platform.RecurringSyncCoordinator
import com.codigitech.ft.platform.requestNotificationPermission
import com.codigitech.ft.ui.App
import org.koin.mp.KoinPlatform
import org.koin.dsl.module

private var koinStarted = false

/** Called once from Swift on app start. */
fun startIosApp() {
    if (koinStarted) return
    koinStarted = true
    val platformModule = module {
        single { DatabaseDriverFactory() }
        single<Notifier> { IosNotifier() }
        single<RecurringScheduler> { IosRecurringScheduler() }
        single<BiometricAuthenticator> { IosBiometricAuthenticator() }
        single<FileSharer> { IosFileSharer() }
    }
    val koin = initKoin(platformModule).koin
    koin.get<RecurringSyncCoordinator>().scheduler = koin.get<RecurringScheduler>()
    requestNotificationPermission()
}

/** Called from Swift when the scene moves to the background so the lock re-engages. */
fun onAppBackground() {
    if (koinStarted) KoinPlatform.getKoin().get<AppLockManager>().onBackground()
}

fun MainViewController() = ComposeUIViewController { App() }
