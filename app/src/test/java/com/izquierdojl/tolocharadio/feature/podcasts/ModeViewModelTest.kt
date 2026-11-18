package com.izquierdojl.tolocharadio.feature.podcasts

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.ui.navigation.ContentMode
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.repo.PodcastCapabilities
import com.izquierdojl.tolocharadio.data.repo.SystemRepo
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class ModePrefsFakeStore : DataStore<Preferences> {
    val state = MutableStateFlow<Preferences>(emptyPreferences())
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val next = transform(state.value)
        state.value = next
        return next
    }
}

/** Modo global: capacidades, conmutación y arranque (T025, Red→Green). */
class ModeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val store = ModePrefsFakeStore()
    private val prefs = InstancePrefs(store)
    private val system: SystemRepo = mockk()

    private fun vm(enabled: Boolean = true): ModeViewModel {
        coEvery { system.podcastCapabilities() } returns
            ApiResult.Ok(PodcastCapabilities(podcasts = enabled))
        return ModeViewModel(prefs, system)
    }

    @Test
    fun `refresh publica capacidades del servidor`() =
        runTest {
            val v = vm(enabled = true)
            assertTrue(v.ui.first { !it.loading }.podcastsEnabled)
        }

    @Test
    fun `sin capacidades el modo queda en Radio sin rastro podcast`() =
        runTest {
            val v = vm(enabled = false)
            val ui = v.ui.first { !it.loading }
            assertFalse(ui.podcastsEnabled)
            assertEquals(ContentMode.RADIO, ui.mode)
        }

    @Test
    fun `setMode persiste el modo`() =
        runTest {
            val v = vm()
            v.setMode(ContentMode.PODCAST)
            assertEquals(ContentMode.PODCAST, prefs.contentMode.first())
        }
}
