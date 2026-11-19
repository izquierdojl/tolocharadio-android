package com.izquierdojl.tolocharadio.domain.podcasts

/** Milisegundos por segundo. */
private const val MS_PER_SEC = 1_000L

/** Salto atrás del episodio en segundos; compartido por UI y notificación. */
const val EPISODE_SKIP_BACK_SEC = 15L

/** Salto adelante del episodio en segundos; compartido por UI y notificación. */
const val EPISODE_SKIP_FORWARD_SEC = 30L

/** Salto atrás en milisegundos para los incrementos del reproductor. */
const val EPISODE_SKIP_BACK_MS = EPISODE_SKIP_BACK_SEC * MS_PER_SEC

/** Salto adelante en milisegundos para los incrementos del reproductor. */
const val EPISODE_SKIP_FORWARD_MS = EPISODE_SKIP_FORWARD_SEC * MS_PER_SEC
