package com.caltrack.app.di

import android.content.Context
import androidx.room.Room
import com.caltrack.app.data.local.CaltrackDatabase
import com.caltrack.app.data.local.dao.DailyGoalDao
import com.caltrack.app.data.local.dao.MealDao
import com.caltrack.app.data.remote.AuthTokenInterceptor
import com.caltrack.app.data.remote.CaltrackApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = "http://10.0.2.2:8098/" // Android emulator → host localhost (backend local port)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CaltrackDatabase {
        return Room.databaseBuilder(
            context,
            CaltrackDatabase::class.java,
            "caltrack.db"
        )
            .addMigrations(CaltrackDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideMealDao(database: CaltrackDatabase): MealDao = database.mealDao()

    @Provides
    @Singleton
    fun provideDailyGoalDao(database: CaltrackDatabase): DailyGoalDao = database.dailyGoalDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(authTokenInterceptor: AuthTokenInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authTokenInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)   // scan can take a while
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideCaltrackApi(retrofit: Retrofit): CaltrackApi =
        retrofit.create(CaltrackApi::class.java)
}
