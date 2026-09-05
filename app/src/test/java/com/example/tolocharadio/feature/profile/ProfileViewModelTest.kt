package com.example.tolocharadio.feature.profile

import android.content.Context
import app.cash.turbine.test
import com.example.tolocharadio.MainDispatcherRule
import com.example.tolocharadio.core.network.ApiResult
import com.example.tolocharadio.data.local.InstancePrefs
import com.example.tolocharadio.data.remote.dto.ThemeDto
import com.example.tolocharadio.data.remote.dto.UserDto
import com.example.tolocharadio.data.repo.AuthRepo
import com.example.tolocharadio.data.repo.UserRepo
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val users: UserRepo = mockk(relaxed = true)
    private val auth: AuthRepo = mockk(relaxed = true)
    private val prefs: InstancePrefs = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)

    @Test
    fun `me OK rellena nombre y tema`() =
        runTest {
            coEvery { users.me() } returns
                ApiResult.Ok(UserDto(1, "a@b.c", "Ana", ThemeDto.LIGHT, 0))
            var s = ProfileUi()
            ProfileViewModel(users, auth, prefs, context).ui.test {
                s = awaitItem()
                if (s.loading) s = awaitItem()
            }
            assertEquals("Ana", s.name)
            assertEquals(false, s.darkTheme)
        }
}
