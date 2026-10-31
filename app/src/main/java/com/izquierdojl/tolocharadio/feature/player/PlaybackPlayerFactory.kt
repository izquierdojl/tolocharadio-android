package com.izquierdojl.tolocharadio.feature.player

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.domain.playback.BufferProfile
import com.izquierdojl.tolocharadio.domain.playback.BufferProfileSpec
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Fábrica de `ExoPlayer` con el `LoadControl` de un perfil (spec 0042, R2). */
@Singleton
class PlaybackPlayerFactory
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun create(profile: BufferProfile): ExoPlayer =
            PlayerAudioConfig
                .applyTo(ExoPlayer.Builder(context), BufferProfileSpec.forProfile(profile))
                .build()
    }
