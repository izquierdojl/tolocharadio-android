package com.izquierdojl.tolocharadio

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.izquierdojl.tolocharadio.core.shortcuts.ShortcutSyncCoordinator
import com.tolocharadio.ui.notification.NotificationChannelManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Punto de entrada de la app. Hilt genera el grafo de dependencias. */
@HiltAndroidApp
class TolochaApp : Application() {
    @Inject
    lateinit var shortcutSyncCoordinator: ShortcutSyncCoordinator

    override fun onCreate() {
        super.onCreate()
        // Create notification channels
        NotificationChannelManager(this).createAllChannels()
        // Sincroniza los accesos directos del icono con el historial (spec 0018).
        shortcutSyncCoordinator.start()
        registerActivityLifecycleCallbacks(ForegroundShortcutSync(shortcutSyncCoordinator))
    }
}

/**
 * Refresca los accesos directos al pasar la app a primer plano (FR-009).
 * Se usa [ActivityLifecycleCallbacks] para no añadir `lifecycle-process`.
 */
private class ForegroundShortcutSync(
    private val coordinator: ShortcutSyncCoordinator,
) : ActivityLifecycleCallbacks {
    override fun onActivityStarted(activity: Activity) {
        coordinator.onForeground()
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
