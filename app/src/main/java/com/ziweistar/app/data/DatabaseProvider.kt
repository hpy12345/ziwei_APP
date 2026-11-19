package com.ziweistar.app.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.ziweistar.app.data.db.ZiWeiDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

/**
 * 数据库单例：Room + SQLCipher 全盘加密。
 *
 * 设计文档 7.1：底层采用 SQLCipher 对本地 SQLite 数据库进行全盘加密，
 * 密钥存储于 Android Keystore 硬件安全模块中。
 */
object DatabaseProvider {

    @Volatile
    private var instance: ZiWeiDatabase? = null

    fun get(context: Context): ZiWeiDatabase {
        instance?.let { return it }
        synchronized(this) {
            instance?.let { return it }
            System.loadLibrary("sqlcipher")
            val passphrase = VaultKeyStore.passphrase(context)
            val factory = SupportOpenHelperFactory(passphrase)
            val db = Room.databaseBuilder(
                context.applicationContext,
                ZiWeiDatabase::class.java,
                ZiWeiDatabase.NAME,
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
            instance = db
            return db
        }
    }

    /** 焚毁天机：关闭并按字节覆写删除数据库文件 */
    fun destroy(context: Context) {
        runCatching { instance?.close() }
        instance = null
        runCatching {
            val files = listOf(
                ZiWeiDatabase.NAME,
                "${ZiWeiDatabase.NAME}-wal",
                "${ZiWeiDatabase.NAME}-shm",
                "${ZiWeiDatabase.NAME}-journal",
            )
            for (f in files) {
                val file = File(context.applicationContext.getDatabasePath(f).absolutePath)
                if (file.exists()) {
                    overwriteAndDelete(file)
                }
            }
        }
        VaultKeyStore.wipe(context)
    }

    private fun overwriteAndDelete(file: File) {
        runCatching {
            val len = file.length()
            java.io.RandomAccessFile(file, "rws").use { raf ->
                val zeros = ByteArray(8192)
                var written = 0L
                while (written < len) {
                    val n = minOf(8192L, len - written).toInt()
                    raf.write(zeros, 0, n)
                    written += n
                }
                raf.fd.sync()
            }
        }
        file.delete()
    }
}
