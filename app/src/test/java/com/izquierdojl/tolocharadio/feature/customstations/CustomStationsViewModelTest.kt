package com.izquierdojl.tolocharadio.feature.customstations

import app.cash.turbine.test
import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.network.FieldError
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.CustomStationsRepo
import com.izquierdojl.tolocharadio.data.repo.CustomStationsResult
import com.izquierdojl.tolocharadio.data.repo.FavoritesRepo
import com.izquierdojl.tolocharadio.domain.ObserveCustomStationsUseCase
import com.izquierdojl.tolocharadio.domain.ValidateCustomStationUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CustomStationsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val observe: ObserveCustomStationsUseCase = mockk()
    private val repo: CustomStationsRepo = mockk(relaxed = true)
    private val favorites: FavoritesRepo = mockk(relaxed = true)
    private val validate = ValidateCustomStationUseCase()

    private val stations =
        listOf(
            StationDto("c1", "Radio Sierra", url = "https://x/live", isCustom = true),
            StationDto("c2", "Radio Mar", url = "https://y/live", isCustom = true),
        )

    private fun vm() = CustomStationsViewModel(observe, repo, favorites, validate)

    private suspend fun CustomStationsViewModel.awaitSettled(): CustomStationsUiState {
        var s: CustomStationsUiState = CustomStationsUiState.Loading
        ui.test {
            s = awaitItem()
            while (s is CustomStationsUiState.Loading) s = awaitItem()
        }
        return s
    }

    @Test
    fun `init con personalizadas muestra Content`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(CustomStationsResult(stations, offline = false))
            val s = vm().awaitSettled() as CustomStationsUiState.Content
            assertEquals(listOf("c1", "c2"), s.items.map { it.id })
            assertEquals(false, s.offline)
        }

    @Test
    fun `lista vacia muestra Empty`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(CustomStationsResult(emptyList(), offline = false))
            assertTrue(vm().awaitSettled() is CustomStationsUiState.Empty)
        }

    @Test
    fun `error sin cache muestra Error`() =
        runTest {
            coEvery { observe() } returns ApiResult.Err(DomainError.Unavailable("x"))
            assertTrue(vm().awaitSettled() is CustomStationsUiState.Error)
        }

    @Test
    fun `offline con cache muestra Content offline`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(CustomStationsResult(stations, offline = true))
            val s = vm().awaitSettled() as CustomStationsUiState.Content
            assertEquals(true, s.offline)
        }

    @Test
    fun `submit con nombre vacio muestra error de campo sin llamar red`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(CustomStationsResult(stations, offline = false))
            val vm = vm()
            vm.awaitSettled()
            vm.onNameChange("   ")
            vm.onUrlChange("https://x/live")
            vm.onSubmit()
            assertEquals("Escribe un nombre para la emisora.", vm.form.value.nameError)
            coVerify(exactly = 0) { repo.create(any(), any()) }
        }

    @Test
    fun `submit con url invalida muestra error de campo sin llamar red`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(CustomStationsResult(stations, offline = false))
            val vm = vm()
            vm.awaitSettled()
            vm.onNameChange("Radio Sierra")
            vm.onUrlChange("ftp://x/live")
            vm.onSubmit()
            assertEquals("La URL debe empezar por http:// o https://.", vm.form.value.urlError)
            coVerify(exactly = 0) { repo.create(any(), any()) }
        }

    @Test
    fun `submit valido limpia formulario y muestra confirmacion`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(CustomStationsResult(stations, offline = false))
            val vm = vm()
            vm.awaitSettled()
            coEvery { repo.create("Radio Sierra", "https://x/live") } returns
                ApiResult.Ok(StationDto("c3", "Radio Sierra", url = "https://x/live", isCustom = true))
            vm.messages.test {
                vm.onNameChange("Radio Sierra")
                vm.onUrlChange("https://x/live")
                vm.onSubmit()
                assertEquals("Emisora personalizada añadida", awaitItem())
            }
            assertEquals("", vm.form.value.name)
            assertEquals(false, vm.form.value.submitting)
        }

    @Test
    fun `submit con 422 muestra error por campo sin snackbar generico`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(CustomStationsResult(stations, offline = false))
            val vm = vm()
            vm.awaitSettled()
            coEvery { repo.create(any(), any()) } returns
                ApiResult.Err(DomainError.Validation(listOf(FieldError("url", "Esa URL no responde."))))
            vm.onNameChange("Radio Sierra")
            vm.onUrlChange("https://x/live")
            vm.onSubmit()
            // Espera a que termine el envío
            while (vm.form.value.submitting) kotlinx.coroutines.delay(10)
            assertEquals("Esa URL no responde.", vm.form.value.urlError)
            assertNull(vm.form.value.nameError)
        }

    @Test
    fun `onDelete elimina optimistamente e invalida favoritos`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(CustomStationsResult(stations, offline = false))
            val vm = vm()
            vm.awaitSettled()
            coEvery { repo.delete("c1") } returns ApiResult.Ok(Unit)
            vm.onDelete("c1")
            val s = vm.ui.value as CustomStationsUiState.Content
            assertEquals(listOf("c2"), s.items.map { it.id })
            coVerify { favorites.list() }
        }

    @Test
    fun `onDelete error revierte estado y emite snackbar`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(CustomStationsResult(stations, offline = false))
            val vm = vm()
            vm.awaitSettled()
            coEvery { repo.delete("c1") } returns ApiResult.Err(DomainError.Unavailable("fail"))
            vm.messages.test {
                vm.onDelete("c1")
                val s = vm.ui.value as CustomStationsUiState.Content
                assertEquals(listOf("c1", "c2"), s.items.map { it.id })
                assertTrue(awaitItem().isNotEmpty())
            }
        }

    @Test
    fun `onDelete ultima emisora muestra Empty`() =
        runTest {
            val single = listOf(stations[0])
            coEvery { observe() } returns ApiResult.Ok(CustomStationsResult(single, offline = false))
            val vm = vm()
            vm.awaitSettled()
            coEvery { repo.delete("c1") } returns ApiResult.Ok(Unit)
            vm.onDelete("c1")
            assertTrue(vm.ui.value is CustomStationsUiState.Empty)
        }
}
