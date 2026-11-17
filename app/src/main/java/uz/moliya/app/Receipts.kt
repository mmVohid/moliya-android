package uz.moliya.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import java.io.File

/** Chek rasmlari ilovaning ichki xotirasida saqlanadi (Excel zaxiraga kirmaydi). */
object Receipts {
    private const val MAX_SIDE = 1280

    private fun dir(context: Context) = File(context.filesDir, "receipts").apply { mkdirs() }

    fun file(context: Context, name: String) = File(dir(context), name)

    /** Kamera uchun vaqtinchalik fayl va uning URI'si */
    fun cameraTarget(context: Context): Pair<File, Uri> {
        val folder = File(context.cacheDir, "cam").apply { mkdirs() }
        val f = File(folder, "capture_${System.currentTimeMillis()}.jpg")
        return f to FileProvider.getUriForFile(context, context.packageName + ".files", f)
    }

    /** Rasmni kichraytirib saqlaydi; fayl nomini qaytaradi (xato bo'lsa null). */
    fun save(context: Context, source: Uri): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > MAX_SIDE * 2 || bounds.outHeight / sample > MAX_SIDE * 2) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return null
            val scale = MAX_SIDE.toFloat() / Math.max(bmp.width, bmp.height)
            val out = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
            val name = "r_${System.currentTimeMillis()}.jpg"
            file(context, name).outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 82, it) }
            name
        } catch (e: Exception) {
            null
        }
    }

    fun load(context: Context, name: String): ImageBitmap? {
        if (name.isEmpty()) return null
        val f = file(context, name)
        if (!f.exists()) return null
        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
        return BitmapFactory.decodeFile(f.path, opts)?.asImageBitmap()
    }

    fun delete(context: Context, name: String) {
        if (name.isNotEmpty()) file(context, name).delete()
    }
}
