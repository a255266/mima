package com.example.mima.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import java.security.SecureRandom
import android.content.Context
import android.media.MediaCodec
import androidx.media3.decoder.CryptoException
import dagger.hilt.android.qualifiers.ApplicationContext


@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CryptoManager"
        private const val TRANSFORMATION_GCM = "AES/GCM/NoPadding"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "MimaAppKeyAlias"
        private const val TRANSFORMATION_CBC = "AES/CBC/PKCS5Padding"
        private const val DATA_KEY_ALIAS = "MimaAppDataKeyAlias" // 新增：数据密钥别名
        private const val IV_SIZE = 12
        private const val KEY_SIZE = 32
        private const val ENCRYPTED_DATA_KEY_PREF = "encrypted_data_key_pref" // 存储加密后数据密钥的key
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private val secretKey: SecretKey by lazy { getOrCreateKeystoreSecretKey() }

    // 1. 缓存混合加密的对称数据密钥
    private var cachedDataKey: SecretKey? = null

    // 2. 获取或生成并存储加密的数据密钥（用Keystore密钥加密后保存到SharedPreferences）
    fun getOrCreateDataKey(): SecretKey {
        cachedDataKey?.let { return it }

        val encryptedDataKeyBase64 = getEncryptedDataKeyFromPrefs()

        return if (encryptedDataKeyBase64 == null) {
            // 生成随机数据密钥
            val dataKeyBytes = ByteArray(KEY_SIZE)
            SecureRandom().nextBytes(dataKeyBytes)
            val dataKey = SecretKeySpec(dataKeyBytes, "AES")

            // 用Keystore密钥加密数据密钥
            val encryptedDataKey = encryptWithKeystore(dataKeyBytes)

            // 保存加密后的数据密钥
            saveEncryptedDataKeyToPrefs(encryptedDataKey)

            cachedDataKey = dataKey
            dataKey
        } else {
            // 解密已保存的加密数据密钥
            val decryptedBytes = decryptWithKeystore(encryptedDataKeyBase64)
            val dataKey = SecretKeySpec(decryptedBytes, "AES")
            cachedDataKey = dataKey
            dataKey
        }
    }

    /**
     * 用 Keystore 密钥加密纯字节数据（如数据密钥）
     */
    private fun encryptWithKeystore(data: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * 用 Keystore 密钥解密Base64字符串
     */
    private fun decryptWithKeystore(encryptedBase64: String): ByteArray {
        val data = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val iv = data.sliceArray(0 until IV_SIZE)
        val encrypted = data.sliceArray(IV_SIZE until data.size)
        val cipher = Cipher.getInstance(TRANSFORMATION_GCM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    // 3. 用数据密钥加密字段
    fun encryptField(plainText: String): String {
        val dataKey = getOrCreateDataKey()
        val cipher = Cipher.getInstance(TRANSFORMATION_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, dataKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    // 4. 用数据密钥解密字段
    fun decryptField(encryptedText: String): String {
        val dataKey = getOrCreateDataKey()
        val data = Base64.decode(encryptedText, Base64.NO_WRAP)
        val iv = data.sliceArray(0 until IV_SIZE)
        val encrypted = data.sliceArray(IV_SIZE until data.size)
        val cipher = Cipher.getInstance(TRANSFORMATION_GCM)
        cipher.init(Cipher.DECRYPT_MODE, dataKey, GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    // ======= 以下是示例的SharedPreferences读写，换成你项目的存储方式 ========

    private fun getEncryptedDataKeyFromPrefs(): String? {
        val prefs = context.getSharedPreferences("crypto_prefs", Context.MODE_PRIVATE)
        return prefs.getString(ENCRYPTED_DATA_KEY_PREF, null)
    }

    private fun saveEncryptedDataKeyToPrefs(encryptedKey: String) {
        val prefs = context.getSharedPreferences("crypto_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(ENCRYPTED_DATA_KEY_PREF, encryptedKey).apply()
    }

    fun encryptWithAES(plainText: String, key: ByteArray): String {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }  // 12字节随机IV
            val spec = GCMParameterSpec(128, iv)
            val secretKey = SecretKeySpec(key, "AES")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = iv + encrypted  // IV + 密文拼接
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "AES加密失败", e)
            throw RuntimeException("加密失败", e)
        }
    }

    fun decryptWithAES(encryptedText: String, key: ByteArray): String {
        try {
            val data = Base64.decode(encryptedText, Base64.NO_WRAP)
            val iv = data.copyOfRange(0, 12)  // 前12字节是IV
            val encrypted = data.copyOfRange(12, data.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            val secretKey = SecretKeySpec(key, "AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decrypted = cipher.doFinal(encrypted)
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "AES解密失败", e)
            throw RuntimeException("解密失败", e)
        }
    }

    fun encryptWithPassword(data: String, password: String): String {
        return try {
            val secretKey = generatePasswordBasedKey(password)
            val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance(TRANSFORMATION_CBC)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            // 将IV和加密数据一起返回
            Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "密码加密失败", e)
            throw RuntimeException("加密失败", e)
        }
    }

        private fun generatePasswordBasedKey(password: String): SecretKey {
        val keyBytes = password.padEnd(KEY_SIZE, '0').take(KEY_SIZE).toByteArray()
        return SecretKeySpec(keyBytes, "AES")
    }

        fun decryptWithPassword(data: String, password: String): String {
        return try {
            val decoded = Base64.decode(data, Base64.NO_WRAP)
            val iv = decoded.sliceArray(0 until 16)
            val encrypted = decoded.sliceArray(16 until decoded.size)
            val secretKey = generatePasswordBasedKey(password)
            val cipher = Cipher.getInstance(TRANSFORMATION_CBC)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "密码解密失败", e)
            SyncStatusBus.update("解密失败，请确认文件是否正确，秘钥与导出时或上一次云同步时是否一致", SyncStatusType.Error)
            throw RuntimeException("解密失败，请确认秘钥与导出时或上一次云同步时是否一致", e)
        }
    }


    // 5. 你Keystore密钥生成保持不变
    private fun getOrCreateKeystoreSecretKey(): SecretKey {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }
}


//@Singleton
//class CryptoManager @Inject constructor() {
//    companion object {
//        private const val TAG = "CryptoManager"
//        private const val TRANSFORMATION_GCM = "AES/GCM/NoPadding"
//        private const val TRANSFORMATION_CBC = "AES/CBC/PKCS5Padding"
//        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
//        private const val KEY_ALIAS = "MimaAppKeyAlias"
//        private const val IV_SIZE = 12 // GCM推荐12字节IV
//        private const val KEY_SIZE = 32 // AES-256
//    }
//
//    private val secretKey: SecretKey by lazy { getOrCreateSecretKey() }
//
//    /**
//     * 使用Android KeyStore加密(GCM模式)
//     */
//    fun encryptWithKeyStore(plainText: String): String {
//        return try {
//            val cipher = Cipher.getInstance(TRANSFORMATION_GCM)
//            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
//            val iv = cipher.iv
//            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
//            val combined = iv + encrypted
//            Base64.encodeToString(combined, Base64.DEFAULT)
//        } catch (e: Exception) {
//            Log.e(TAG, "KeyStore加密失败", e)
//            throw CryptoException("加密失败", e)
//        }
//    }
//
//    @Deprecated("仅用于兼容旧数据导入", level = DeprecationLevel.WARNING)
//    fun decryptWithPasswordLegacy(data: String, password: String): String {
//        return try {
//            Log.d(TAG, "解密1")
//            val secretKey = generatePasswordBasedKey(password)
//            val iv = ByteArray(16) { 0 } // 固定IV
//            val cipher = Cipher.getInstance(TRANSFORMATION_CBC)
//            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
//            String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8)
//        } catch (e: Exception) {
//            throw CryptoException("旧格式解密失败", e)
//        }
//    }
//    /**
//     * 使用Android KeyStore解密(GCM模式)
//     */
//    fun decryptWithKeyStore(encryptedText: String): String {
//        return try {
//            Log.d(TAG, "解密2") // 重点日志
//            val data = Base64.decode(encryptedText, Base64.DEFAULT)
//            val iv = data.sliceArray(0 until IV_SIZE)
//            val encrypted = data.sliceArray(IV_SIZE until data.size)
//            val cipher = Cipher.getInstance(TRANSFORMATION_GCM)
//            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
//            String(cipher.doFinal(encrypted), Charsets.UTF_8)
//        } catch (e: Exception) {
//            Log.e(TAG, "KeyStore解密失败", e)
//            throw CryptoException("解密失败", e)
//        }
//    }
//
//    /**
//     * 使用密码加密(CBC模式，用于数据导出)
//     */
//    fun encryptWithPassword(data: String, password: String): String {
//        return try {
//            val secretKey = generatePasswordBasedKey(password)
//            val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
//            val cipher = Cipher.getInstance(TRANSFORMATION_CBC)
//            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
//            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
//            // 将IV和加密数据一起返回
//            Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
//        } catch (e: Exception) {
//            Log.e(TAG, "密码加密失败", e)
//            throw CryptoException("加密失败", e)
//        }
//    }
//
//    /**
//     * 使用密码解密(CBC模式，用于数据导入)
//     */
//    fun decryptWithPassword(data: String, password: String): String {
//        return try {
//            val decoded = Base64.decode(data, Base64.NO_WRAP)
//            val iv = decoded.sliceArray(0 until 16)
//            val encrypted = decoded.sliceArray(16 until decoded.size)
//            val secretKey = generatePasswordBasedKey(password)
//            val cipher = Cipher.getInstance(TRANSFORMATION_CBC)
//            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
//            String(cipher.doFinal(encrypted), Charsets.UTF_8)
//        } catch (e: Exception) {
//            Log.e(TAG, "密码解密失败", e)
//            throw CryptoException("解密失败", e)
//        }
//    }
//
//    private fun getOrCreateSecretKey(): SecretKey {
//        return try {
//            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
//            if (keyStore.containsAlias(KEY_ALIAS)) {
//                (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
//            } else {
//                KeyGenerator.getInstance(
//                    KeyProperties.KEY_ALGORITHM_AES,
//                    ANDROID_KEYSTORE
//                ).apply {
//                    init(
//                        KeyGenParameterSpec.Builder(
//                            KEY_ALIAS,
//                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
//                        )
//                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
//                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
//                            .setKeySize(256)
//                            .build()
//                    )
//                }.generateKey()
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "密钥创建/获取失败", e)
//            throw CryptoException("密钥操作失败", e)
//        }
//    }
//
//    private fun generatePasswordBasedKey(password: String): SecretKey {
//        val keyBytes = password.padEnd(KEY_SIZE, '0').take(KEY_SIZE).toByteArray()
//        return SecretKeySpec(keyBytes, "AES")
//    }
//}
//
//class CryptoException(message: String, cause: Throwable? = null) : Exception(message, cause)