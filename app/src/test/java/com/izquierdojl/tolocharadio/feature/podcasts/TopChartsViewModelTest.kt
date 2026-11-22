package com.izquierdojl.tolocharadio.feature.podcasts

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSearchResult
import com.izquierdojl.tolocharadio.data.repo.PodcastCapabilities
import com.izquierdojl.tolocharadio.data.repo.SystemRepo
import com.izquierdojl.tolocharadio.domain.podcasts.LoadTopChartsUseCase
import com.izquierdojl.tolocharadio.domain.podcasts.TopChartsLoad
import com.izquierdojl.tolocharadio.domain.podcasts.TopCountry
import com.izquierdojl.tolocharadio.domain.podcasts.TopGenre
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class TopChartsFakeStore : DataStore<Preferences> {
    val state = MutableStateFlow<Preferences>(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val next = transform(state.value)
        state.value = next
        return next
    }
}

/** Estado del ranking, capacidad, filtros y degradación (spec 0053). */
class TopChartsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val store = TopChartsFakeStore()
    private val prefs = InstancePrefs(store)
    private val system: SystemRepo = mockk()
    private val load: LoadTopChartsUseCase = mockk()

    private val item = PodcastSearchResult("Podcast A", "Autor", "https://a.example/f.xml")

    private fun vm(
        capabilities: ApiResult<PodcastCapabilities> = ApiResult.Ok(PodcastCapabilities(topCharts = true)),
        loadResult: ApiResult<TopChartsLoad> = ApiResult.Ok(TopChartsLoad(listOf(item))),
    ): TopChartsViewModel {
        coEvery { system.podcastCapabilities(any()) } returns capabilities
        coEvery { load(any(), any()) } returns loadResult
        return TopChartsViewModel(prefs, system, load)
    }

    @Test
    fun `carga inicial con Espana y todas`() =
        runTest {
            val v = vm()
            val ui = v.ui.first { !it.loading && it.items.isNotEmpty() }

            assertTrue(ui.visible)
            assertEquals(TopCountry.SPAIN, ui.country)
            assertEquals(TopGenre.ALL, ui.genre)
            assertEquals(listOf(item), ui.items)
            assertFalse(ui.genreUnavailable)
        }

    @Test
    fun `sin ranking soportado oculta la seccion sin pedir ranking`() =
        runTest {
            val v = vm(capabilities = ApiResult.Ok(PodcastCapabilities(topCharts = false)))
            val ui = v.ui.first { !it.loading }

            assertFalse(ui.visible)
            coVerify(exactly = 0) { load(any(), any()) }
        }

    @Test
    fun `un fallo transitorio de capacidad muestra error con reintento`() =
        runTest {
            val v = vm(capabilities = ApiResult.Err(DomainError.Unavailable("sin red")))
            val ui = v.ui.first { !it.loading }

            assertTrue(ui.visible)
            assertNotNull(ui.error)
            assertFalse(ui.isAuthError)
        }

    @Test
    fun `un error de credenciales activa Editar servidor`() =
        runTest {
            val v = vm(loadResult = ApiResult.Err(DomainError.Unauthorized("UNAUTHORIZED")))
            val ui = v.ui.first { !it.loading }

            assertTrue(ui.visible)
            assertTrue(ui.isAuthError)
            assertNotNull(ui.error)
        }

    @Test
    fun `un 404 oculta la seccion sin error`() =
        runTest {
            val v = vm(loadResult = ApiResult.Err(DomainError.NotFound("NOT_FOUND")))
            val ui = v.ui.first { !it.loading }

            assertFalse(ui.visible)
            assertEquals(null, ui.error)
        }

    @Test
    fun `errores desconocidos y de validacion muestran error`() =
        runTest {
            val unknown = vm(loadResult = ApiResult.Err(DomainError.Unknown("x")))
            assertNotNull(unknown.ui.first { !it.loading }.error)

            val validation = vm(loadResult = ApiResult.Err(DomainError.Validation(emptyList())))
            assertNotNull(validation.ui.first { !it.loading }.error)
        }

    @Test
    fun `el fallback de categoria publica el aviso`() =
        runTest {
            val v = vm(loadResult = ApiResult.Ok(TopChartsLoad(listOf(item), genreUnavailable = true)))
            val ui = v.ui.first { !it.loading }

            assertTrue(ui.visible)
            assertTrue(ui.genreUnavailable)
            assertEquals(null, ui.error)
        }

    @Test
    fun `selectCountry persiste y recarga con el nuevo pais`() =
        runTest {
            val v = vm()
            v.ui.first { !it.loading }

            v.selectCountry(TopCountry.MEXICO)

            val ui = v.ui.first { !it.loading && it.country == TopCountry.MEXICO }
            assertEquals(TopCountry.MEXICO, ui.country)
            coVerify { load(TopCountry.MEXICO, TopGenre.ALL) }
            assertEquals(TopCountry.MEXICO, prefs.topChartsCountry.first())
        }

    @Test
    fun `selectGenre persiste y recarga con la categoria`() =
        runTest {
            val v = vm()
            v.ui.first { !it.loading }

            v.selectGenre(TopGenre.COMEDY)

            val ui = v.ui.first { !it.loading && it.genre == TopGenre.COMEDY }
            assertEquals(TopGenre.COMEDY, ui.genre)
            coVerify { load(TopCountry.SPAIN, TopGenre.COMEDY) }
            assertEquals(TopGenre.COMEDY, prefs.topChartsGenre.first())
        }

    @Test
    fun `la ultima seleccion gana`() =
        runTest {
            val v = vm()
            v.ui.first { !it.loading }

            v.selectCountry(TopCountry.MEXICO)
            v.selectCountry(TopCountry.CHILE)

            val ui = v.ui.first { !it.loading && it.country == TopCountry.CHILE }
            assertEquals(TopCountry.CHILE, ui.country)
        }

    @Test
    fun `lee la preferencia persistida al crear el view model`() =
        runTest {
            store.state.value =
                mutablePreferencesOf(
                    stringPreferencesKey("top_charts_country") to "mx",
                    stringPreferencesKey("top_charts_genre") to "1303",
                )
            val v = vm()
            val ui = v.ui.first { !it.loading }

            assertEquals(TopCountry.MEXICO, ui.country)
            assertEquals(TopGenre.COMEDY, ui.genre)
        }

    @Test
    fun `retry fuerza la capacidad solo cuando fallo`() =
        runTest {
            coEvery { system.podcastCapabilities(false) } returns
                ApiResult.Err(DomainError.Unavailable("sin red"))
            coEvery { system.podcastCapabilities(true) } returns
                ApiResult.Ok(PodcastCapabilities(topCharts = true))
            coEvery { load(any(), any()) } returns ApiResult.Ok(TopChartsLoad(listOf(item)))

            val v = TopChartsViewModel(prefs, system, load)
            v.ui.first { !it.loading && it.error != null }

            v.retry()

            v.ui.first { !it.loading && it.error == null }
            coVerify { system.podcastCapabilities(true) }
        }
}
