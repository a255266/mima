package com.example.mima.ui.viewmodels

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mima.data.DataManager
import com.example.mima.data.SettingsData
import com.example.mima.data.WebDavKeys
import com.example.mima.data.WebDavManager
import com.example.mima.data.WebDavSettings
import com.example.mima.data.webDavDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject


@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataManager: DataManager,
    private val settingsData: SettingsData,
    private val webDavManager: WebDavManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val webDavSettings = settingsData.webDavSettings

    fun updateString(key: Preferences.Key<String>, value: String) {
        viewModelScope.launch {
            settingsData.updatePreference(key, value)
        }
    }

    fun updateBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        viewModelScope.launch {
            settingsData.updatePreference(key, value)
        }
    }

    fun toggleForegroundService(enabled: Boolean) {
        viewModelScope.launch {
            appContext.webDavDataStore.edit {
                it[WebDavKeys.FOREGROUND_SERVICE] = enabled
            }
            if (enabled) checkNotificationPermission()
        }
    }

    fun toggleSystemSettingsPermission(enabled: Boolean) {
        viewModelScope.launch {
            appContext.webDavDataStore.edit {
                it[WebDavKeys.ALLOW_SYSTEM_SETTINGS] = enabled
            }
            if (enabled) openSystemSettingsPermission()
        }
    }

    private fun checkNotificationPermission() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        appContext.startActivity(intent)
    }

    private fun openSystemSettingsPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = android.net.Uri.parse("package:${appContext.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        appContext.startActivity(intent)
    }

    suspend fun exportData(key: String): String? = dataManager.exportDataAsEncryptedString(key)

    suspend fun importData(key: String, encryptedData: String): Boolean {
        return dataManager.importDataWithKey(key, encryptedData)
    }

//    //TODO上传下载测试
//    private val _result = MutableStateFlow("")
//    val result: StateFlow<String> = _result
//
//
//    fun uploadTestFileWithDataManager(remotePath: String, password: String?) {
//        viewModelScope.launch {
//            dataManager.uploadEncryptedDataToCloud(remotePath, password)
//            _result.value = "上传成功"
//        }
//    }
//
//    fun downloadTestFileWithDataManager(remotePath: String, password: String?) {
//        viewModelScope.launch {
//            val success = dataManager.downloadAndImportFromCloud(remotePath, password)
//            _result.value = if (success) "下载并导入成功" else "下载失败"
//        }
//    }
//
//
//
//    fun deleteTestFile() {
//        viewModelScope.launch {
//            val success = webDavManager.delete("test/upload_test.txt")
//            _result.value = if (success) "删除成功" else "删除失败"
//        }
//    }
}