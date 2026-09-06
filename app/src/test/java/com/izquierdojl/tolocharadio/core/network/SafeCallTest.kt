package com.izquierdojl.tolocharadio.core.network

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class SafeCallTest {
    @Test
    fun `200 con cuerpo devuelve Ok`() =
        runTest {
            val r = safeCall { Response.success("hola") }
            assertTrue(r is ApiResult.Ok && r.value == "hola")
        }

    @Test
    fun `422 devuelve Validation`() =
        runTest {
            val err =
                Response.error<String>(
                    422,
                    """{"error":{"code":"INVALID","message":"m","status":422,"details":[]}}"""
                        .toResponseBody("application/json".toMediaType()),
                )
            val r = safeCall { err }
            assertTrue((r as ApiResult.Err).error is DomainError.Validation)
        }

    @Test
    fun `IOException devuelve Unavailable`() =
        runTest {
            val r = safeCall<String> { throw java.io.IOException("down") }
            assertTrue((r as ApiResult.Err).error is DomainError.Unavailable)
        }
}

