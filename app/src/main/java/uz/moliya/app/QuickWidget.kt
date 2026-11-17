package uz.moliya.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Bosh ekran vidjeti: bu oy xarajati va tez qo'shish tugmalari. */
class QuickWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val settings = Prefs.load(context)
        Dates.startDay = settings.monthStartDay
        val data = Storage.load(context)
        val hidden = data.hideBalance || settings.pinHash.isNotEmpty()
        val spent = data.expense(Dates.monthStart(0), Dates.monthStart(1))

        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick)
            views.setTextViewText(R.id.widget_month, Dates.monthLabel(0))
            views.setTextViewText(R.id.widget_amount, if (hidden) "••••••• so'm" else formatMoney(spent))
            views.setOnClickPendingIntent(R.id.widget_root, open(context, null, 10))
            views.setOnClickPendingIntent(R.id.widget_expense, open(context, "expense", 11))
            views.setOnClickPendingIntent(R.id.widget_income, open(context, "income", 12))
            manager.updateAppWidget(id, views)
        }
    }

    private fun open(context: Context, quick: String?, code: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (quick != null) intent.putExtra(MainActivity.EXTRA_QUICK, quick)
        return PendingIntent.getActivity(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        /** Ma'lumot o'zgarganda vidjetni yangilaydi. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(android.content.ComponentName(context, QuickWidget::class.java))
            if (ids.isNotEmpty()) {
                context.sendBroadcast(
                    Intent(context, QuickWidget::class.java)
                        .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
                )
            }
        }
    }
}
