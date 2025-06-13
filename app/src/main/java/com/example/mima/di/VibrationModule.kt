package com.example.mima.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import com.example.mima.data.CryptoManager
import com.example.mima.util.VibrationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// VibrationModule.kt
@Module
@InstallIn(SingletonComponent::class)
object VibrationModule {

    @Provides
    @Singleton
    fun provideVibrator(@ApplicationContext context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(VibratorManager::class.java)
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java) as Vibrator
        }
    }
}