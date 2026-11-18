package com.izquierdojl.tolocharadio.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.izquierdojl.tolocharadio.core.ui.navigation.ContentMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class PodcastPrefsFakeStore : DataStore<Preferences> {
    val state = MutableStateFlow<Preferences>(emptyPreferences())
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val next = transform(state.value)
        state.value = next
        return next
    }
}

/** Modo de contenido y sección inicial (T018, Red). */
class ContentModePrefsTest {
    @Test
    fun `sin claves el modo es RADIO`() =
        runTest {
            val prefs = InstancePrefs(PodcastPrefsFakeStore())
            assertEquals(ContentMode.RADIO, prefs.contentMode.first())
            assertEquals(ContentMode.RADIO, prefs.defaultSection.first())
        }

    @Test
    fun `setContentMode y setDefaultSection persisten`() =
        runTest {
            val prefs = InstancePrefs(PodcastPrefsFakeStore())
            prefs.setContentMode(ContentMode.PODCAST)
            prefs.setDefaultSection(ContentMode.PODCAST)
            assertEquals(ContentMode.PODCAST, prefs.contentMode.first())
            assertEquals(ContentMode.PODCAST, prefs.defaultSection.first())
        }

    @Test
    fun `valor corrupto cae a RADIO sin error`() =
        runTest {
            val store = PodcastPrefsFakeStore()
            store.state.value = mutablePreferencesOf(stringPreferencesKey("content_mode") to "XXX")
            val prefs = InstancePrefs(store)
            assertEquals(ContentMode.RADIO, prefs.contentMode.first())
        }
}
