package com.ziweistar.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 【结缘星宿】头像沙盒
 *
 * 设计文档 7.1：
 *  - 裁剪后剥离全部 EXIF 地理与相机元数据
 *  - 转为加密二进制流存储于应用私有目录
 *  - 设置 .nomedia 防止被系统相册或第三方应用扫描
 */
object AvatarStore {

    private const val DIR = "vault/stars"
    private const val EXT = ".ziw"

    fun dir(context: Context): File {
        val d = File(context.filesDir, DIR)
        if (!d.exists()) d.mkdirs()
        ensureNoMedia(d)
        return d
    }

    private fun ensureNoMedia(dir: File) {
        val nomedia = File(dir, ".nomedia")
        if (!nomedia.exists()) runCatching { nomedia.createNewFile() }
        // 父目录也标记
        val parent = dir.parentFile
        if (parent != null && parent.exists()) {
            val pn = File(parent, ".nomedia")
            if (!pn.exists()) runCatching { pn.createNewFile() }
        }
    }

    /**
     * 导入并裁剪为圆形方形头像（512×512），加密落盘。
     * @return 相对文件名（不含目录）
     */
    fun importAndStore(context: Context, sourceUri: android.net.Uri): String? {
        val bitmap = decodeDownsampled(context, sourceUri, 1024) ?: return null
        val square = centerCropSquare(bitmap)
        val scaled = Bitmap.createScaledBitmap(square, 512, 512, true)

        val bytes = java.io.ByteArrayOutputStream().use { bos ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, bos)
            bos.toByteArray()
        }
        if (square !== bitmap) bitmap.recycle()
        if (scaled !== square) scaled.recycle()

        // 剥离 EXIF：重新编码后的 JPEG 已不含原始 EXIF；此处显式擦拭一遍
        val cleaned = stripExifViaReencode(bytes) ?: bytes

        val key = CryptoUtil.getOrCreateMasterKey()
        val (ct, iv) = CryptoUtil.encryptAesGcm(key, cleaned)

        val name = UUID.randomUUID().toString() + EXT
        val out = File(dir(context), name)
        FileOutputStream(out).use { fos ->
            fos.write(byteArrayOf(1)) // 版本号
            fos.write(iv)
            fos.write(ct)
        }
        return name
    }

    /** 读取并解密头像；返回可直接给 Compose 的 Bitmap */
    fun load(context: Context, relativeName: String?): Bitmap? {
        if (relativeName.isNullOrBlank()) return null
        val f = File(dir(context), relativeName)
        if (!f.exists()) return null
        return runCatching {
            val all = f.readBytes()
            if (all.size < 13) return null
            val iv = all.copyOfRange(1, 13)
            val ct = all.copyOfRange(13, all.size)
            val key = CryptoUtil.getOrCreateMasterKey()
            val plain = CryptoUtil.decryptAesGcm(key, ct, iv)
            BitmapFactory.decodeByteArray(plain, 0, plain.size)
        }.getOrNull()
    }

    fun delete(context: Context, relativeName: String?) {
        if (relativeName.isNullOrBlank()) return
        runCatching { File(dir(context), relativeName).delete() }
    }

    fun wipeAll(context: Context) {
        runCatching {
            val d = dir(context)
            d.listFiles()?.forEach { f ->
                if (f.isFile) overwriteAndDelete(f)
            }
        }
        runCatching { File(context.filesDir, "vault").deleteRecursively() }
        runCatching { File(context.filesDir, "vault").mkdirs() }
    }

    private fun overwriteAndDelete(f: File) {
        runCatching {
            val len = f.length()
            java.io.RandomAccessFile(f, "rws").use { raf ->
                val zeros = ByteArray(8192)
                var w = 0L
                while (w < len) {
                    val n = minOf(8192L, len - w).toInt()
                    raf.write(zeros, 0, n)
                    w += n
                }
                raf.fd.sync()
            }
        }
        f.delete()
    }

    // ---------- 图像处理 ----------

    private fun decodeDownsampled(context: Context, uri: android.net.Uri, maxSize: Int): Bitmap? {
        return runCatching {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            var sample = 1
            while (opts.outWidth / sample > maxSize * 2 || opts.outHeight / sample > maxSize * 2) {
                sample *= 2
            }
            val opts2 = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts2)
            }
        }.getOrNull()
    }

    private fun centerCropSquare(src: Bitmap): Bitmap {
        val size = minOf(src.width, src.height)
        val x = (src.width - size) / 2
        val y = (src.height - size) / 2
        return Bitmap.createBitmap(src, x, y, size, size)
    }

    /** 通过重编码剥离 EXIF（JPEG 重编码不携带原元数据） */
    private fun stripExifViaReencode(bytes: ByteArray): ByteArray? = runCatching {
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val out = java.io.ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 88, out)
        bmp.recycle()
        out.toByteArray()
    }.getOrNull()

    @Suppress("unused")
    private fun readExif(context: Context, uri: android.net.Uri): ExifInterface? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
    }.getOrNull()
}
