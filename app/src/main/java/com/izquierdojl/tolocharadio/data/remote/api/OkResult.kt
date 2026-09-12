package com.izquierdojl.tolocharadio.data.remote.api

import kotlinx.serialization.Serializable

/** Respuesta genérica de operaciones que confirman con `{ok:true}`. */
@Serializable
data class OkResult(val ok: Boolean = true)
