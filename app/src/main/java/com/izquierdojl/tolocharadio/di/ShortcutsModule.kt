package com.izquierdojl.tolocharadio.di

import com.izquierdojl.tolocharadio.core.shortcuts.AndroidShortcutPublisher
import com.izquierdojl.tolocharadio.core.shortcuts.ShortcutPublisher
import com.izquierdojl.tolocharadio.core.shortcuts.ShortcutSyncCoordinator
import com.izquierdojl.tolocharadio.domain.shortcuts.ShortcutClearer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Publicación y limpieza de accesos directos del icono (spec 0018). */
@Module
@InstallIn(SingletonComponent::class)
abstract class ShortcutsModule {
    @Binds
    abstract fun bindShortcutPublisher(impl: AndroidShortcutPublisher): ShortcutPublisher

    @Binds
    abstract fun bindShortcutClearer(impl: ShortcutSyncCoordinator): ShortcutClearer
}
