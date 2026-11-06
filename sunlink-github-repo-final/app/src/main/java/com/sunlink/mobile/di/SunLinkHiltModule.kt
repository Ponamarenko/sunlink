package com.sunlink.mobile.di

import android.content.Context
import androidx.room.Room
import com.sunlink.mobile.data.repository.InverterRepository
import com.sunlink.mobile.network.ApiService
import com.sunlink.mobile.network.RetrofitClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SunLinkHiltModule {

    @Provides
    @Singleton
    fun provideApiService(): ApiService {
        return RetrofitClient.apiService
    }

    @Provides
    @Singleton
    fun provideInverterRepository(
        apiService: ApiService,
        @ApplicationContext context: Context
    ): InverterRepository {
        return InverterRepository(apiService)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "sunlink_database"
        ).build()
    }
}