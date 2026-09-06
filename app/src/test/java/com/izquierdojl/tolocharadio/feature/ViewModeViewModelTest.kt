package com.izquierdojl.tolocharadio.feature

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.ui.ViewMode
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Fake JVM de DataStore (data + updateData; `edit` delega en updateData). */
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
 * ViewModeViewModel (spec 008): estado inicial desde la preferencia,
 * toggle optimista que persiste y valor compartido para todos los suscriptores.
 */
class ViewModeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val prefs: InstancePrefs = mockk(relaxed = true)

    private fun vm(persisted: ViewMode): ViewModeViewModel {
        every { prefs.viewMode } returns flowOf(persisted)
        return ViewModeViewModel(prefs)
    }

    @Test
    fun `estado inicial es el modo persistido (FR-004)`() =
        runTest {
            assertEquals(ViewMode.GRID, vm(ViewMode.GRID).mode.value)
        }

    @Test
    fun `sin preferencia previa el modo es LIST (FR-005)`() =
        runTest {
            assertEquals(ViewMode.LIST, vm(ViewMode.LIST).mode.value)
        }

    @Test
    fun `toggle alterna LIST a GRID y persiste (FR-003, FR-006)`() =
        runTest {
            val v = vm(ViewMode.LIST)
            v.toggle()
            assertEquals(ViewMode.GRID, v.mode.value)
            coVerify { prefs.setViewMode(ViewMode.GRID) }
        }

    @Test
    fun `toggle doble vuelve a LIST y sobrescribe (FR-006)`() =
        runTest {
            val v = vm(ViewMode.LIST)
            v.toggle()
            v.toggle()
            assertEquals(ViewMode.LIST, v.mode.value)
            coVerify { prefs.setViewMode(ViewMode.LIST) }
        }

    @Test
    fun `todos los suscriptores observan el mismo modo (FR-004)`() =
        runTest {
            val v = vm(ViewMode.LIST)
            v.mode.test {
                assertEquals(ViewMode.LIST, awaitItem())
                v.toggle()
                assertEquals(ViewMode.GRID, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `dos instancias sucesivas heredan el último modo guardado (FR-006, US-2)`() =
        runTest {
            val store = FakeDataStore()
            val prefs = InstancePrefs(store)
            val primera = ViewModeViewModel(prefs)
            primera.toggle()
            val segunda = ViewModeViewModel(prefs)
            assertEquals(ViewMode.GRID, segunda.mode.value)
        }

    @Test
    fun `instancia nueva sin escrituras previas arranca en LIST (US-2 escenario 2)`() =
        runTest {
            val v = ViewModeViewModel(InstancePrefs(FakeDataStore()))
            assertEquals(ViewMode.LIST, v.mode.value)
        }
}
