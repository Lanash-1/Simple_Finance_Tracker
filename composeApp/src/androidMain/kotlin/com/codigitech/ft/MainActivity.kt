package com.codigitech.ft

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codigitech.ft.platform.ActivityHolder
import com.codigitech.ft.platform.AppSettings
import com.codigitech.ft.platform.ThemeMode
import com.codigitech.ft.ui.App
import org.koin.android.ext.android.inject

class MainActivity : FragmentActivity() {
    private val requestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val settings: AppSettings by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ActivityHolder.current = this
        if (Build.VERSION.SDK_INT >= 33) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            // System bar icon colours must follow the *effective* theme. enableEdgeToEdge() alone reads the
            // OS setting, so the in-app Dark override would leave dark icons on a dark status bar.
            val mode by settings.themeMode.collectAsStateWithLifecycle()
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        ActivityHolder.current = this
    }

    override fun onDestroy() {
        if (ActivityHolder.current === this) ActivityHolder.current = null
        super.onDestroy()
    }
}
