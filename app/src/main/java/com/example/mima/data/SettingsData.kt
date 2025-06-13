package com.example.mima.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

// 声明 DataStore
val Context.webDavDataStore: DataStore<Preferences> by preferencesDataStore(name = "webdav_settings")

// 公共的 Keys，可跨类访问
object WebDavKeys {
    val SERVER = stringPreferencesKey("webdav_server")
    val ACCOUNT = stringPreferencesKey("webdav_account")
    val PASSWORD = stringPreferencesKey("webdav_password")
    val PASSWORD_LENGTH = stringPreferencesKey("password_length")
    val DECRYPT_KEY = stringPreferencesKey("decrypt_key")
    val FOREGROUND_SERVICE = booleanPreferencesKey("foreground_service_enabled")
    val ALLOW_SYSTEM_SETTINGS = booleanPreferencesKey("allow_system_settings")
}

data class WebDavSettings(
    val server: String = "",
    val account: String = "",
    val password: String = "",
    val passwordLength: String = "16",
    val decryptKey: String = "",
    val foregroundServiceEnabled: Boolean = false,
    val allowSystemSettings: Boolean = false
)

@Singleton
class SettingsData @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // 通用读取方法
    private fun <T> preferenceFlow(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return context.webDavDataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs -> prefs[key] ?: defaultValue }
    }

    // 公开的单项配置 Flow
    val server = preferenceFlow(WebDavKeys.SERVER, "")
    val account = preferenceFlow(WebDavKeys.ACCOUNT, "")
    val password = preferenceFlow(WebDavKeys.PASSWORD, "")
    val passwordLength = preferenceFlow(WebDavKeys.PASSWORD_LENGTH, "16")
    val decryptKey = preferenceFlow(WebDavKeys.DECRYPT_KEY, "")
    val foregroundServiceEnabled = preferenceFlow(WebDavKeys.FOREGROUND_SERVICE, false)
    val allowSystemSettings = preferenceFlow(WebDavKeys.ALLOW_SYSTEM_SETTINGS, false)

    // 组合所有设置
    val webDavSettings: Flow<WebDavSettings> = combine(
        server, account, password, passwordLength, decryptKey,
        foregroundServiceEnabled, allowSystemSettings
    ) { values: Array<Any> ->
        WebDavSettings(
            server = values[0] as String,
            account = values[1] as String,
            password = values[2] as String,
            passwordLength = values[3] as String,
            decryptKey = values[4] as String,
            foregroundServiceEnabled = values[5] as Boolean,
            allowSystemSettings = values[6] as Boolean
        )
    }


    // 保存配置项的方法
    suspend fun <T> updatePreference(key: Preferences.Key<T>, value: T) {
        context.webDavDataStore.edit { prefs ->
            prefs[key] = value
        }
    }
}


//val Context.webDavDataStore: DataStore<Preferences> by preferencesDataStore(name = "webdav_settings")
//
//
//data class WebDavSettings(
//    val server: String,
//    val account: String,
//    val password: String,
//    val passwordLength: String,
//    val decryptKey: String,
//    val foregroundServiceEnabled: Boolean = false,
//    val allowSystemSettings: Boolean = false
//)
//
//@Singleton
//class SettingsData @Inject constructor(
//    private val context: Context
//) {
//    // 所有配置项的 Keys
//    private object WebDavKeys {
//        val SERVER = stringPreferencesKey("webdav_server")
//        val ACCOUNT = stringPreferencesKey("webdav_account")
//        val PASSWORD = stringPreferencesKey("webdav_password")
//        val PASSWORD_LENGTH = stringPreferencesKey("password_length")
//        val DECRYPT_KEY = stringPreferencesKey("decrypt_key")
//        val FOREGROUND_SERVICE = booleanPreferencesKey("foreground_service_enabled")
//        val ALLOW_SYSTEM_SETTINGS = booleanPreferencesKey("allow_system_settings") // 新增
//    }
//
//    // 自动生成 Flow 属性
//    @Suppress("UNCHECKED_CAST")
//    private fun <T> preferenceFlow(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
//        return appContext.webDavDataStore.data.map { prefs ->
//            (prefs[key] ?: defaultValue) as T
//        }
//    }
//
//
//    // --- 公共暴露的 Flow 属性 ---
//    val server: Flow<String> = preferenceFlow(WebDavKeys.SERVER, "")
//    val account: Flow<String> = preferenceFlow(WebDavKeys.ACCOUNT, "")
//    val password: Flow<String> = preferenceFlow(WebDavKeys.PASSWORD, "")
//    val passwordLength: Flow<String> = preferenceFlow(WebDavKeys.PASSWORD_LENGTH, "16")
//    val decryptKey: Flow<String> = preferenceFlow(WebDavKeys.DECRYPT_KEY, "")
//    val foregroundServiceEnabled: Flow<Boolean> = preferenceFlow(WebDavKeys.FOREGROUND_SERVICE, false)
//    val allowSystemSettings: Flow<Boolean> = preferenceFlow(WebDavKeys.ALLOW_SYSTEM_SETTINGS, false) // 新增
//
//
//
//    // 组合所有设置
//    val webDavSettings: Flow<WebDavSettings> = combine(
//        server, account, password, passwordLength, decryptKey,
//        foregroundServiceEnabled, allowSystemSettings
//    ) { values: Array<Any> ->
//        WebDavSettings(
//            server = values[0] as String,
//            account = values[1] as String,
//            password = values[2] as String,
//            passwordLength = values[3] as String,
//            decryptKey = values[4] as String,
//            foregroundServiceEnabled = values[5] as Boolean,
//            allowSystemSettings = values[6] as Boolean
//        )
//    }
//}