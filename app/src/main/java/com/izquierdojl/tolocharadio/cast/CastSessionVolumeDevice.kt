package com.izquierdojl.tolocharadio.cast

import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient

/**
 * Adaptador de [RemoteVolumeDevice] sobre una [CastSession] (spec 0037).
 *
 * Usa el silencio de dispositivo del receptor (`CastSession.setMute`/`isMute`).
 * El eco se obtiene con `RemoteMediaClient.Callback.onStatusUpdated`.
 *
 * Todas las operaciones toleran una sesión ya cerrada.
 */
class CastSessionVolumeDevice(
    private val session: CastSession,
) : RemoteVolumeDevice {
    private var onChanged: (() -> Unit)? = null

    private val client: RemoteMediaClient?
        get() = session.remoteMediaClient

    private val statusCallback =
        object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                onChanged?.invoke()
            }
        }

    override fun readMuted(): Boolean = runCatching { session.isMute }.getOrDefault(false)

    override fun writeMuted(muted: Boolean) {
        runCatching { session.isMute = muted }
    }

    override fun observe(onChanged: () -> Unit) {
        this.onChanged = onChanged
        client?.registerCallback(statusCallback)
    }

    override fun stopObserving() {
        client?.unregisterCallback(statusCallback)
        onChanged = null
    }
}
