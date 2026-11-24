package com.izquierdojl.tolocharadio.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.izquierdojl.tolocharadio.domain.playback.HeadsetControlSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Caché del flujo de ajustes de auriculares (spec 0060). */
class HeadsetControlSettingsCacheTest {
    private class FakeDataStore : DataStore<Preferences> {
        val state = MutableStateFlow<Preferences>(emptyPreferences())

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val next = transform(state.value)
            state.value = next
            return next
        }
    }

    @Test
    fun `arranca con el default y emite los cambios`() =
        runTest {
            val prefs = InstancePrefs(FakeDataStore())
            val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
            val cache = HeadsetControlSettingsCache(prefs, scope)
            try {
                assertEquals(HeadsetControlSettings.DEFAULT, cache.current())

                prefs.setHeadsetSkipBackSeconds(5)
                prefs.setHeadsetSkipForwardSeconds(60)
                prefs.setHeadsetSkipGesturesEnabled(false)
                advanceUntilIdle()

                assertEquals(
                    HeadsetControlSettings(
                        skipBackSeconds = 5,
                        skipForwardSeconds = 60,
                        gesturesEnabled = false,
                    ),
                    cache.current(),
                )
            } finally {
                scope.cancel()
            }
        }
}
