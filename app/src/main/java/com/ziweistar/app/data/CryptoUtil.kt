package com.ziweistar.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM 加解密工具。
 * 用于：① 数据库口令的 Keystore 封装；② 星宿头像二进制的本地加密落盘。
 */
object CryptoUtil {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val MASTER_ALIAS = "ziwei_vault_master_v1"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12

    /** 取得（或创建）硬件背书的 AES-256 主密钥 */
    fun getOrCreateMasterKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getEntry(MASTER_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                MASTER_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    /** 销毁主密钥（焚毁天机时调用） */
    fun destroyMasterKey() {
        runCatching {
            val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            if (ks.containsAlias(MASTER_ALIAS)) ks.deleteEntry(MASTER_ALIAS)
        }
    }

    fun randomBytes(size: Int): ByteArray = ByteArray(size).also { SecureRandom().nextBytes(it) }

    fun randomHex(size: Int): String = randomBytes(size).joinToString("") { "%02x".format(it) }

    /** 返回 (密文, IV) */
    fun encryptAesGcm(key: SecretKey, plain: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.doFinal(plain) to cipher.iv
    }

    fun decryptAesGcm(key: SecretKey, cipherText: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(cipherText)
    }

    /** 生成随机口令（32 字节十六进制 ASCII），用作 SQLCipher key */
    fun newPassphrase(): ByteArray =
        randomHex(32).toByteArray(Charsets.US_ASCII)

    // ---- Base64 便捷封装（用于 SharedPreferences 持久化） ----
    fun enc64(b: ByteArray): String = Base64.encodeToString(b, Base64.NO_WRAP)
    fun dec64(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)
}

/**
 * SQLCipher 口令仓库：口令本身以 AES-GCM 包裹后落 SharedPreferences，
 * 包裹密钥由 Android Keystore 硬件保护。
 */
object VaultKeyStore {

    private const val PREF = "ziwei_vault_meta"
    private const val K_PASS = "enc_db_passphrase"
    private const val K_IV = "enc_db_iv"

    @Volatile
    private var cached: ByteArray? = null

    fun passphrase(context: Context): ByteArray {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val prefs = context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            val encPass = prefs.getString(K_PASS, null)
            val encIv = prefs.getString(K_IV, null)

            val key = CryptoUtil.getOrCreateMasterKey()

            if (encPass != null && encIv != null) {
                val restored = runCatching {
                    CryptoUtil.decryptAesGcm(
                        key,
                        CryptoUtil.dec64(encPass),
                        CryptoUtil.dec64(encIv),
                    )
                }.getOrNull()
                if (restored != null && restored.isNotEmpty()) {
                    cached = restored
                    return restored
                }
            }
            val fresh = CryptoUtil.newPassphrase()
            val (ct, iv) = CryptoUtil.encryptAesGcm(key, fresh)
            prefs.edit()
                .putString(K_PASS, CryptoUtil.enc64(ct))
                .putString(K_IV, CryptoUtil.enc64(iv))
                .apply()
            cached = fresh
            return fresh
        }
    }

    /** 焚毁天机：清除口令与 Keystore 主密钥 */
    fun wipe(context: Context) {
        cached = null
        runCatching {
            context.applicationContext
                .getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().clear().commit()
        }
        CryptoUtil.destroyMasterKey()
    }
}
