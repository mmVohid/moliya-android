package uz.moliya.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

class FinanceViewModel(app: Application) : AndroidViewModel(app) {

    var data by mutableStateOf(Storage.load(app))
        private set

    private fun update(block: (FinanceData) -> FinanceData) {
        val next = block(data)
        data = next
        Storage.save(getApplication<Application>(), next)
    }

    // Daromad / Xarajat
    fun addTxn(t: Txn) = update { it.copy(txns = it.txns + t) }
    fun deleteTxn(id: Long) = update { d -> d.copy(txns = d.txns.filterNot { it.id == id }) }

    // Himoya yostig'i
    fun setPlan(monthlyNeed: Long, months: Int) =
        update { it.copy(monthlyNeed = monthlyNeed, targetMonths = months) }

    fun addCushion(e: CushionEntry) = update { it.copy(cushion = it.cushion + e) }
    fun deleteCushion(id: Long) = update { d -> d.copy(cushion = d.cushion.filterNot { it.id == id }) }

    // Qarzlar
    fun addDebt(debt: Debt) = update { it.copy(debts = it.debts + debt) }
    fun deleteDebt(id: Long) = update { d -> d.copy(debts = d.debts.filterNot { it.id == id }) }

    fun addPayment(debtId: Long, p: DebtPayment) = update { d ->
        d.copy(debts = d.debts.map { if (it.id == debtId) it.copy(payments = it.payments + p) else it })
    }

    fun deletePayment(debtId: Long, paymentId: Long) = update { d ->
        d.copy(
            debts = d.debts.map { debt ->
                if (debt.id == debtId) debt.copy(payments = debt.payments.filterNot { it.id == paymentId }) else debt
            },
        )
    }

    // Balansni yashirish
    fun setHideBalance(hide: Boolean) = update { it.copy(hideBalance = hide) }

    // Backup
    fun markBackup() = update { it.copy(lastBackup = System.currentTimeMillis()) }
}
