package com.izquierdojl.tolocharadio.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.izquierdojl.tolocharadio.core.ui.ViewMode
import com.izquierdojl.tolocharadio.core.ui.navigation.PodcastStartScreen
import com.izquierdojl.tolocharadio.domain.playback.BufferProfile
import com.izquierdojl.tolocharadio.domain.podcasts.TopCountry
import com.izquierdojl.tolocharadio.domain.podcasts.TopGenre
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Fake JVM de DataStore: data + updateData (edit delega en updateData). */
private class FakeDataStore : DataStore<Preferences> {
    val state = MutableStateFlow<Preferences>(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val next = transform(state.value)
        state.value = next
        return next
    }
}

/**
 * Preferencia de modo de vista (spec 008): default LIST sin clave,
 * round-trip al persistir y fallback LIST ante valor corrupto.
 */
class InstancePrefsTest {
    @Test
    fun `sin clave el modo es LIST (FR-005)`() =
        runTest {
            val prefs = InstancePrefs(FakeDataStore())
            assertEquals(ViewMode.LIST, prefs.viewMode.first())
        }

    @Test
    fun `setViewMode persiste y se lee de vuelta (FR-006)`() =
        runTest {
            val store = FakeDataStore()
            val prefs = InstancePrefs(store)
            prefs.setViewMode(ViewMode.GRID)
            assertEquals(ViewMode.GRID, prefs.viewMode.first())
            prefs.setViewMode(ViewMode.LIST)
            assertEquals(ViewMode.LIST, prefs.viewMode.first())
        }

    @Test
    fun `valor corrupto cae a LIST sin error (FR-010)`() =
        runTest {
            val store = FakeDataStore()
            store.state.value = mutablePreferencesOf(stringPreferencesKey("view_mode") to "XXX")
            val prefs = InstancePrefs(store)
            assertEquals(ViewMode.LIST, prefs.viewMode.first())
        }

    @Test
    fun `buffer y reanudacion tienen defaults de producto`() =
        runTest {
            val prefs = InstancePrefs(FakeDataStore())
            assertEquals(BufferProfile.BALANCED, prefs.bufferProfile.first())
            assertEquals(true, prefs.resumeOnConnectivity.first())
        }

    @Test
    fun `setBufferProfile persiste y se lee de vuelta`() =
        runTest {
            val prefs = InstancePrefs(FakeDataStore())
            prefs.setBufferProfile(BufferProfile.MAX_STABILITY)
            assertEquals(BufferProfile.MAX_STABILITY, prefs.bufferProfile.first())
        }

    @Test
    fun `buffer corrupto cae a BALANCED`() =
        runTest {
            val store = FakeDataStore()
            store.state.value = mutablePreferencesOf(stringPreferencesKey("buffer_profile") to "XXX")
            val prefs = InstancePrefs(store)
            assertEquals(BufferProfile.BALANCED, prefs.bufferProfile.first())
        }

    @Test
    fun `setResumeOnConnectivity persiste y se lee de vuelta`() =
        runTest {
            val prefs = InstancePrefs(FakeDataStore())
            prefs.setResumeOnConnectivity(false)
            assertEquals(false, prefs.resumeOnConnectivity.first())
        }

    @Test
    fun `voiceBoost tiene default false y persiste (US4)`() =
        runTest {
            val prefs = InstancePrefs(FakeDataStore())
            assertEquals(false, prefs.voiceBoost.first())
            prefs.setVoiceBoost(true)
            assertEquals(true, prefs.voiceBoost.first())
        }

    @Test
    fun `downloadRetentionDays tiene default 30 y persiste`() =
        runTest {
            val prefs = InstancePrefs(FakeDataStore())
            assertEquals(30, prefs.downloadRetentionDays.first())
            prefs.setDownloadRetentionDays(7)
            assertEquals(7, prefs.downloadRetentionDays.first())
            prefs.setDownloadRetentionDays(0)
            assertEquals(0, prefs.downloadRetentionDays.first())
        }

    @Test
    fun `podcastStartScreen tiene default HOME y persiste`() =
        runTest {
            val prefs = InstancePrefs(FakeDataStore())
            assertEquals(PodcastStartScreen.HOME, prefs.podcastStartScreen.first())
            prefs.setPodcastStartScreen(PodcastStartScreen.QUEUE)
            assertEquals(PodcastStartScreen.QUEUE, prefs.podcastStartScreen.first())
        }

    @Test
    fun `podcastStartScreen corrupto cae a HOME`() =
        runTest {
            val store = FakeDataStore()
            store.state.value = mutablePreferencesOf(stringPreferencesKey("podcast_start_screen") to "XXX")
            val prefs = InstancePrefs(store)
            assertEquals(PodcastStartScreen.HOME, prefs.podcastStartScreen.first())
        }

    @Test
    fun `top charts tiene defaults Espana y todas`() =
        runTest {
            val prefs = InstancePrefs(FakeDataStore())
            assertEquals(TopCountry.SPAIN, prefs.topChartsCountry.first())
            assertEquals(TopGenre.ALL, prefs.topChartsGenre.first())
        }

    @Test
    fun `top charts persiste pais y genero`() =
        runTest {
            val prefs = InstancePrefs(FakeDataStore())
            prefs.setTopChartsCountry(TopCountry.MEXICO)
            prefs.setTopChartsGenre(TopGenre.COMEDY)

            assertEquals(TopCountry.MEXICO, prefs.topChartsCountry.first())
            assertEquals(TopGenre.COMEDY, prefs.topChartsGenre.first())
        }

    @Test
    fun `top charts corrupto cae a los defaults`() =
        runTest {
            val store = FakeDataStore()
            store.state.value =
                mutablePreferencesOf(
                    stringPreferencesKey("top_charts_country") to "XX",
                    stringPreferencesKey("top_charts_genre") to "nope",
                )
            val prefs = InstancePrefs(store)

            assertEquals(TopCountry.SPAIN, prefs.topChartsCountry.first())
            assertEquals(TopGenre.ALL, prefs.topChartsGenre.first())
        }
}
