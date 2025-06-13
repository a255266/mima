// MyApplication.kt
package com.example.mima

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber


@HiltAndroidApp
class MimaApplication : Application(){
    override fun onCreate() {
        super.onCreate()

        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                // 这里直接使用 Android Log，不限制日志等级
                Log.println(priority, tag ?: "Timber", message)
            }
        })
    }
}
