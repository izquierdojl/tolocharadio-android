package com.example.tolocharadio.core.network

/** Error de dominio tipado desde `{error:{code,message,status,details}}` (FR-011). */
sealed interface DomainError {
    /** 401: sesión expirada o credenciales inválidas. */
    data class Unauthorized(val code: String) : DomainError

    /** 404: emisora o recurso inexistente. */
    data class NotFound(val code: String) : DomainError

    /** 409: conflicto (email en uso, favorito duplicado…). */
    data class Conflict(val code: String, val message: String) : DomainError

    /** 422: validación con detalle por campo. */
    data class Validation(val details: List<FieldError>) : DomainError

    /** 503 / sin red: reintentable, admite caché local. */
    data class Unavailable(val message: String, val cause: Throwable? = null) : DomainError

    /** Cualquier otro fallo. */
    data class Unknown(val message: String, val cause: Throwable? = null) : DomainError
}

/** Detalle de validación `{field,message}`. */
data class FieldError(val field: String, val message: String)

/**
 * Mensaje del servidor para un campo concreto (422 con `details`), o
 * null si el error no trae detalle para ese campo. Permite mostrar el
 * error junto al campo del formulario (FR-010).
 */
fun DomainError.fieldMessage(field: String): String? =
    (this as? DomainError.Validation)?.details?.firstOrNull { it.field == field }?.message

/**
 * Mensaje accionable en español para el usuario. Nunca expone texto
 * técnico crudo del servidor ni PII (constitución IV).
 */
fun DomainError.userMessage(): String =
    when (this) {
        is DomainError.Unauthorized -> "Tu sesión ha expirado. Vuelve a iniciar sesión."
        is DomainError.NotFound -> "No se ha encontrado lo que buscabas."
        is DomainError.Conflict -> message.ifBlank { "Esa acción entra en conflicto con tus datos." }
        is DomainError.Validation -> details.firstOrNull()?.message ?: "Revisa los datos introducidos."
        is DomainError.Unavailable -> "Servicio no disponible. Comprueba tu conexión e inténtalo de nuevo."
        is DomainError.Unknown -> "Algo ha fallado. Inténtalo de nuevo."
    }
