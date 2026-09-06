package com.izquierdojl.tolocharadio.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.izquierdojl.tolocharadio.core.ui.ViewMode
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
    fun `sin clave el modo es LIST (FR-005)`() = runTest {
        val prefs = InstancePrefs(FakeDataStore())
        assertEquals(ViewMode.LIST, prefs.viewMode.first())
    }

    @Test
    fun `setViewMode persiste y se lee de vuelta (FR-006)`() = runTest {
        val store = FakeDataStore()
        val prefs = InstancePrefs(store)
        prefs.setViewMode(ViewMode.GRID)
        assertEquals(ViewMode.GRID, prefs.viewMode.first())
        prefs.setViewMode(ViewMode.LIST)
        assertEquals(ViewMode.LIST, prefs.viewMode.first())
    }

    @Test
    fun `valor corrupto cae a LIST sin error (FR-010)`() = runTest {
        val store = FakeDataStore()
        store.state.value = mutablePreferencesOf(stringPreferencesKey("view_mode") to "XXX")
        val prefs = InstancePrefs(store)
        assertEquals(ViewMode.LIST, prefs.viewMode.first())
    }
}
