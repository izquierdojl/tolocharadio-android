package com.example.tolocharadio.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException

/** Resultado de una llamada al backend `/api/v1`. */
sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>

    data class Err(val error: DomainError) : ApiResult<Nothing>
}

@Serializable
private data class ErrorEnvelope(val error: ErrorBody)

@Serializable
private data class ErrorBody(
    val code: String = "UNKNOWN",
    val message: String = "",
    val status: Int = 0,
    val details: List<ErrorDetail> = emptyList(),
)

@Serializable
private data class ErrorDetail(val field: String = "", val message: String = "")

private val envelopeJson = Json { ignoreUnknownKeys = true }

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_NOT_FOUND = 404
private const val HTTP_CONFLICT = 409
private const val HTTP_UNPROCESSABLE = 422
private const val HTTP_UNAVAILABLE = 503

/** Mapea un `Response` HTTP al error de dominio (FR-011). */
fun mapHttpError(
    code: Int,
    rawBody: String?,
): DomainError {
    if (code == HTTP_UNAUTHORIZED) return DomainError.Unauthorized(extractCode(rawBody))
    val body = rawBody?.let { runCatching { envelopeJson.decodeFromString<ErrorEnvelope>(it).error }.getOrNull() }
    return when (code) {
        HTTP_NOT_FOUND -> DomainError.NotFound(body?.code ?: "NOT_FOUND")
        HTTP_CONFLICT -> DomainError.Conflict(body?.code ?: "CONFLICT", body?.message.orEmpty())
        HTTP_UNPROCESSABLE -> DomainError.Validation(body?.details?.map { FieldError(it.field, it.message) }.orEmpty())
        HTTP_UNAVAILABLE -> DomainError.Unavailable(body?.message.orEmpty())
        else -> DomainError.Unknown(body?.message?.ifBlank { null } ?: "Error $code")
    }
}

private fun extractCode(rawBody: String?): String =
    rawBody?.let { runCatching { envelopeJson.decodeFromString<ErrorEnvelope>(it).error.code }.getOrNull() }
        ?: "UNAUTHORIZED"

/** Ejecuta una llamada Retrofit capturando red y HTTP a [ApiResult]. */
@Suppress("TooGenericExceptionCaught") // Fallback intencional: todo error no-IO mapea a Unknown (FR-011).
suspend fun <T> safeCall(call: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) ApiResult.Ok(body) else ApiResult.Err(DomainError.Unknown("Respuesta vacía"))
        } else {
            ApiResult.Err(mapHttpError(response.code(), response.errorBody()?.string()))
        }
    } catch (e: IOException) {
        ApiResult.Err(DomainError.Unavailable("Sin conexión", e))
    } catch (e: Exception) {
        ApiResult.Err(DomainError.Unknown(e.message ?: "Error inesperado", e))
    }
}
