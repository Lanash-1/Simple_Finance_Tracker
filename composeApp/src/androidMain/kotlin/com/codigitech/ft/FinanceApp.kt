package com.codigitech.ft

import android.app.Application
import android.content.pm.ApplicationInfo
import com.codigitech.ft.data.db.DatabaseDriverFactory
import com.codigitech.ft.di.initKoin
import com.codigitech.ft.platform.AndroidBiometricAuthenticator
import com.codigitech.ft.platform.AndroidFileSharer
import com.codigitech.ft.platform.AndroidNotifier
import com.codigitech.ft.platform.AndroidRecurringScheduler
import com.codigitech.ft.platform.BiometricAuthenticator
import com.codigitech.ft.platform.FileSharer
import com.codigitech.ft.platform.Notifier
import com.codigitech.ft.platform.RecurringScheduler
import com.codigitech.ft.platform.RecurringSyncCoordinator
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level
import org.koin.dsl.module

class FinanceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val platformModule = module {
            single { DatabaseDriverFactory(this@FinanceApp) }
            single<Notifier> { AndroidNotifier(this@FinanceApp) }
            single<RecurringScheduler> { AndroidRecurringScheduler(this@FinanceApp) }
            single<BiometricAuthenticator> { AndroidBiometricAuthenticator() }
            single<FileSharer> { AndroidFileSharer(this@FinanceApp) }
        }
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val koin = initKoin(platformModule) {
            androidLogger(if (debuggable) Level.DEBUG else Level.NONE)
            androidContext(this@FinanceApp)
        }.koin
        koin.get<RecurringSyncCoordinator>().scheduler = koin.get<RecurringScheduler>()
    }
}
