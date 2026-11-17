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
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // Tunda bezovta qilmaymiz
        if (!wroteToday && hour in 8..21) DailyHabit.notify(ctx)
        return Result.success()
    }
}

object DailyHabit {
    private const val CHANNEL_ID = "kunlik_odat"
    private const val WORK_NAME = "kunlik_odat_eslatma"
    private const val NOTIFICATION_ID = 1003

    private val MESSAGES = listOf(
        "Hamyoningiz sizni sog'indi" to "Bugungi xarajatlarni yozish bir daqiqa oladi. Boshladikmi?",
        "Pul qayerga ketdi?" to "Hali yozmadingiz. Birga topamiz, bir daqiqa kifoya.",
        "Odat zanjirini uzmang" to "Kichik qadam: bugungi birinchi yozuvni kiriting.",
        "Choy puli ham hisob" to "Mayda xarajatlar ko'p narsani hal qiladi. Yozib qo'ying.",
        "Moliya sizni kutyapti" to "Bugun hali bitta ham yozuv yo'q. Qani, boshlaymiz!",
        "Kimdir budjetni nazorat qilishi kerak" to "Bu sizsiz. Bugungi xarajatlarni kiriting.",
        "Jamg'arma kichik qadamdan boshlanadi" to "Bugun nima sarfladingiz? Bir daqiqada yozing.",
        "Eslatma, ammo mehr bilan" to "Xarajatlarni yozish kelajakdagi o'zingizga sovg'a.",
    )

    /** Ilova ochilganda chaqiriladi: hisob shu paytdan boshlab har 2 soatda eslatadi. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<DailyHabitWorker>(2, TimeUnit.HOURS)
            .setInitialDelay(2, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun notify(context: Context) {
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
        val (title, text) = MESSAGES[((System.currentTimeMillis() / (2L * 60 * 60 * 1000)) % MESSAGES.size).toInt()]
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_wallet)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
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
