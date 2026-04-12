package com.caltrack.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // MealRepository uses constructor @Inject, so Hilt can provide it
    // automatically without explicit binding. Additional repository
    // bindings (interface -> impl) can be added here as needed.
}
