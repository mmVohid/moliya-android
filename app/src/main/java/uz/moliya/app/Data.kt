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
) {
    val paid: Long get() = payments.sumOf { it.amount }
    val remaining: Long get() = (total - paid).coerceAtLeast(0L)
    val progress: Float
        get() = if (total <= 0L) 0f else (paid.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

data class FinanceData(
    val txns: List<Txn> = emptyList(),
    val cushion: List<CushionEntry> = emptyList(),
    val monthlyNeed: Long = 0L,
    val targetMonths: Int = 6,
    val debts: List<Debt> = emptyList(),
    val lastBackup: Long = 0L,
) {
    val cushionSaved: Long get() = cushion.sumOf { it.amount }.coerceAtLeast(0L)
    val cushionTarget: Long get() = monthlyNeed * targetMonths
    val debtRemaining: Long get() = debts.sumOf { it.remaining }
    val isEmpty: Boolean get() = txns.isEmpty() && cushion.isEmpty() && debts.isEmpty()

    fun income(start: Long, end: Long): Long =
        txns.filter { it.isIncome && it.date >= start && it.date < end }.sumOf { it.amount }

    fun expense(start: Long, end: Long): Long =
        txns.filter { !it.isIncome && it.date >= start && it.date < end }.sumOf { it.amount }
}

object Categories {
    val expense = listOf(
        "Oziq-ovqat", "Transport", "Uy-joy", "Aloqa", "Sog'liq",
        "Ta'lim", "Ko'ngilochar", "Kiyim", "Qarz to'lovi", "Boshqa",
    )
    val income = listOf("Oylik", "Qo'shimcha daromad", "Sovg'a", "Boshqa")
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

        val txns = JSONArray()
        for (t in d.txns) {
            val o = JSONObject()
            o.put("id", t.id)
            o.put("income", t.isIncome)
            o.put("amount", t.amount)
            o.put("category", t.category)
            o.put("note", t.note)
            o.put("date", t.date)
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
    )
}
