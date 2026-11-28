package com.izquierdojl.tolocharadio.feature.player

import com.izquierdojl.tolocharadio.cast.CastConnectionState
import com.izquierdojl.tolocharadio.cast.CastPlayerState
import com.izquierdojl.tolocharadio.core.ui.playback.NowPlayingUi
import com.izquierdojl.tolocharadio.core.ui.playback.StationPlaybackUiState
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Spec 0061: mapeo puro de PlayerState/Cast al estado de tarjeta. */
class PlayerStateMapperTest {
    private val station = StationDto(id = "s1", name = "Test", url = "https://stream.test/radio.mp3")
    private val other = StationDto(id = "s2", name = "Otra", url = "https://stream.test/otra.mp3")

    private fun cast(state: PlayerState) =
        CastPlayerState.Cast(
            playerState = state,
            deviceName = "Sala",
            connectionState = CastConnectionState.CONNECTED,
        )

    @Test
    fun `idle no tiene emisora`() {
        val ui = nowPlayingUi(PlayerState.Idle)
        assertNull(ui.stationId)
        assertEquals(StationPlaybackUiState.Inactive, ui.state)
    }

    @Test
    fun `playing y paused identifican la emisora`() {
        assertEquals(
            NowPlayingUi(station.id, StationPlaybackUiState.Playing),
            nowPlayingUi(PlayerState.Playing(station)),
        )
        assertEquals(
            NowPlayingUi(station.id, StationPlaybackUiState.Paused),
            nowPlayingUi(PlayerState.Paused(station)),
        )
    }

    @Test
    fun `buffering y reconnecting se presentan como loading`() {
        assertEquals(
            StationPlaybackUiState.Loading,
            nowPlayingUi(PlayerState.Buffering(station)).state,
        )
        assertEquals(
            StationPlaybackUiState.Loading,
            nowPlayingUi(PlayerState.Reconnecting(station, 1, 3)).state,
        )
    }

    @Test
    fun `error conserva la emisora y error sin emisora no la inventa`() {
        assertEquals(
            NowPlayingUi(station.id, StationPlaybackUiState.Error),
            nowPlayingUi(PlayerState.Error(station, "Fallo")),
        )
        assertNull(nowPlayingUi(PlayerState.Error(null, "Fallo")).stationId)
    }

    @Test
    fun `cast manda sobre el estado local`() {
        val local = PlayerState.Playing(other)
        val remote = PlayerState.Paused(station)
        assertEquals(StationPlaybackUiState.Paused, nowPlayingUi(local, cast(remote)).state)
        assertEquals(station.id, nowPlayingUi(local, cast(remote)).stationId)
    }

    @Test
    fun `stateFor devuelve inactive para otra emisora`() {
        val ui = NowPlayingUi(station.id, StationPlaybackUiState.Playing)
        assertEquals(StationPlaybackUiState.Playing, ui.stateFor(station.id))
        assertEquals(StationPlaybackUiState.Inactive, ui.stateFor(other.id))
    }
}
