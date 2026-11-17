package uz.moliya.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Haftada bir marta ma'lumotni foydalanuvchi tanlagan papkaga Excel qilib saqlaydi. */
class AutoBackupWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val ctx = applicationContext
        val settings = Prefs.load(ctx)
        if (!settings.autoBackup || settings.backupFolder.isEmpty()) return Result.success()
        return try {
            AutoBackup.runNow(ctx, settings.backupFolder)
            Prefs.setLastAutoBackup(ctx, System.currentTimeMillis())
            Result.success()
        } catch (e: Exception) {
            AutoBackup.notifyFailure(ctx, e.message ?: "Noma'lum xato")
            Result.success()
        }
    }
}

object AutoBackup {
    private const val WORK_NAME = "moliya_avto_backup"
    private const val CHANNEL_ID = "avto_backup"
    private const val NOTIFICATION_ID = 1002
    private const val PREFIX = "moliya_auto_"

    fun schedule(context: Context, replace: Boolean = false) {
        val weekday = Prefs.load(context).backupWeekday
        val delayMs = if (weekday in 1..7) {
            val now = Calendar.getInstance()
            val next = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, weekday)
                set(Calendar.HOUR_OF_DAY, 3)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (!after(now)) add(Calendar.DAY_OF_YEAR, 7)
            }
            next.timeInMillis - now.timeInMillis
        } else {
            TimeUnit.HOURS.toMillis(1)
        }
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Hozir zaxira oladi. Xato bo'lsa, sababini matn sifatida qaytaradi (muvaffaqiyatda null). */
    fun backupNow(context: Context): String? {
        val settings = Prefs.load(context)
        if (settings.backupFolder.isEmpty()) return "Avval zaxira papkasini tanlang."
        return try {
            runNow(context, settings.backupFolder)
            Prefs.setLastAutoBackup(context, System.currentTimeMillis())
            null
        } catch (e: Exception) {
            e.message ?: "Noma'lum xato. Papkani qayta tanlab ko'ring."
        }
    }

    /** Yoqilgan zahoti birinchi nusxani oladi, haftalik jadval esa keyin ishlaydi. */
    fun runOnce(context: Context) {
        WorkManager.getInstance(context).enqueue(androidx.work.OneTimeWorkRequestBuilder<AutoBackupWorker>().build())
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Papkaga yangi fayl yozadi va faqat oxirgi 4 ta avtomatik faylni qoldiradi. */
    fun runNow(context: Context, folder: String) {
        val resolver = context.contentResolver
        val tree = Uri.parse(folder)
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val (bytes, encrypted) = BackupCrypto.export(context, Storage.load(context))
        val name = PREFIX + Dates.iso(Dates.today()) + "_" + (System.currentTimeMillis() % 100000) + if (encrypted) ".mbk" else ".xlsx"
        val created = DocumentsContract.createDocument(
            resolver,
            parent,
            if (encrypted) "application/octet-stream" else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            name,
        ) ?: throw IllegalStateException("Papkada fayl yaratib bo'lmadi. Papkani qayta tanlang.")
        val out = resolver.openOutputStream(created, "w")
            ?: throw IllegalStateException("Faylga yozib bo'lmadi.")
        out.use { it.write(bytes) }
        prune(context, tree)
    }

    private fun prune(context: Context, tree: Uri) {
        val resolver = context.contentResolver
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val files = ArrayList<Triple<Uri, String, Long>>()
        resolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            ),
            null,
            null,
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                val fileName = c.getString(1) ?: continue
                if (fileName.startsWith(PREFIX) && (fileName.endsWith(".xlsx") || fileName.endsWith(".mbk"))) {
                    files.add(Triple(DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0)), fileName, c.getLong(2)))
                }
            }
        }
        files.sortedByDescending { it.third }.drop(Prefs.load(context).backupKeep.coerceAtLeast(1)).forEach {
            try {
                DocumentsContract.deleteDocument(resolver, it.first)
            } catch (e: Exception) {
                // eski faylni o'chirib bo'lmasa, e'tiborsiz qoldiramiz
            }
        }
    }

    fun notifyFailure(context: Context, reason: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Avtomatik zaxira", NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_wallet)
            .setContentTitle("Avtomatik zaxira bajarilmadi")
            .setContentText(reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$reason Sozlamalarda papkani qayta tanlang."))
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // ruxsat yo'q
        }
    }
}
