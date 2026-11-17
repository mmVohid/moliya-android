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

    /** Tez qo'shish so'rovi: 0 = yo'q, 1 = xarajat, 2 = daromad (widget, ikonka menyusi, Bosh sahifa tugmasi) */
    var quickAdd by mutableStateOf(0)
        private set

    /** PIN o'rnatilgan bo'lsa, ilova qulflangan holda ochiladi. */
    var locked by mutableStateOf(settings.hasPin)
        private set

    private fun syncGlobals() {
        Dates.startDay = settings.monthStartDay
        NumberFormat.compact = settings.compactNumbers
    }

    init {
        syncGlobals()
        val migrated = CategoryMigration.apply(data)
        if (migrated != data) {
            data = migrated
            Storage.save(getApplication<Application>(), migrated)
        }
        // Mavjud foydalanuvchi uchun zaxira eslatmasi hisobi bugundan boshlanadi
        if (!data.isEmpty && settings.firstEntryAt == 0L) {
            updateSettings(settings.copy(firstEntryAt = System.currentTimeMillis()))
        }
    }

    fun completeOnboarding() = updateSettings(settings.copy(onboarded = true))

    private fun update(block: (FinanceData) -> FinanceData) {
        val next = block(data)
        if (data.isEmpty && !next.isEmpty && settings.firstEntryAt == 0L) {
            updateSettings(settings.copy(firstEntryAt = System.currentTimeMillis()))
        }
        data = next
        Storage.save(getApplication<Application>(), next)
        QuickWidget.refresh(getApplication<Application>())
    }

    private fun updateSettings(next: AppSettings) {
        settings = next
        syncGlobals()
        Prefs.save(getApplication<Application>(), next)
    }

    // ───── Daromad / Xarajat ─────
    fun addTxn(t: Txn) = update { it.copy(txns = it.txns + t) }
    fun deleteTxn(id: Long) = update { d -> d.copy(txns = d.txns.filterNot { it.id == id }) }
    fun updateTxn(t: Txn) = update { d -> d.copy(txns = d.txns.map { if (it.id == t.id) t else it }) }
    fun restoreTxn(t: Txn) = update { d -> if (d.txns.any { it.id == t.id }) d else d.copy(txns = d.txns + t) }

    // ───── Refleksiya va byudjet ─────
    fun saveReflection(r: Reflection) = update { d ->
        d.copy(reflections = d.reflections.filterNot { it.monthKey == r.monthKey } + r)
    }

    fun setBudget(needs: Int, wants: Int, saveRate: Int) =
        update { it.copy(budgetNeeds = needs, budgetWants = wants, saveRate = saveRate) }

    // ───── Hamyonlar va kategoriyalar ─────
    fun addWallet(w: Wallet) = update { it.copy(wallets = it.wallets + w) }
    fun deleteWallet(id: Long) = update { d ->
        d.copy(
            wallets = d.wallets.filterNot { it.id == id },
            transfers = d.transfers.filterNot { it.fromId == id || it.toId == id },
            txns = d.txns.map { if (it.walletId == id) it.copy(walletId = 0L) else it },
        )
    }
    fun addTransfer(t: Transfer) = update { it.copy(transfers = it.transfers + t) }
    fun deleteTransfer(id: Long) = update { d -> d.copy(transfers = d.transfers.filterNot { it.id == id }) }

    private fun FinanceData.cats(income: Boolean) = if (income) incomeCategories else expenseCategories

    private fun FinanceData.withCats(income: Boolean, list: List<String>) =
        if (income) copy(incomeList = list) else copy(expenseList = list)

    fun addCategory(income: Boolean, name: String) = update { d ->
        val n = name.trim()
        val list = d.cats(income)
        if (n.isEmpty() || list.any { it.equals(n, true) }) d else d.withCats(income, list + n)
    }

    fun renameCategory(income: Boolean, old: String, new: String) = update { d ->
        val n = new.trim()
        val list = d.cats(income)
        if (n.isEmpty() || n == old || list.any { it.equals(n, true) }) {
            d
        } else {
            val renamed = d.withCats(income, list.map { if (it == old) n else it })
            renamed.copy(
                txns = d.txns.map { if (it.isIncome == income && it.category == old) it.copy(category = n) else it },
                templates = d.templates.map { if (it.isIncome == income && it.category == old) it.copy(category = n) else it },
                limits = if (income) d.limits else d.limits.mapKeys { (k, _) -> if (k == old) n else k },
            )
        }
    }

    fun deleteCategory(income: Boolean, name: String) = update { d ->
        val list = d.cats(income)
        if (list.size <= 1) d
        else d.withCats(income, list - name).copy(limits = if (income) d.limits else d.limits - name)
    }

    fun moveCategory(income: Boolean, name: String, delta: Int) = update { d ->
        val list = d.cats(income).toMutableList()
        val i = list.indexOf(name)
        val k = i + delta
        if (i < 0 || k !in list.indices) d else {
            list[i] = list[k].also { list[k] = list[i] }
            d.withCats(income, list)
        }
    }

    fun setEncryption(on: Boolean) {
        updateSettings(settings.copy(encryptData = on))
        Storage.save(getApplication<Application>(), data)
    }

    fun setLockOnScreenOff(on: Boolean) = updateSettings(settings.copy(lockOnScreenOff = on))

    /** null = zaxira shifrlashni o'chirish */
    fun setBackupPassword(password: String?) {
        if (password.isNullOrEmpty()) updateSettings(settings.copy(backupEncrypted = false, backupPwd = ""))
        else updateSettings(settings.copy(backupEncrypted = true, backupPwd = BackupCrypto.sealPassword(password)))
    }

    fun setUserName(name: String) = updateSettings(settings.copy(userName = name.trim().take(24)))
    fun setLockDelay(sec: Int) = updateSettings(settings.copy(lockDelaySec = sec))
    fun setBackupReminder(on: Boolean) = updateSettings(settings.copy(backupReminder = on))
    fun setTextScale(i: Int) = updateSettings(settings.copy(textScale = i))
    fun setCompactNumbers(on: Boolean) = updateSettings(settings.copy(compactNumbers = on))
    fun setHomeCards(mask: Int) = updateSettings(settings.copy(homeCards = mask))
    fun setMonthStartDay(d: Int) = updateSettings(settings.copy(monthStartDay = d.coerceIn(1, 28)))

    fun setDaily(on: Boolean, hour: Int, minute: Int) {
        updateSettings(settings.copy(dailyReminder = on, dailyHour = hour, dailyMinute = minute))
        if (on) DailyHabit.schedule(getApplication<Application>(), hour, minute) else DailyHabit.cancel(getApplication<Application>())
    }

    fun setBackupKeep(n: Int) = updateSettings(settings.copy(backupKeep = n))

    fun setBackupWeekday(day: Int) {
        updateSettings(settings.copy(backupWeekday = day))
        if (settings.autoBackup) AutoBackup.schedule(getApplication<Application>(), replace = true)
    }

    /** Barcha moliyaviy ma'lumotni o'chiradi (sozlamalar va PIN saqlanadi). */
    fun wipeAll() {
        data = FinanceData()
        Storage.save(getApplication<Application>(), data)
        Receipts.deleteAll(getApplication<Application>())
        QuickWidget.refresh(getApplication<Application>())
    }

    fun setDynamicColor(on: Boolean) = updateSettings(settings.copy(dynamicColor = on))

    fun setSecureScreen(on: Boolean) = updateSettings(settings.copy(secureScreen = on))

    // ───── Shablonlar ─────
    fun addTemplate(t: Template) = update { it.copy(templates = it.templates + t) }
    fun deleteTemplate(id: Long) = update { d -> d.copy(templates = d.templates.filterNot { it.id == id }) }

    // ───── Tez qo'shish ─────
    fun requestQuickAdd(income: Boolean) {
        quickAdd = if (income) 2 else 1
    }

    fun consumeQuickAdd() {
        quickAdd = 0
    }

    // ───── Limitlar ─────
    fun setLimits(limits: Map<String, Long>) = update { it.copy(limits = limits.filterValues { v -> v > 0L }) }

    // ───── Himoya yostig'i ─────
    fun setPlan(monthlyNeed: Long, months: Int) =
        update { it.copy(monthlyNeed = monthlyNeed, targetMonths = months) }

    fun addCushion(e: CushionEntry) = update { it.copy(cushion = it.cushion + e) }
    fun deleteCushion(id: Long) = update { d -> d.copy(cushion = d.cushion.filterNot { it.id == id }) }
    fun restoreCushion(e: CushionEntry) =
        update { d -> if (d.cushion.any { it.id == e.id }) d else d.copy(cushion = d.cushion + e) }

    // ───── Maqsadlar ─────
    fun addGoal(g: Goal) = update { it.copy(goals = it.goals + g) }
    fun deleteGoal(id: Long) = update { d -> d.copy(goals = d.goals.filterNot { it.id == id }) }
    fun restoreGoal(g: Goal) = update { d -> if (d.goals.any { it.id == g.id }) d else d.copy(goals = d.goals + g) }

    fun addGoalEntry(goalId: Long, e: CushionEntry) = update { d ->
        d.copy(goals = d.goals.map { if (it.id == goalId) it.copy(entries = it.entries + e) else it })
    }

    fun deleteGoalEntry(goalId: Long, entryId: Long) = update { d ->
        d.copy(
            goals = d.goals.map { g ->
                if (g.id == goalId) g.copy(entries = g.entries.filterNot { it.id == entryId }) else g
            },
        )
    }

    fun restoreGoalEntry(goalId: Long, e: CushionEntry) = update { d ->
        d.copy(
            goals = d.goals.map { g ->
                if (g.id == goalId && g.entries.none { it.id == e.id }) g.copy(entries = g.entries + e) else g
            },
        )
    }

    // ───── Qarzlar ─────
    fun addDebt(debt: Debt) = update { it.copy(debts = it.debts + debt) }
    fun deleteDebt(id: Long) = update { d -> d.copy(debts = d.debts.filterNot { it.id == id }) }
    fun restoreDebt(debt: Debt) = update { d -> if (d.debts.any { it.id == debt.id }) d else d.copy(debts = d.debts + debt) }

    fun restorePayment(debtId: Long, p: DebtPayment) = update { d ->
        d.copy(
            debts = d.debts.map { debt ->
                if (debt.id == debtId && debt.payments.none { it.id == p.id }) debt.copy(payments = debt.payments + p) else debt
            },
        )
    }

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
    fun replaceData(raw: FinanceData) {
        val imported = CategoryMigration.apply(raw)
        update { it.copy(
            txns = imported.txns,
            cushion = imported.cushion,
            monthlyNeed = imported.monthlyNeed,
            targetMonths = imported.targetMonths,
            debts = imported.debts,
            limits = imported.limits,
            recurring = imported.recurring,
            templates = imported.templates,
            goals = imported.goals,
            reflections = imported.reflections,
            budgetNeeds = imported.budgetNeeds,
            budgetWants = imported.budgetWants,
            saveRate = imported.saveRate,
            wallets = imported.wallets,
            transfers = imported.transfers,
            customExpense = imported.customExpense,
            customIncome = imported.customIncome,
            expenseList = imported.expenseList,
            incomeList = imported.incomeList,
        ) }
    }

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

    /** Fonda ishlagan avto-zaxira (worker) yangilagan sozlamalarni qayta o'qiydi. */
    fun refreshSettings() {
        settings = Prefs.load(getApplication<Application>())
        syncGlobals()
    }

    /** Oxirgi zaxira vaqti: qo'lda yoki avtomatik, qaysi biri yangi bo'lsa */
    val lastBackupAt: Long get() = maxOf(data.lastBackup, settings.lastAutoBackup)

    fun setBackupFolder(uri: String) = updateSettings(settings.copy(backupFolder = uri))

    fun setAutoBackup(on: Boolean) {
        updateSettings(settings.copy(autoBackup = on))
        if (on) {
            AutoBackup.schedule(getApplication<Application>())
            if (settings.backupFolder.isNotEmpty()) AutoBackup.runOnce(getApplication<Application>())
        } else {
            AutoBackup.cancel(getApplication<Application>())
        }
    }

    fun setReminders(on: Boolean) {
        updateSettings(settings.copy(reminders = on))
        if (on) Reminders.schedule(getApplication<Application>()) else Reminders.cancel(getApplication<Application>())
    }
}
