package uz.moliya.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

class FinanceViewModel(app: Application) : AndroidViewModel(app) {

    var data by mutableStateOf(Storage.load(app))
        private set

    var settings by mutableStateOf(Prefs.load(app))
        private set

    /** PIN o'rnatilgan bo'lsa, ilova qulflangan holda ochiladi. */
    var locked by mutableStateOf(settings.hasPin)
        private set

    init {
        applyRecurring()
    }

    private fun update(block: (FinanceData) -> FinanceData) {
        val next = block(data)
        data = next
        Storage.save(getApplication<Application>(), next)
    }

    private fun updateSettings(next: AppSettings) {
        settings = next
        Prefs.save(getApplication<Application>(), next)
    }

    // ───── Daromad / Xarajat ─────
    fun addTxn(t: Txn) = update { it.copy(txns = it.txns + t) }
    fun deleteTxn(id: Long) = update { d -> d.copy(txns = d.txns.filterNot { it.id == id }) }

    // ───── Takroriy yozuvlar ─────
    fun addRecurring(r: Recurring) {
        update { it.copy(recurring = it.recurring + r) }
        applyRecurring()
    }

    fun deleteRecurring(id: Long) = update { d -> d.copy(recurring = d.recurring.filterNot { it.id == id }) }

    /** Muddati kelgan takroriy yozuvlarni qo'shadi (ilova ochilganda va yangi yozuv qo'shilganda). */
    fun applyRecurring() {
        val next = Recurrence.apply(data, Dates.today())
        if (next !== data) {
            data = next
            Storage.save(getApplication<Application>(), next)
        }
    }

    // ───── Limitlar ─────
    fun setLimits(limits: Map<String, Long>) = update { it.copy(limits = limits.filterValues { v -> v > 0L }) }

    // ───── Himoya yostig'i ─────
    fun setPlan(monthlyNeed: Long, months: Int) =
        update { it.copy(monthlyNeed = monthlyNeed, targetMonths = months) }

    fun addCushion(e: CushionEntry) = update { it.copy(cushion = it.cushion + e) }
    fun deleteCushion(id: Long) = update { d -> d.copy(cushion = d.cushion.filterNot { it.id == id }) }

    // ───── Qarzlar ─────
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

    // ───── Balansni yashirish ─────
    fun setHideBalance(hide: Boolean) = update { it.copy(hideBalance = hide) }

    // ───── Backup / tiklash ─────
    fun markBackup() = update { it.copy(lastBackup = System.currentTimeMillis()) }

    /** Excel'dan tiklangan ma'lumot bilan to'liq almashtiradi. */
    fun replaceData(imported: FinanceData) =
        update { it.copy(
            txns = imported.txns,
            cushion = imported.cushion,
            monthlyNeed = imported.monthlyNeed,
            targetMonths = imported.targetMonths,
            debts = imported.debts,
            limits = imported.limits,
            recurring = imported.recurring,
        ) }

    // ───── Sozlamalar, qulf ─────
    fun verifyPin(pin: String): Boolean = Pin.verify(pin, settings)

    fun unlock() {
        locked = false
    }

    fun lock() {
        if (settings.hasPin) locked = true
    }

    fun setPin(pin: String) {
        val salt = Pin.newSalt()
        updateSettings(settings.copy(pinSalt = salt, pinHash = Pin.hash(pin, salt)))
    }

    fun clearPin() {
        updateSettings(settings.copy(pinHash = "", pinSalt = "", biometric = false))
        locked = false
    }

    fun setBiometric(on: Boolean) = updateSettings(settings.copy(biometric = on && settings.hasPin))

    fun setTheme(mode: Int) = updateSettings(settings.copy(theme = mode))

    fun setReminders(on: Boolean) {
        updateSettings(settings.copy(reminders = on))
        if (on) Reminders.schedule(getApplication<Application>()) else Reminders.cancel(getApplication<Application>())
    }
}
