package com.izquierdojl.tolocharadio

import android.app.Application
import com.tolocharadio.ui.notification.NotificationChannelManager
import dagger.hilt.android.HiltAndroidApp

/** Punto de entrada de la app. Hilt genera el grafo de dependencias. */
@HiltAndroidApp
class TolochaApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Create notification channels
        NotificationChannelManager(this).createAllChannels()
    }
}
