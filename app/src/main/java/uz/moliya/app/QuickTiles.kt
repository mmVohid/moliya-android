package uz.moliya.app

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Tezkor sozlamalar paneli plitkasi: bosilsa ilova tegishli oynani darhol ochadi. */
abstract class BaseQuickTile(private val kind: String) : TileService() {

    override fun onStartListening() {
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_QUICK, kind)
        if (Build.VERSION.SDK_INT >= 34) {
            val pending = PendingIntent.getActivity(
                this,
                kind.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

class IncomeTile : BaseQuickTile("income")
class ExpenseTile : BaseQuickTile("expense")
class DebtTile : BaseQuickTile("debt")
class SavingTile : BaseQuickTile("saving")
class ReportTile : BaseQuickTile("report")
class BackupTile : BaseQuickTile("backup")
