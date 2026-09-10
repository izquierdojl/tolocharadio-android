package com.izquierdojl.tolocharadio.core.shortcuts

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ImageRequest
import com.izquierdojl.tolocharadio.MainActivity
import com.izquierdojl.tolocharadio.R
import com.izquierdojl.tolocharadio.domain.shortcuts.ShortcutStation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Publica los accesos directos con `ShortcutManagerCompat` (research.md R1/R2).
 *
 * Wrapper delgado de plataforma: la construcción de specs y la selección de
 * emisoras viven fuera (testables en JVM). Los iconos se resuelven
 * best-effort con Coil y caen al icono de la app si fallan o expiran, sin
 * bloquear la publicación.
 */
@Singleton
class AndroidShortcutPublisher
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val specFactory: ShortcutSpecFactory,
    ) : ShortcutPublisher {
        override fun maxSlots(): Int =
            ShortcutManagerCompat
                .getMaxShortcutCountPerActivity(context)
                .coerceIn(0, MAX_SLOTS)

        override suspend fun publish(stations: List<ShortcutStation>) {
            val slots = maxSlots()
            if (slots <= 0) return
            val specs = specFactory.create(stations.take(slots))
            val icons = specs.associate { it.id to fetchIcon(it.iconUrl) }
            val shortcuts = specs.map { toShortcutInfo(it, icons[it.id]) }
            runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
                .onFailure { Log.w(TAG, "No se pudieron publicar los accesos directos", it) }
        }

        override suspend fun clear() {
            runCatching { ShortcutManagerCompat.removeAllDynamicShortcuts(context) }
                .onFailure { Log.w(TAG, "No se pudieron limpiar los accesos directos", it) }
        }

        private suspend fun fetchIcon(url: String?): IconCompat? {
            if (url.isNullOrBlank()) return null
            return withTimeoutOrNull(ICON_TIMEOUT_MS) {
                runCatching {
                    val request =
                        ImageRequest
                            .Builder(context)
                            .data(url)
                            .allowHardware(false)
                            .build()
                    Coil
                        .imageLoader(context)
                        .execute(request)
                        .drawable
                        ?.let { IconCompat.createWithAdaptiveBitmap(it.toBitmap()) }
                }.getOrNull()
            }
        }

        private fun toShortcutInfo(
            spec: ShortcutSpec,
            icon: IconCompat?,
        ): ShortcutInfoCompat {
            val intent =
                Intent(context, MainActivity::class.java)
                    .setAction(spec.intentAction)
                    .putExtra(ShortcutIntents.EXTRA_STATION_ID, spec.intentStationId)
                    .putExtra(ShortcutIntents.EXTRA_STATION_NAME, spec.intentStationName)
            return ShortcutInfoCompat
                .Builder(context, spec.id)
                .setShortLabel(spec.shortLabel)
                .setLongLabel(spec.longLabel)
                .setRank(spec.rank)
                .setIntent(intent)
                .setIcon(icon ?: IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .build()
        }

        private companion object {
            const val TAG = "ShortcutPublisher"
            const val MAX_SLOTS = 5
            const val ICON_TIMEOUT_MS = 1_500L
        }
    }
