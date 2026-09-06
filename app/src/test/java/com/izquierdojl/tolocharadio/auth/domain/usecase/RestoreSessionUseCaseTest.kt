package com.izquierdojl.tolocharadio.auth.domain.usecase

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.session.AuthState
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.remote.dto.ThemeDto
import com.izquierdojl.tolocharadio.data.remote.dto.UserDto
import com.izquierdojl.tolocharadio.data.repo.AuthRepo
import com.izquierdojl.tolocharadio.domain.auth.RestoreSessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RestoreSessionUseCaseTest {
    private lateinit var useCase: RestoreSessionUseCase
    private val authRepo = mockk<AuthRepo>(relaxed = true)
    private val session = mockk<SessionManager>(relaxed = true)
    private val tokens = mockk<TokenStore>(relaxed = true)

    @Before
    fun setup() {
        useCase = RestoreSessionUseCase(authRepo, session, tokens)
    }

    @Test
    fun `invoke returns current state when not Loading`() = runTest {
        every { session.authState.value } returns AuthState.Unauthenticated()
        val result = useCase()
        assertTrue(result is AuthState.Unauthenticated)
    }

    @Test
    fun `invoke returns Unauthenticated when no refresh token`() = runTest {
        every { session.authState.value } returns AuthState.Loading
        every { tokens.getRefresh() } returns null
        coEvery { session.logout() } answers {
            // Simulate logout setting state to Unauthenticated
            every { session.authState.value } returns AuthState.Unauthenticated()
        }
        val result = useCase()
        assertTrue(result is AuthState.Unauthenticated)
    }

    @Test
    fun `invoke returns Authenticated on successful refresh`() = runTest {
        val user = UserDto(id = 1, email = "test@test.com", name = "Test", theme = ThemeDto.DARK, createdAt = 0L)
        every { session.authState.value } returnsMany listOf(
            AuthState.Loading,
            AuthState.Authenticated(user)
        )
        every { tokens.getRefresh() } returns "refresh-token"
        coEvery { authRepo.refreshNow("refresh-token") } returns ApiResult.Ok(user)
        coEvery { session.setAuthenticated(user, any(), any()) } returns Unit
        val result = useCase()
        assertTrue(result is AuthState.Authenticated)
    }

    @Test
    fun `invoke returns Unauthenticated on failed refresh`() = runTest {
        every { session.authState.value } returnsMany listOf(
            AuthState.Loading,
            AuthState.Unauthenticated()
        )
        every { tokens.getRefresh() } returns "invalid-refresh"
        coEvery { authRepo.refreshNow("invalid-refresh") } returns ApiResult.Err(
            com.izquierdojl.tolocharadio.core.network.DomainError.Unauthorized("expired")
        )
        coEvery { session.logout() } returns Unit
        val result = useCase()
        assertTrue(result is AuthState.Unauthenticated)
    }

    @Test
    fun `sin refresh pero con credenciales guardadas re-autentica (FR-006)`() = runTest {
        val user = UserDto(id = 1, email = "test@test.com", name = "Test", theme = ThemeDto.DARK, createdAt = 0L)
        every { session.authState.value } returnsMany listOf(
            AuthState.Loading,
            AuthState.Authenticated(user)
        )
        every { tokens.getRefresh() } returns null
        every { tokens.getActiveServerId() } returns "srv1"
        every { tokens.getServerCredentials("srv1") } returns
            TokenStore.ServerCredentials(null, "test@test.com", "secreta123")
        coEvery { authRepo.login("test@test.com", "secreta123") } returns ApiResult.Ok(user)

        val result = useCase()

        assertTrue(result is AuthState.Authenticated)
        coVerify(exactly = 0) { session.logout() }
    }

    @Test
    fun `sin refresh ni credenciales pide login`() = runTest {
        every { session.authState.value } returnsMany listOf(
            AuthState.Loading,
            AuthState.Unauthenticated()
        )
        every { tokens.getRefresh() } returns null
        every { tokens.getActiveServerId() } returns "srv1"
        every { tokens.getServerCredentials("srv1") } returns
            TokenStore.ServerCredentials(null, null, null)
        coEvery { session.logout() } returns Unit

        val result = useCase()

        assertTrue(result is AuthState.Unauthenticated)
        coVerify { session.logout() }
    }

    @Test
    fun `refresh revocado re-autentica con password cifrado (FR-006b)`() = runTest {
        val user = UserDto(id = 1, email = "test@test.com", name = "Test", theme = ThemeDto.DARK, createdAt = 0L)
        every { session.authState.value } returnsMany listOf(
            AuthState.Loading,
            AuthState.Authenticated(user)
        )
        every { tokens.getRefresh() } returnsMany listOf("revocado", "nuevo-refresh")
        coEvery { authRepo.refreshNow("revocado") } returns ApiResult.Err(
            com.izquierdojl.tolocharadio.core.network.DomainError.Unauthorized("revoked")
        )
        every { tokens.getActiveServerId() } returns "srv1"
        every { tokens.getServerCredentials("srv1") } returns
            TokenStore.ServerCredentials("revocado", "test@test.com", "secreta123")
        coEvery { authRepo.login("test@test.com", "secreta123") } returns ApiResult.Ok(user)

        val result = useCase()

        assertTrue(result is AuthState.Authenticated)
        coVerify(exactly = 0) { session.logout() }
    }

    @Test
    fun `refresh revocado sin password guardado pide login`() = runTest {
        every { session.authState.value } returnsMany listOf(
            AuthState.Loading,
            AuthState.Unauthenticated()
        )
        every { tokens.getRefresh() } returns "revocado"
        coEvery { authRepo.refreshNow("revocado") } returns ApiResult.Err(
            com.izquierdojl.tolocharadio.core.network.DomainError.Unauthorized("revoked")
        )
        every { tokens.getActiveServerId() } returns "srv1"
        every { tokens.getServerCredentials("srv1") } returns
            TokenStore.ServerCredentials("revocado", "test@test.com", null)
        coEvery { session.logout() } returns Unit

        val result = useCase()

        assertTrue(result is AuthState.Unauthenticated)
        coVerify { session.logout() }
    }
}
