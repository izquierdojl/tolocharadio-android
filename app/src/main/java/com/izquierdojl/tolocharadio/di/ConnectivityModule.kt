package com.izquierdojl.tolocharadio.di

import com.izquierdojl.tolocharadio.core.network.AndroidConnectivityMonitor
import com.izquierdojl.tolocharadio.core.network.ConnectivityMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binding de conectividad (spec 0042, R5). */
@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityModule {
    @Binds
    @Singleton
    abstract fun bindConnectivityMonitor(impl: AndroidConnectivityMonitor): ConnectivityMonitor
}
