package com.example.mima.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VibrationHelper @Inject constructor(
    private val vibrator: Vibrator
) {
    /**
     * 检查设备是否支持震动
     */
    fun hasVibrator(): Boolean = vibrator.hasVibrator()

    /**
     * 执行自定义震动模式
     * @param timings 震动时间模式数组（毫秒）
     * @param amplitudes 震动强度数组（0-255）
     * @param repeat 重复次数，-1表示不重复
     */
    fun vibrate(
        timings: LongArray = longArrayOf(0, 2, 5, 30), // 默认：等待0ms -> 震动100ms -> 等待50ms -> 震动100ms
        amplitudes: IntArray? = intArrayOf(0, 2, 0, 8), // 默认使用系统强度
        repeat: Int = -1
    ) {
        if (!hasVibrator()) return
//        Log.d("Vibration", "使用VibrationEffect波形震动")
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    val effect = if (amplitudes != null && vibrator.hasAmplitudeControl()) {
                        require(timings.size == amplitudes.size) { "Timings和Amplitudes数组长度必须相同" }
                        VibrationEffect.createWaveform(timings, amplitudes, repeat)
                    } else {
                        VibrationEffect.createWaveform(timings, repeat)
                    }
                    vibrator.vibrate(effect)
                }
                else -> @Suppress("DEPRECATION") {
                    vibrator.vibrate(timings, repeat)
                }
            }
        } catch (e: Exception) {
            Log.e("VibrationHelper", "震动失败", e)
        }
    }

    /**
     * 简单震动
     * @param duration 震动时长（毫秒）
     * @param amplitude 震动强度（1-255）
     */
    fun simpleVibrate(
        duration: Long = 100,
        amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE
    ) {
        if (!hasVibrator()) return
//        Log.d("Vibration", "使用VibrationEffect简单震动")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (e: Exception) {
            Log.e("VibrationHelper", "简单震动失败", e)
        }
    }

    /**
     * 取消震动
     */
    fun cancel() {
        try {
            vibrator.cancel()
        } catch (e: Exception) {
            Log.e("VibrationHelper", "取消震动失败", e)
        }
    }

    object Patterns {
        val CLICK = longArrayOf(0, 30)
        val DOUBLE_CLICK = longArrayOf(0, 30, 100, 30)
        val HEAVY = longArrayOf(0, 100, 50, 200)
    }
}