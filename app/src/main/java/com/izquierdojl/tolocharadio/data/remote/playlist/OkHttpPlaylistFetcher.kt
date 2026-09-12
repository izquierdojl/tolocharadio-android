package com.izquierdojl.tolocharadio.data.remote.playlist

import com.izquierdojl.tolocharadio.domain.playback.PlaylistContent
import com.izquierdojl.tolocharadio.domain.playback.PlaylistFetchException
import com.izquierdojl.tolocharadio.domain.playback.PlaylistFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.Reader
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Descarga listas de texto con OkHttp **sin** interceptores de autenticación
 * (spec 0019, FR-010): el token nunca se envía a hosts de terceros.
 */
@Singleton
class OkHttpPlaylistFetcher
    @Inject
    constructor() : PlaylistFetcher {
        private val client: OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()

        override suspend fun fetch(url: String): PlaylistContent =
            withContext(Dispatchers.IO) {
                val request =
                    Request.Builder()
                        .url(url)
                        .header(USER_AGENT_HEADER, USER_AGENT)
                        .build()
                try {
                    client.newCall(request).execute().use { response ->
                        val body = response.body ?: throw PlaylistFetchException("Respuesta sin cuerpo")
                        if (!response.isSuccessful) {
                            throw PlaylistFetchException("HTTP ${response.code} al descargar la lista")
                        }
                        val text = body.charStream().use(::readLimited)
                        PlaylistContent(text = text, finalUrl = response.request.url.toString())
                    }
                } catch (e: IOException) {
                    throw PlaylistFetchException("No se pudo descargar la lista", e)
                } catch (e: IllegalArgumentException) {
                    throw PlaylistFetchException("URL de lista inválida", e)
                }
            }

        private fun readLimited(reader: Reader): String {
            val builder = StringBuilder()
            val chars = CharArray(READ_CHUNK)
            var total = 0
            while (total < MAX_CHARS) {
                val read = reader.read(chars)
                if (read < 0) break
                builder.append(chars, 0, read)
                total += read
            }
            return builder.toString()
        }

        private companion object {
            const val USER_AGENT_HEADER = "User-Agent"
            const val USER_AGENT = "TolochaRadio-Android"
            const val TIMEOUT_SECONDS = 10L
            const val MAX_CHARS = 1_048_576
            const val READ_CHUNK = 8192
        }
    }
