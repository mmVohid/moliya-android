package uz.moliya.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicLong

// ───────────────────────── Modellar ─────────────────────────

data class Txn(
    val id: Long,
    val isIncome: Boolean,
    val amount: Long,
    val category: String,
    val note: String,
    val date: Long,
    /** "UZS" yoki chet el valyutasi; amount har doim so'mdagi qiymat */
    val currency: String = "UZS",
    val foreignAmount: Double = 0.0,
    /** 1 birlik valyuta necha so'm (yozuv paytidagi kurs) */
    val rate: Double = 0.0,
    /** 0 = hamyon belgilanmagan */
    val walletId: Long = 0L,
    /** Chek rasmi fayl nomi (ilova xotirasida); Excelga kirmaydi */
    val receipt: String = "",
)

/** amount > 0: yostiqqa qo'shildi, amount < 0: yostiqdan yechildi */
data class CushionEntry(
    val id: Long,
    val amount: Long,
    val note: String,
    val date: Long,
)

data class DebtPayment(
    val id: Long,
    val amount: Long,
    val date: Long,
)

data class Debt(
    val id: Long,
    val name: String,
    val total: Long,
    val interest: Double,
    /** 0 = muddat belgilanmagan */
    val dueDate: Long,
    val payments: List<DebtPayment>,
    /** false = men olgan qarz (men qaytaraman), true = men bergan qarz (menga qaytarishadi) */
    val isLent: Boolean = false,
) {
    val paid: Long get() = payments.sumOf { it.amount }
    val remaining: Long get() = (total - paid).coerceAtLeast(0L)
    val progress: Float
        get() = if (total <= 0L) 0f else (paid.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

data class Wallet(val id: Long, val name: String, val opening: Long)

data class Transfer(val id: Long, val fromId: Long, val toId: Long, val amount: Long, val date: Long)

/** Oylik refleksiya: 3 ta savolga javob. monthKey = "2026-11" */
data class Reflection(
    val id: Long,
    val monthKey: String,
    val wasteful: String,
    val improved: String,
    val nextGoal: String,
)

/** Tez-tez takrorlanadigan xarajat/daromad shabloni */
data class Template(
    val id: Long,
    val isIncome: Boolean,
    val amount: Long,
    val category: String,
    val note: String,
)

/** Jamg'arma maqsadi (telefon, safar va h.k.). entries: musbat = qo'shildi, manfiy = yechildi */
data class Goal(
    val id: Long,
    val name: String,
    val target: Long,
    /** 0 = muddat yo'q */
    val deadline: Long,
    val entries: List<CushionEntry>,
) {
    val saved: Long get() = entries.sumOf { it.amount }.coerceAtLeast(0L)
    val remaining: Long get() = (target - saved).coerceAtLeast(0L)
    val progress: Float
        get() = if (target <= 0L) 0f else (saved.toFloat() / target.toFloat()).coerceIn(0f, 1f)
}

object Currencies {
    val foreign = listOf("USD", "EUR", "RUB")
    val all = listOf("UZS") + foreign
}

/** Har oy takrorlanadigan yozuv (oylik maosh, ijara, kredit to'lovi va h.k.) */
data class Recurring(
    val id: Long,
    val isIncome: Boolean,
    val amount: Long,
    val category: String,
    val note: String,
    /** Oyning qaysi kuni (1..31); qisqa oylarda oxirgi kunga tushadi */
    val anchorDay: Int,
    /** Keyingi yozuv qo'shiladigan sana (kun o'rtasi, millis) */
    val nextDue: Long,
)

data class FinanceData(
    val txns: List<Txn> = emptyList(),
    val cushion: List<CushionEntry> = emptyList(),
    val monthlyNeed: Long = 0L,
    val targetMonths: Int = 6,
    val debts: List<Debt> = emptyList(),
    val lastBackup: Long = 0L,
    val hideBalance: Boolean = false,
    /** Kategoriya -> oylik xarajat limiti (so'm) */
    val limits: Map<String, Long> = emptyMap(),
    val recurring: List<Recurring> = emptyList(),
    val templates: List<Template> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val reflections: List<Reflection> = emptyList(),
    /** Byudjet: zarur %, xohish %, qolgani jamg'arma. */
    val budgetNeeds: Int = 50,
    val budgetWants: Int = 30,
    /** Daromad kiritilganda jamg'armaga taklif qilinadigan foiz (0 = o'chiq) */
    val saveRate: Int = 0,
    val wallets: List<Wallet> = emptyList(),
    val transfers: List<Transfer> = emptyList(),
    val customExpense: List<String> = emptyList(),
    val customIncome: List<String> = emptyList(),
) {
    val expenseCategories: List<String> get() = Categories.expense + customExpense
    val incomeCategories: List<String> get() = Categories.income + customIncome

    fun walletBalance(w: Wallet): Long {
        val flow = txns.filter { it.walletId == w.id }.sumOf { if (it.isIncome) it.amount else -it.amount }
        val tin = transfers.filter { it.toId == w.id }.sumOf { it.amount }
        val tout = transfers.filter { it.fromId == w.id }.sumOf { it.amount }
        return w.opening + flow + tin - tout
    }

    val cushionSaved: Long get() = cushion.sumOf { it.amount }.coerceAtLeast(0L)
    val cushionTarget: Long get() = monthlyNeed * targetMonths

    /** Men qarzdorman (olingan qarzlar qoldig'i) */
    val debtOwed: Long get() = debts.filter { !it.isLent }.sumOf { it.remaining }

    /** Menga qarzdor (berilgan qarzlar qoldig'i) */
    val debtLent: Long get() = debts.filter { it.isLent }.sumOf { it.remaining }
    val isEmpty: Boolean get() = txns.isEmpty() && cushion.isEmpty() && debts.isEmpty() && goals.isEmpty()

    fun income(start: Long, end: Long): Long =
        txns.filter { it.isIncome && it.date >= start && it.date < end }.sumOf { it.amount }

    fun expense(start: Long, end: Long): Long =
        txns.filter { !it.isIncome && it.date >= start && it.date < end }.sumOf { it.amount }

    /** Oy uchun jamg'arma darajasi: (daromad - xarajat) / daromad; daromad yo'q bo'lsa null */
    fun savingsRate(start: Long, end: Long): Float? {
        val inc = income(start, end)
        if (inc <= 0L) return null
        return (inc - expense(start, end)).toFloat() / inc.toFloat()
    }

    fun categorySpent(category: String, start: Long, end: Long): Long =
        txns.filter { !it.isIncome && it.category == category && it.date >= start && it.date < end }
            .sumOf { it.amount }
}

object Recurrence {
    /** Muddati kelgan takroriy yozuvlarni yozuvlar ro'yxatiga qo'shadi. O'zgarish bo'lmasa, o'sha obyektni qaytaradi. */
    fun apply(d: FinanceData, today: Long): FinanceData {
        if (d.recurring.isEmpty()) return d
        val created = ArrayList<Txn>()
        var changed = false
        val updated = d.recurring.map { r ->
            var due = r.nextDue
            var guard = 0
            while (due <= today && guard < 60) {
                created.add(
                    Txn(
                        id = Ids.next(),
                        isIncome = r.isIncome,
                        amount = r.amount,
                        category = r.category,
                        note = if (r.note.isBlank()) "Takroriy" else r.note,
                        date = due,
                    ),
                )
                due = Dates.nextMonthDue(due, r.anchorDay)
                guard++
            }
            if (due != r.nextDue) {
                changed = true
                r.copy(nextDue = due)
            } else {
                r
            }
        }
        return if (changed) d.copy(txns = d.txns + created, recurring = updated) else d
    }
}

object Budget {
    /** Zarur toifasi; qolgan xarajatlar "xohish" hisoblanadi */
    val needs = setOf(
        "Do'konlar", "Transport", "Mobile aloqa", "Kommunal xizmatlar", "Ta'lim",
        // eski yozuvlar uchun
        "Oziq-ovqat", "Uy-joy", "Aloqa", "Sog'liq", "Qarz to'lovi",
    )
}

object Categories {
    val expense = listOf(
        "Ta'lim", "Do'konlar", "Transport", "Mobile aloqa",
        "Restoran va kofelar", "Kommunal xizmatlar", "Sayohat va mehmonxona", "Boshqalar",
    )
    val income = listOf("Avans", "Oylik", "Hadiya", "Qo'shimcha daromad", "Boshqa")
}

object Ids {
    private val counter = AtomicLong(System.currentTimeMillis() * 1000L)
    fun next(): Long = counter.incrementAndGet()
}

// ───────────────────────── Formatlash ─────────────────────────

fun formatMoney(value: Long): String {
    val negative = value < 0
    val digits = Math.abs(value).toString()
    val sb = StringBuilder()
    for (i in digits.indices) {
        if (i > 0 && (digits.length - i) % 3 == 0) sb.append(' ')
        sb.append(digits[i])
    }
    return (if (negative) "−" else "") + sb.toString() + " so'm"
}

fun formatForeign(value: Double, currency: String): String {
    val s = if (value % 1.0 == 0.0) String.format(Locale.US, "%,.0f", value) else String.format(Locale.US, "%,.2f", value)
    return "$s $currency"
}

fun parseMoney(text: String): Long = text.filter { it in '0'..'9' }.toLongOrNull() ?: 0L

fun parseDecimal(text: String): Double = text.replace(',', '.').toDoubleOrNull() ?: 0.0

fun formatPercent(value: Double): String {
    val s = String.format(Locale.US, "%.1f", value)
    return (if (s.endsWith(".0")) s.dropLast(2) else s) + "%"
}

object Dates {
    val monthNames = listOf(
        "Yanvar", "Fevral", "Mart", "Aprel", "May", "Iyun",
        "Iyul", "Avgust", "Sentabr", "Oktyabr", "Noyabr", "Dekabr",
    )

    private fun cal(millis: Long): Calendar =
        Calendar.getInstance().apply { timeInMillis = millis }

    /** Sanani kunning o'rtasiga (12:00) keltiradi, shunda vaqt zonasi siljishlari kunni o'zgartirmaydi. */
    fun noon(millis: Long): Long = cal(millis).apply {
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun today(): Long = noon(System.currentTimeMillis())

    /** offset = 0 joriy oy, -1 o'tgan oy, 1 keyingi oy */
    fun monthStart(offset: Int): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.add(Calendar.MONTH, offset)
        return c.timeInMillis
    }

    private fun monthCal(offset: Int): Calendar {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.add(Calendar.MONTH, offset)
        return c
    }

    fun monthLabel(offset: Int): String {
        val c = monthCal(offset)
        return monthNames[c.get(Calendar.MONTH)] + " " + c.get(Calendar.YEAR)
    }

    fun shortMonth(offset: Int): String =
        monthNames[monthCal(offset).get(Calendar.MONTH)].take(3)

    fun format(millis: Long): String {
        val c = cal(millis)
        return String.format(
            Locale.US, "%02d.%02d.%d",
            c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR),
        )
    }

    fun iso(millis: Long): String {
        val c = cal(millis)
        return String.format(
            Locale.US, "%04d-%02d-%02d",
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
        )
    }

    fun formatDateTime(millis: Long): String {
        val c = cal(millis)
        return format(millis) + String.format(
            Locale.US, " %02d:%02d",
            c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
        )
    }

    /** Bugundan muddatgacha taxminan nechta oy qolgan (kamida 1) */
    fun monthsLeft(deadline: Long): Int {
        val days = (deadline - today()).toDouble() / 86_400_000.0
        return Math.max(1, Math.ceil(days / 30.0).toInt())
    }

    fun dayOfMonth(millis: Long): Int = cal(millis).get(Calendar.DAY_OF_MONTH)

    /** Keyingi oydagi shu kun (qisqa oyda oxirgi kun), kun o'rtasida. */
    fun nextMonthDue(current: Long, anchorDay: Int): Long {
        val c = cal(current)
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.add(Calendar.MONTH, 1)
        val last = c.getActualMaximum(Calendar.DAY_OF_MONTH)
        c.set(Calendar.DAY_OF_MONTH, minOf(anchorDay, last))
        c.set(Calendar.HOUR_OF_DAY, 12)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    /** "2026-11" ko'rinishidagi oy kaliti */
    fun monthKey(millis: Long): String {
        val c = cal(millis)
        return String.format(Locale.US, "%04d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
    }

    /** Material DatePicker UTC yarim tunni kutadi. */
    fun toPickerMillis(local: Long): Long {
        val c = cal(local)
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.clear()
        utc.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
        return utc.timeInMillis
    }

    fun fromPickerMillis(utcMillis: Long): Long {
        val u = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        u.timeInMillis = utcMillis
        val c = Calendar.getInstance()
        c.clear()
        c.set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
        return c.timeInMillis
    }
}

// ───────────────────────── Saqlash (ichki xotira, JSON) ─────────────────────────

object Storage {
    private const val FILE_NAME = "moliya.json"

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    fun load(context: Context): FinanceData {
        val f = file(context)
        if (!f.exists()) return FinanceData()
        return try {
            parse(JSONObject(f.readText(Charsets.UTF_8)))
        } catch (e: Exception) {
            // Buzilgan faylni yo'qotmaymiz: nusxasini saqlab qo'yamiz.
            try {
                f.copyTo(File(context.filesDir, "moliya.buzilgan.json"), overwrite = true)
            } catch (copyError: Exception) {
                // nusxa olinmasa ham ilova ishlashda davom etadi
            }
            FinanceData()
        }
    }

    fun save(context: Context, data: FinanceData) {
        val f = file(context)
        val tmp = File(context.filesDir, "$FILE_NAME.tmp")
        tmp.writeText(toJson(data).toString(), Charsets.UTF_8)
        if (!tmp.renameTo(f)) {
            f.writeText(tmp.readText(Charsets.UTF_8), Charsets.UTF_8)
            tmp.delete()
        }
    }

    private fun toJson(d: FinanceData): JSONObject {
        val root = JSONObject()
        root.put("version", 1)
        root.put("monthlyNeed", d.monthlyNeed)
        root.put("targetMonths", d.targetMonths)
        root.put("lastBackup", d.lastBackup)
        root.put("hideBalance", d.hideBalance)

        val limits = JSONObject()
        for ((category, amount) in d.limits) limits.put(category, amount)
        root.put("limits", limits)

        val recurring = JSONArray()
        for (r in d.recurring) {
            val o = JSONObject()
            o.put("id", r.id)
            o.put("income", r.isIncome)
            o.put("amount", r.amount)
            o.put("category", r.category)
            o.put("note", r.note)
            o.put("anchorDay", r.anchorDay)
            o.put("nextDue", r.nextDue)
            recurring.put(o)
        }
        root.put("recurring", recurring)

        val templates = JSONArray()
        for (t in d.templates) {
            val o = JSONObject()
            o.put("id", t.id)
            o.put("income", t.isIncome)
            o.put("amount", t.amount)
            o.put("category", t.category)
            o.put("note", t.note)
            templates.put(o)
        }
        root.put("templates", templates)

        val goals = JSONArray()
        for (g in d.goals) {
            val o = JSONObject()
            o.put("id", g.id)
            o.put("name", g.name)
            o.put("target", g.target)
            o.put("deadline", g.deadline)
            val entries = JSONArray()
            for (e in g.entries) {
                val eo = JSONObject()
                eo.put("id", e.id)
                eo.put("amount", e.amount)
                eo.put("note", e.note)
                eo.put("date", e.date)
                entries.put(eo)
            }
            o.put("entries", entries)
            goals.put(o)
        }
        root.put("goals", goals)

        val refl = JSONArray()
        for (r in d.reflections) {
            val o = JSONObject()
            o.put("id", r.id)
            o.put("month", r.monthKey)
            o.put("a", r.wasteful)
            o.put("b", r.improved)
            o.put("c", r.nextGoal)
            refl.put(o)
        }
        root.put("reflections", refl)
        root.put("bNeeds", d.budgetNeeds)
        root.put("bWants", d.budgetWants)
        root.put("saveRate", d.saveRate)

        val wallets = JSONArray()
        for (w in d.wallets) {
            val o = JSONObject()
            o.put("id", w.id); o.put("name", w.name); o.put("opening", w.opening)
            wallets.put(o)
        }
        root.put("wallets", wallets)
        val transfers = JSONArray()
        for (t in d.transfers) {
            val o = JSONObject()
            o.put("id", t.id); o.put("from", t.fromId); o.put("to", t.toId)
            o.put("amount", t.amount); o.put("date", t.date)
            transfers.put(o)
        }
        root.put("transfers", transfers)
        root.put("cEx", JSONArray(d.customExpense))
        root.put("cIn", JSONArray(d.customIncome))

        val txns = JSONArray()
        for (t in d.txns) {
            val o = JSONObject()
            o.put("id", t.id)
            o.put("income", t.isIncome)
            o.put("amount", t.amount)
            o.put("category", t.category)
            o.put("note", t.note)
            o.put("date", t.date)
            if (t.currency != "UZS") {
                o.put("cur", t.currency)
                o.put("fa", t.foreignAmount)
                o.put("rate", t.rate)
            }
            if (t.walletId != 0L) o.put("w", t.walletId)
            if (t.receipt.isNotEmpty()) o.put("rc", t.receipt)
            txns.put(o)
        }
        root.put("txns", txns)

        val cushion = JSONArray()
        for (c in d.cushion) {
            val o = JSONObject()
            o.put("id", c.id)
            o.put("amount", c.amount)
            o.put("note", c.note)
            o.put("date", c.date)
            cushion.put(o)
        }
        root.put("cushion", cushion)

        val debts = JSONArray()
        for (debt in d.debts) {
            val o = JSONObject()
            o.put("id", debt.id)
            o.put("name", debt.name)
            o.put("total", debt.total)
            o.put("interest", debt.interest)
            o.put("dueDate", debt.dueDate)
            o.put("lent", debt.isLent)
            val pays = JSONArray()
            for (p in debt.payments) {
                val po = JSONObject()
                po.put("id", p.id)
                po.put("amount", p.amount)
                po.put("date", p.date)
                pays.put(po)
            }
            o.put("payments", pays)
            debts.put(o)
        }
        root.put("debts", debts)
        return root
    }

    private fun <T> JSONArray?.mapObjects(block: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        val out = ArrayList<T>(length())
        for (i in 0 until length()) out.add(block(getJSONObject(i)))
        return out
    }

    private fun parse(o: JSONObject): FinanceData = FinanceData(
        txns = o.optJSONArray("txns").mapObjects {
            Txn(
                id = it.getLong("id"),
                isIncome = it.getBoolean("income"),
                amount = it.getLong("amount"),
                category = it.optString("category", "Boshqa"),
                note = it.optString("note", ""),
                date = it.getLong("date"),
                currency = it.optString("cur", "UZS"),
                foreignAmount = it.optDouble("fa", 0.0),
                rate = it.optDouble("rate", 0.0),
                walletId = it.optLong("w", 0L),
                receipt = it.optString("rc", ""),
            )
        },
        cushion = o.optJSONArray("cushion").mapObjects {
            CushionEntry(
                id = it.getLong("id"),
                amount = it.getLong("amount"),
                note = it.optString("note", ""),
                date = it.getLong("date"),
            )
        },
        monthlyNeed = o.optLong("monthlyNeed", 0L),
        targetMonths = o.optInt("targetMonths", 6),
        debts = o.optJSONArray("debts").mapObjects { d ->
            Debt(
                id = d.getLong("id"),
                name = d.optString("name", ""),
                total = d.getLong("total"),
                interest = d.optDouble("interest", 0.0),
                dueDate = d.optLong("dueDate", 0L),
                isLent = d.optBoolean("lent", false),
                payments = d.optJSONArray("payments").mapObjects { p ->
                    DebtPayment(
                        id = p.getLong("id"),
                        amount = p.getLong("amount"),
                        date = p.getLong("date"),
                    )
                },
            )
        },
        lastBackup = o.optLong("lastBackup", 0L),
        hideBalance = o.optBoolean("hideBalance", false),
        templates = o.optJSONArray("templates").mapObjects {
            Template(
                id = it.getLong("id"),
                isIncome = it.getBoolean("income"),
                amount = it.getLong("amount"),
                category = it.optString("category", "Boshqa"),
                note = it.optString("note", ""),
            )
        },
        goals = o.optJSONArray("goals").mapObjects { g ->
            Goal(
                id = g.getLong("id"),
                name = g.optString("name", ""),
                target = g.getLong("target"),
                deadline = g.optLong("deadline", 0L),
                entries = g.optJSONArray("entries").mapObjects { e ->
                    CushionEntry(
                        id = e.getLong("id"),
                        amount = e.getLong("amount"),
                        note = e.optString("note", ""),
                        date = e.getLong("date"),
                    )
                },
            )
        },
        reflections = o.optJSONArray("reflections").mapObjects {
            Reflection(
                id = it.getLong("id"),
                monthKey = it.optString("month", ""),
                wasteful = it.optString("a", ""),
                improved = it.optString("b", ""),
                nextGoal = it.optString("c", ""),
            )
        },
        budgetNeeds = o.optInt("bNeeds", 50),
        budgetWants = o.optInt("bWants", 30),
        saveRate = o.optInt("saveRate", 0),
        wallets = o.optJSONArray("wallets").mapObjects {
            Wallet(it.getLong("id"), it.optString("name", ""), it.optLong("opening", 0L))
        },
        transfers = o.optJSONArray("transfers").mapObjects {
            Transfer(it.getLong("id"), it.getLong("from"), it.getLong("to"), it.getLong("amount"), it.getLong("date"))
        },
        customExpense = o.optJSONArray("cEx").strings(),
        customIncome = o.optJSONArray("cIn").strings(),
        limits = parseLimits(o.optJSONObject("limits")),
        recurring = o.optJSONArray("recurring").mapObjects {
            Recurring(
                id = it.getLong("id"),
                isIncome = it.getBoolean("income"),
                amount = it.getLong("amount"),
                category = it.optString("category", "Boshqa"),
                note = it.optString("note", ""),
                anchorDay = it.optInt("anchorDay", 1),
                nextDue = it.getLong("nextDue"),
            )
        },
    )

    private fun parseLimits(o: JSONObject?): Map<String, Long> {
        if (o == null) return emptyMap()
        val out = LinkedHashMap<String, Long>()
        val keys = o.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = o.optLong(k, 0L)
            if (v > 0L) out[k] = v
        }
        return out
    }
}


private fun JSONArray?.strings(): List<String> {
    if (this == null) return emptyList()
    val out = ArrayList<String>()
    for (i in 0 until length()) {
        val v = optString(i, "")
        if (v.isNotEmpty()) out.add(v)
    }
    return out
}
