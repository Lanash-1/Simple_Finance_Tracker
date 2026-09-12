package com.codigitech.ft

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.codigitech.ft.platform.ActivityHolder
import com.codigitech.ft.ui.App

class MainActivity : FragmentActivity() {
    private val requestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ActivityHolder.current = this
        if (Build.VERSION.SDK_INT >= 33) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { App() }
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
