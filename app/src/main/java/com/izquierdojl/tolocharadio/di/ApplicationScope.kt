package com.izquierdojl.tolocharadio.di

import javax.inject.Qualifier

/** Qualifier for an application-scoped [kotlinx.coroutines.CoroutineScope]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
