package com.izquierdojl.tolocharadio.domain.shortcuts

import com.izquierdojl.tolocharadio.data.remote.dto.HistoryEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildShortcutStationsUseCaseTest {
    private val useCase = BuildShortcutStationsUseCase()

    private fun entry(
        id: String,
        playedAt: Long,
        name: String = "Emisora $id",
        isCustom: Boolean = false,
    ) = HistoryEntryDto(
        station = StationDto(id = id, name = name, isCustom = isCustom),
        playedAt = playedAt,
    )

    @Test
    fun `deduplica por emisora conservando la reproduccion mas reciente`() {
        val result =
            useCase(
                listOf(
                    entry("a", 1000),
                    entry("b", 2000),
                    entry("a", 5000),
                ),
                maxSlots = 5,
            )

        assertEquals(listOf("a", "b"), result.map { it.stationId })
        assertEquals(listOf(0, 1), result.map { it.rank })
    }

    @Test
    fun `ordena por recencia descendente`() {
        val result =
            useCase(
                listOf(entry("a", 1000), entry("b", 3000), entry("c", 2000)),
                maxSlots = 5,
            )

        assertEquals(listOf("b", "c", "a"), result.map { it.stationId })
    }

    @Test
    fun `recorta al maximo de slots`() {
        val result =
            useCase(
                listOf(entry("a", 4000), entry("b", 3000), entry("c", 2000), entry("d", 1000)),
                maxSlots = 2,
            )

        assertEquals(listOf("a", "b"), result.map { it.stationId })
    }

    @Test
    fun `maxSlots cero o negativo devuelve vacio`() {
        assertTrue(useCase(listOf(entry("a", 1)), maxSlots = 0).isEmpty())
        assertTrue(useCase(listOf(entry("a", 1)), maxSlots = -1).isEmpty())
    }

    @Test
    fun `incluye emisoras personalizadas`() {
        val result = useCase(listOf(entry("custom-1", 5000, isCustom = true)), maxSlots = 5)
        assertEquals(listOf("custom-1"), result.map { it.stationId })
    }

    @Test
    fun `nombre en blanco usa fallback`() {
        val result = useCase(listOf(entry("a", 1000, name = "  ")), maxSlots = 5)
        assertEquals("Emisora", result.single().name)
    }

    @Test
    fun `ignora ids en blanco`() {
        val result = useCase(listOf(entry("", 9000), entry("a", 1000)), maxSlots = 5)
        assertEquals(listOf("a"), result.map { it.stationId })
    }
}
