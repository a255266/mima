package com.example.mima


import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class VibrationHelper {
    private lateinit var vibrator: Vibrator

    fun init(context: Context) {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(VibratorManager::class.java)).defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java) as Vibrator
        }
    }
    /**
     * 执行自定义震动模式
     * @param timings 震动时间模式数组（毫秒），奇数位为震动时长，偶数位为等待时长
     * @param amplitudes 震动强度数组（0-255），必须与timings长度相同
     * @param repeat 重复次数，-1表示不重复
     */
    fun vibrate(
        timings: LongArray = longArrayOf(0, 2, 5, 30), // 默认：等待0ms -> 震动100ms -> 等待50ms -> 震动100ms
        amplitudes: IntArray? = intArrayOf(0, 2, 0, 8), // 默认使用系统强度
        repeat: Int = -1
    ) {
        if (vibrator == null || !vibrator.hasVibrator()) return

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
                    // 旧API只支持简单震动
                    if (timings.isNotEmpty()) vibrator.vibrate(timings[0])
                }
            }
        } catch (e: Exception) {
            // 处理API调用异常
        }
    }

    /**
     * 简单震动（兼容所有API版本）
     * @param duration 震动时长（毫秒）
     */
    fun simpleVibrate(duration: Long = 100) {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(duration)
            }
        }
    }

    companion object {
        /**
         * 预定义震动模式
         */
        object Patterns {
            val CLICK = longArrayOf(0, 30)
            val DOUBLE_CLICK = longArrayOf(0, 30, 100, 30)
            val HEAVY = longArrayOf(0, 100, 50, 200)
        }
    }
}