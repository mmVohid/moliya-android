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
    private const val KEEP = 4
    private const val PREFIX = "moliya_auto_"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Papkaga yangi fayl yozadi va faqat oxirgi 4 ta avtomatik faylni qoldiradi. */
    fun runNow(context: Context, folder: String) {
        val resolver = context.contentResolver
        val tree = Uri.parse(folder)
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val name = PREFIX + Dates.iso(Dates.today()) + "_" + (System.currentTimeMillis() % 100000) + ".xlsx"
        val created = DocumentsContract.createDocument(
            resolver,
            parent,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            name,
        ) ?: throw IllegalStateException("Papkada fayl yaratib bo'lmadi. Papkani qayta tanlang.")
        val out = resolver.openOutputStream(created, "w")
            ?: throw IllegalStateException("Faylga yozib bo'lmadi.")
        out.use { XlsxExporter.write(Storage.load(context), it) }
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
                if (fileName.startsWith(PREFIX) && fileName.endsWith(".xlsx")) {
                    files.add(Triple(DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0)), fileName, c.getLong(2)))
                }
            }
        }
        files.sortedByDescending { it.third }.drop(KEEP).forEach {
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
