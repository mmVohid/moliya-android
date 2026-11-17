package uz.moliya.app

import android.Manifest
import android.annotation.SuppressLint
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
import kotlin.math.roundToInt

/** Har kuni soat 9:00 da qarz muddatlarini tekshiradi va kerak bo'lsa bildirishnoma yuboradi. */
class DebtReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val data = Storage.load(applicationContext)
        val messages = Reminders.dueMessages(data, Dates.today())
        if (messages.isNotEmpty()) Reminders.notify(applicationContext, messages)
        return Result.success()
    }
}

object Reminders {
    private const val CHANNEL_ID = "qarz_eslatmalari"
    private const val WORK_NAME = "qarz_eslatmalari_kunlik"
    private const val NOTIFICATION_ID = 1001
    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun schedule(context: Context) {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        val delay = next.timeInMillis - now.timeInMillis
        val request = PeriodicWorkRequestBuilder<DebtReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Qaysi kunlarda eslatish: muddatga 3 kun, 1 kun, bugun va 3 kun o'tganda. */
    fun dueMessages(data: FinanceData, today: Long): List<String> {
        val out = ArrayList<String>()
        for (debt in data.debts) {
            if (debt.remaining <= 0L || debt.dueDate <= 0L) continue
            val days = ((Dates.noon(debt.dueDate) - Dates.noon(today)).toDouble() / DAY_MS).roundToInt()
            val whenText = when (days) {
                3 -> "muddatiga 3 kun qoldi"
                1 -> "muddati ertaga"
                0 -> "muddati bugun"
                -3 -> "muddati 3 kun oldin o'tgan"
                else -> continue
            }
            val amount = formatMoney(debt.remaining)
            out.add(
                if (debt.isLent) "${debt.name} sizga $amount qaytarishi kerak: $whenText"
                else "${debt.name} qarzi ($amount): $whenText",
            )
        }
        return out
    }

    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun notify(context: Context, messages: List<String>) {
        if (!canNotify(context)) return

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Qarz eslatmalari",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Qarz qaytarish muddati yaqinlashganda ogohlantiradi" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val style = NotificationCompat.InboxStyle()
        messages.forEach { style.addLine(it) }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_wallet)
            .setContentTitle(if (messages.size == 1) "Qarz muddati" else "${messages.size} ta qarz muddati")
            .setContentText(messages.first())
            .setStyle(style)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
