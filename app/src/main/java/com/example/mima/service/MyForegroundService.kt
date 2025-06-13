package com.example.mima.service

import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.graphics.drawable.Icon
import android.hardware.usb.UsbManager
import android.os.Handler
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.core.os.ExecutorCompat
import com.example.mima.R
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.Executor
import android.net.TetheringManager
import android.content.IntentFilter
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues.TAG
import android.nfc.Tag
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat


class MyForegroundService : Service() {



    companion object {
        const val CHANNEL_ID = "foreground_channel"
        const val NOTIFICATION_ID = 1
        const val LOG_TAG = "MyForegroundService"
    }


    override fun onCreate() {
        super.onCreate()
        Log.d("111","111")
        Timber.d("前台服务已创建")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("前台服务启动中")

        createNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("前台服务运行中")
            .setContentText("通过磁贴启动的服务正在运行...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // TODO: 可在这里启动你的长时间运行任务

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("前台服务已销毁")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "前台服务通知",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}

//class MyForegroundService : Service() {
//
//    init {
//        Services.init { this }
//    }
//
//    //监听USB
//    private val prefs by lazy {
//        getSharedPreferences("tether_settings", Context.MODE_PRIVATE)
//    }
//
//    private var usbReceiver: UsbStateReceiver? = null
//
//    private val ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE"
//
//    //    override fun onStartListening() {
////
////    }
//    override fun onStartListening() {
//        updateTileState(isAutoTetheringEnabled())
//        super.onStartListening()
//        val filter = IntentFilter(ACTION_USB_STATE)
//        usbReceiver = UsbStateReceiver { connected, isTethering ->
//            Timber.d("回调: USB连接=$connected, 共享=$isTethering")
//        }
//        registerReceiver(usbReceiver, filter)
//    }
//
//    override fun onStopListening() {
//        super.onStopListening()
//        usbReceiver?.let { unregisterReceiver(it) }
//    }
//
//
//
//
//
//
//
//    override fun onClick() {
//        val isEnabled = !isAutoTetheringEnabled()
//        prefs.edit().putBoolean("auto_usb_tethering", isEnabled).apply()
//        updateTileState(isEnabled)
//
//        if (isEnabled) {
//            handleAutoTetheringEnable(1, true, object : TetheringManager.StartTetheringCallback {
//                override fun onTetheringStarted() {}
//                override fun onTetheringFailed(error: Int) {}
//            })
//        } else {
//            showToast("已禁用自动USB共享")
//        }
//    }
//
//    private fun requestWriteSettingsPermission() {
//        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
//            data = Uri.parse("package:$packageName")
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK
//        }
//        startActivityAndCollapse(intent)
//    }
//
//
//
//    fun handleAutoTetheringEnable(
//        type: Int, showProvisioningUi: Boolean, callback: TetheringManager.StartTetheringCallback,
//        handler: Handler? = null,
//    ) {
//        if (Build.VERSION.SDK_INT >= 30) {
//            if (!Settings.System.canWrite(this)) {
//                requestWriteSettingsPermission()
//            } else {
//                Timber.d("开始尝试启动网络共享")
//                val executor = handler?.let { ExecutorCompat.create(it) } ?: InPlaceExecutor
//                startTethering(type, false, showProvisioningUi, executor, proxy(callback))
//            }
//        }
//    }
//
//    @RequiresApi(30)
//    fun startTethering(
//        type: Int,
//        exemptFromEntitlementCheck: Boolean,
//        showProvisioningUi: Boolean,
//        executor: Executor,
//        callback: TetheringManager.StartTetheringCallback,
//    ) {
//        Services.tethering.startTethering(
//            TetheringManager.TetheringRequest.Builder(type).also { builder ->
//                if (exemptFromEntitlementCheck) setExemptFromEntitlementCheck(builder, true)
//                setShouldShowEntitlementUi(builder, showProvisioningUi)
//            }.build(), executor, callback
//        )
//    }
//
//    private fun isAutoTetheringEnabled() =
//        prefs.getBoolean("auto_usb_tethering", false)
//
//    private fun updateTileState(isEnabled: Boolean) {
//        qsTile?.apply {
//            state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
//            label = "USB自动共享"
//            icon = Icon.createWithResource(this@UsbTetheringTileService, R.drawable.ic_usb)
//            updateTile()
//        }
//    }
//
//    private fun showToast(message: String) {
//        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
//    }
//}
//
//// -------- 反射方法 --------
//@get:RequiresApi(30)
//private val setExemptFromEntitlementCheck by lazy @TargetApi(30) {
//    TetheringManager.TetheringRequest.Builder::class.java
//        .getDeclaredMethod("setExemptFromEntitlementCheck", Boolean::class.java)
//}
//
//@get:RequiresApi(30)
//private val setShouldShowEntitlementUi by lazy @TargetApi(30) {
//    TetheringManager.TetheringRequest.Builder::class.java
//        .getDeclaredMethod("setShouldShowEntitlementUi", Boolean::class.java)
//}
//
//
//// -------- 服务工具 --------
//object Services {
//    private lateinit var contextInit: () -> Context
//    val context by lazy { contextInit() }
//    fun init(context: () -> Context) {
//        contextInit = context
//    }
//
//    @get:RequiresApi(30)
//    val tethering by lazy { context.getSystemService<TetheringManager>()!! }
//}
//
//// -------- 执行器 & 回调代理 --------
//private object InPlaceExecutor : Executor {
//    override fun execute(command: Runnable) = try {
//        command.run()
//    } catch (e: Exception) {
//        Timber.w(e)
//    }
//}
//
//@RequiresApi(30)
//private fun proxy(callback: TetheringManager.StartTetheringCallback): TetheringManager.StartTetheringCallback {
//    val reference = WeakReference(callback)
//    return object : TetheringManager.StartTetheringCallback {
//        override fun onTetheringStarted() {
//            reference.get()?.onTetheringStarted()
//        }
//
//        override fun onTetheringFailed(error: Int) {
//            reference.get()?.onTetheringFailed(error)
//        }
//    }
//}
//
//class UsbStateReceiver(
//    private val onStateChanged: (connected: Boolean, isTethering: Boolean) -> Unit
//) : BroadcastReceiver() {
//
//    companion object {
//        private const val ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE"
//    }
//
//    override fun onReceive(context: Context, intent: Intent?) {
//        if (intent?.action == ACTION_USB_STATE) {
//            val connected = intent.getBooleanExtra("connected", false)
//            val rndis = intent.getBooleanExtra("rndis", false)
//
//            if (connected) {
//                Timber.d("USB 已连接")
//                if (rndis) {
//                    Timber.d("USB 网络共享（RNDIS）已启用")
//                } else {
//                    Timber.d("USB 连接但未启用网络共享（RNDIS）")
//                }
//            } else {
//                Timber.d("USB 已断开连接")
//            }
//
//            onStateChanged(connected, rndis)
//        }
//    }
//}
