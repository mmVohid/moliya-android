package uz.moliya.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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

/** Har kuni tanlangan vaqtda "bugun yozdingizmi?" deb eslatadi (bugun yozuv bo'lsa jim turadi). */
class DailyHabitWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val ctx = applicationContext
        val settings = Prefs.load(ctx)
        if (!settings.dailyReminder) return Result.success()
        val start = Dates.noon(System.currentTimeMillis()) - 12L * 60 * 60 * 1000
        val wroteToday = Storage.load(ctx).txns.any { it.date in start until start + 24L * 60 * 60 * 1000 }
        if (!wroteToday) DailyHabit.notify(ctx, settings.userName)
        return Result.success()
    }
}

object DailyHabit {
    private const val CHANNEL_ID = "kunlik_odat"
    private const val WORK_NAME = "kunlik_odat_eslatma"
    private const val NOTIFICATION_ID = 1003

    fun schedule(context: Context, hour: Int, minute: Int) {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        val request = PeriodicWorkRequestBuilder<DailyHabitWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(next.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun notify(context: Context, name: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Kunlik eslatma", NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_QUICK, "expense")
        val pending = PendingIntent.getActivity(context, 20, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val title = if (name.isBlank()) "Bugungi xarajatlarni yozdingizmi?" else "$name, bugungi xarajatlarni yozdingizmi?"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_wallet)
            .setContentTitle(title)
            .setContentText("Bir daqiqa vaqt ajrating: yozib borish odatni mustahkamlaydi.")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // ruxsat yo'q
        }
    }
}
