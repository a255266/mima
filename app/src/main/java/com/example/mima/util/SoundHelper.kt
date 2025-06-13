// SoundHelper.kt
package com.example.mima.util

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundHelper @Inject constructor(
    private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null

    fun playSound(@RawRes soundResId: Int) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, soundResId)?.apply {
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}