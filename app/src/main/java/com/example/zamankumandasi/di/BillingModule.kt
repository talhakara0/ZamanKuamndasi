package com.example.zamankumandasi.di

import android.content.Context
import com.example.zamankumandasi.billing.BillingManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingModule {

    @Provides
    @Singleton
    fun provideBillingManager(@ApplicationContext context: Context): BillingManager {
        return BillingManager(context)
    }
}
