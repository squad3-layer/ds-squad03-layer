package com.domleondev.designsystem.di

import com.domleondev.designsystem.contract.DesignSystem
import com.domleondev.designsystem.data.repository.FirebaseRemoteConfigRepository
import com.domleondev.designsystem.domain.renderer.UiRenderer
import com.domleondev.designsystem.domain.repository.RemoteConfigRepository
import com.domleondev.designsystem.parser.GsonJsonParser
import com.domleondev.designsystem.parser.JsonParser
import com.domleondev.designsystem.presentation.renderer.BackendDrivenUiRenderer
import com.domleondev.designsystem.runtime.DesignSystemImpl
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DesignSystemModule {

    @Binds
    @Singleton
    abstract fun bindJsonParser(
        gsonJsonParser: GsonJsonParser
    ): JsonParser

    @Binds
    @Singleton
    abstract fun bindUiRenderer(
        uiRenderer: BackendDrivenUiRenderer
    ): UiRenderer

    @Binds
    @Singleton
    abstract fun bindRemoteConfigRepository(
        remoteConfigRepositoryImpl: FirebaseRemoteConfigRepository
    ): RemoteConfigRepository

    @Module
    @InstallIn(ActivityComponent::class)
    abstract class DsModule {
        @Binds
        @ActivityScoped
        internal abstract fun bindDesignSystem(impl: DesignSystemImpl): DesignSystem
    }

    companion object {
        @Provides
        @Singleton
        fun provideGson(): Gson {
            return Gson()
        }
    }
}