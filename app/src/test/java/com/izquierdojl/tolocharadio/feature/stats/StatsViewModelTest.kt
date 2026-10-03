package com.izquierdojl.tolocharadio.feature.stats

import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsCountryEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsCountryListDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsGenreEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsGenreListDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsHabitEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsHabitListDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsRecentEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsRecentListDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTimelineDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTimelineEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTopEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTopListDto
import com.izquierdojl.tolocharadio.domain.stats.ComputeStatsSummaryUseCase
import com.izquierdojl.tolocharadio.domain.stats.LoadStatsUseCase
import com.izquierdojl.tolocharadio.domain.stats.StatsBundle
import com.izquierdojl.tolocharadio.domain.stats.StatsPeriod
import com.izquierdojl.tolocharadio.domain.stats.TimelineGranularity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Transiciones de `StatsUiState`, periodo y errores (US1: SC-004, FR-003/FR-004/FR-007). */
@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val load: LoadStatsUseCase = mockk()
    private val compute = ComputeStatsSummaryUseCase()
    private val errorLogger: StatsErrorLogger = mockk(relaxed = true)

    private val top =
        StatsTopListDto(listOf(StatsTopEntryDto(StationDto("s1", "Rock FM"), 60_000)))
    private val timeline =
        StatsTimelineDto("day", listOf(StatsTimelineEntryDto("2026-10-01", 60_000)))
    private val okBundle = ApiResult.Ok(StatsBundle(top, timeline))
    private val emptyBundle =
        ApiResult.Ok(StatsBundle(StatsTopListDto(), StatsTimelineDto()))

    private fun vm() = StatsViewModel(load, compute, errorLogger)

    @Test
    fun `carga inicial con datos muestra Content con resumen y granularidad dia`() =
        runTest {
            coEvery { load(any()) } returns okBundle
            val v = vm()
            advanceUntilIdle()

            val state = v.ui.value as StatsUiState.Content
            assertEquals(60_000L, state.summary.totalMs)
            assertEquals("Rock FM", state.summary.topStation?.name)
            assertEquals(TimelineGranularity.DAY, state.granularity)
            assertEquals(StatsPeriod.THIRTY, v.period.value)
        }

    @Test
    fun `periodo sin escuchas muestra estado vacio`() =
        runTest {
            coEvery { load(any()) } returns emptyBundle
            val v = vm()
            advanceUntilIdle()
            assertTrue(v.ui.value is StatsUiState.Empty)
        }

    @Test
    fun `error de credenciales marca la edicion de servidor (principio IV)`() =
        runTest {
            coEvery { load(any()) } returns ApiResult.Err(DomainError.Unauthorized("bad_credentials"))
            val v = vm()
            advanceUntilIdle()

            val state = v.ui.value as StatsUiState.Error
            assertTrue(state.isAuthError)
            assertTrue(state.message.isNotBlank())
        }

    @Test
    fun `error de red muestra mensaje accionable sin marcar autenticacion`() =
        runTest {
            coEvery { load(any()) } returns ApiResult.Err(DomainError.Unavailable("down"))
            val v = vm()
            advanceUntilIdle()

            val state = v.ui.value as StatsUiState.Error
            assertFalse(state.isAuthError)
            assertTrue(state.message.isNotBlank())
        }

    @Test
    fun `cambiar de periodo recarga todo con granularidad semana (SC-004, FR-004)`() =
        runTest {
            coEvery { load(any()) } returnsMany listOf(okBundle, emptyBundle)
            val v = vm()
            advanceUntilIdle()
            assertTrue(v.ui.value is StatsUiState.Content)

            v.onPeriodChange(StatsPeriod.NINETY)
            advanceUntilIdle()

            val state = v.ui.value
            assertTrue(
                "El estado nuevo no conserva datos del periodo anterior",
                state is StatsUiState.Empty,
            )
            assertEquals(StatsPeriod.NINETY, v.period.value)
            coVerify(exactly = 2) { load(any()) }
        }

    @Test
    fun `la granularidad de 90 dias es semana`() =
        runTest {
            coEvery { load(any()) } returns okBundle
            val v = vm()
            advanceUntilIdle()

            v.onPeriodChange(StatsPeriod.NINETY)
            advanceUntilIdle()

            assertEquals(TimelineGranularity.WEEK, (v.ui.value as StatsUiState.Content).granularity)
        }

    @Test
    fun `retry recarga tras un error (FR-007)`() =
        runTest {
            coEvery { load(any()) } returnsMany listOf(ApiResult.Err(DomainError.Unavailable("down")), okBundle)
            val v = vm()
            advanceUntilIdle()
            assertTrue(v.ui.value is StatsUiState.Error)

            v.retry()
            advanceUntilIdle()

            assertTrue(v.ui.value is StatsUiState.Content)
        }

    @Test
    fun `el bundle de US2 incluye generos y paises`() =
        runTest {
            coEvery { load(any()) } returns
                ApiResult.Ok(
                    StatsBundle(
                        top = top,
                        timeline = timeline,
                        genres = StatsGenreListDto(listOf(StatsGenreEntryDto("rock", 40_000))),
                        countries = StatsCountryListDto(listOf(StatsCountryEntryDto("Espana", "ES", 40_000))),
                    ),
                )
            val v = vm()
            advanceUntilIdle()

            val state = v.ui.value as StatsUiState.Content
            assertEquals("rock", state.bundle.genres.items.single().genre)
            assertEquals("Espana", state.bundle.countries.items.single().country)
        }

    @Test
    fun `el cambio de periodo actualiza tambien generos y paises (SC-004)`() =
        runTest {
            coEvery { load(any()) } returnsMany
                listOf(
                    ApiResult.Ok(
                        StatsBundle(
                            top = top,
                            timeline = timeline,
                            genres = StatsGenreListDto(listOf(StatsGenreEntryDto("rock", 40_000))),
                            countries = StatsCountryListDto(listOf(StatsCountryEntryDto("Espana", "ES", 40_000))),
                        ),
                    ),
                    ApiResult.Ok(StatsBundle(top = top, timeline = timeline)),
                )
            val v = vm()
            advanceUntilIdle()

            v.onPeriodChange(StatsPeriod.SEVEN)
            advanceUntilIdle()

            val state = v.ui.value as StatsUiState.Content
            assertTrue(state.bundle.genres.items.isEmpty())
            assertTrue(state.bundle.countries.items.isEmpty())
        }

    @Test
    fun `el bundle de US3 incluye habitos con matriz 7x24 y recientes ordenados`() =
        runTest {
            coEvery { load(any()) } returns
                ApiResult.Ok(
                    StatsBundle(
                        top = top,
                        timeline = timeline,
                        habits =
                            StatsHabitListDto(
                                listOf(
                                    StatsHabitEntryDto(weekday = 2, hour = 10, totalMs = 60_000),
                                    StatsHabitEntryDto(weekday = 2, hour = 10, totalMs = 10_000),
                                    StatsHabitEntryDto(weekday = 6, hour = 23, totalMs = 5_000),
                                ),
                            ),
                        recent =
                            StatsRecentListDto(
                                listOf(
                                    StatsRecentEntryDto(
                                        StationDto("s1", "Rock FM"),
                                        startedAt = 1_000,
                                        durationMs = 100,
                                    ),
                                    StatsRecentEntryDto(
                                        StationDto("s2", "Jazz FM"),
                                        startedAt = 3_000,
                                        durationMs = 200,
                                    ),
                                ),
                            ),
                    ),
                )
            val v = vm()
            advanceUntilIdle()

            val state = v.ui.value as StatsUiState.Content
            assertEquals(7, state.habitsMatrix.size)
            assertEquals(24, state.habitsMatrix.first().size)
            assertEquals(70_000L, state.habitsMatrix[2][10])
            assertEquals(0L, state.habitsMatrix[0][0])
            assertEquals(
                listOf(3_000L, 1_000L),
                state.bundle.recent.items.map { it.startedAt },
            )
        }

    @Test
    fun `un error de carga se registra con log estructurado y el exito no lo hace (Principio IV)`() =
        runTest {
            val error = DomainError.Unavailable("down")
            coEvery { load(any()) } returnsMany listOf(ApiResult.Err(error), okBundle)
            val v = vm()
            advanceUntilIdle()

            verify(exactly = 1) { errorLogger.log(error) }

            v.retry()
            advanceUntilIdle()

            assertTrue(v.ui.value is StatsUiState.Content)
            verify(exactly = 1) { errorLogger.log(any()) }
        }
}
