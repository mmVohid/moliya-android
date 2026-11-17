package uz.moliya.app

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Calendar
import java.util.zip.ZipInputStream
import kotlin.math.roundToLong

class ImportException(message: String) : Exception(message)

/**
 * Moliya yaratgan .xlsx zaxira faylini o'qiydi. Fayl Excel yoki Google Sheets'da qayta saqlangan bo'lsa ham
 * ishlaydi (sharedStrings qo'llab-quvvatlanadi).
 */
object XlsxImporter {

    private val KNOWN_SHEETS = setOf(
        "Daromad-Xarajat", "Himoya yostigi", "Qarzlar", "Qarz tolovlari", "Limitlar", "Takroriy", "Xulosa",
        "Shablonlar", "Maqsadlar", "Maqsad yozuvlari", "Refleksiya", "Byudjet", "Hamyonlar", "Otkazmalar", "Kategoriyalar",
    )

    fun read(input: InputStream): FinanceData {
        val files = HashMap<String, ByteArray>()
        try {
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                var count = 0
                while (entry != null) {
                    if (!entry.isDirectory && (entry.name.endsWith(".xml") || entry.name.endsWith(".rels"))) {
                        files[entry.name] = zip.readBytes()
                    }
                    count++
                    if (count > 500) throw ImportException("Fayl juda katta yoki noto'g'ri.")
                    entry = zip.nextEntry
                }
            }
        } catch (e: ImportException) {
            throw e
        } catch (e: Exception) {
            throw ImportException("Bu Excel (.xlsx) fayl emas yoki shikastlangan.")
        }

        val workbook = files["xl/workbook.xml"] ?: throw ImportException("Bu Excel (.xlsx) fayl emas yoki shikastlangan.")
        val sheetRefs = parseWorkbook(workbook)
        val rels = files["xl/_rels/workbook.xml.rels"]?.let { parseRels(it) } ?: emptyMap()
        val shared = files["xl/sharedStrings.xml"]?.let { parseShared(it) } ?: emptyList()

        val sheets = HashMap<String, List<List<String>>>()
        for ((name, rid) in sheetRefs) {
            val target = rels[rid] ?: continue
            val path = if (target.startsWith("/")) target.trimStart('/') else "xl/$target"
            val bytes = files[path] ?: continue
            sheets[name] = parseSheet(bytes, shared)
        }

        if (sheets.keys.none { it in KNOWN_SHEETS }) {
            throw ImportException("Bu Moliya zaxira fayli emas. Ilovadan \"Excelga yuklab olish\" orqali olingan faylni tanlang.")
        }

        val wallets = sheets["Hamyonlar"].dataRows().mapNotNull { r ->
            if (r.col(0).isEmpty()) null else Wallet(Ids.next(), r.col(0), num(r.col(1)))
        }
        val txns = sheets["Daromad-Xarajat"].dataRows().mapNotNull { r ->
            val amount = num(r.col(3))
            if (amount <= 0L) {
                null
            } else {
                Txn(
                    id = Ids.next(),
                    isIncome = r.col(1).equals("Daromad", ignoreCase = true),
                    amount = amount,
                    category = r.col(2).ifEmpty { "Boshqa" },
                    note = r.col(4),
                    date = date(r.col(0)),
                    currency = r.col(5).ifEmpty { "UZS" },
                    foreignAmount = r.col(6).replace(',', '.').toDoubleOrNull() ?: 0.0,
                    rate = r.col(7).replace(',', '.').toDoubleOrNull() ?: 0.0,
                    walletId = wallets.firstOrNull { w -> w.name == r.col(8) }?.id ?: 0L,
                )
            }
        }

        val cushion = sheets["Himoya yostigi"].dataRows().mapNotNull { r ->
            val amount = num(r.col(2))
            if (amount <= 0L) {
                null
            } else {
                val withdraw = r.col(1).startsWith("Yech", ignoreCase = true)
                CushionEntry(
                    id = Ids.next(),
                    amount = if (withdraw) -amount else amount,
                    note = r.col(3),
                    date = date(r.col(0)),
                )
            }
        }

        val debts = ArrayList<Debt>()
        for (r in sheets["Qarzlar"].dataRows()) {
            val surname = r.col(7)
            val name = r.col(0).let { if (surname.isNotEmpty() && it.endsWith(" $surname")) it.removeSuffix(" $surname").trim() else it }
            val total = num(r.col(2))
            if (name.isEmpty() || total <= 0L) continue
            val dueText = r.col(6)
            debts.add(
                Debt(
                    id = Ids.next(),
                    name = name,
                    surname = surname,
                    phone = r.col(8),
                    total = total,
                    interest = r.col(5).replace(',', '.').toDoubleOrNull() ?: 0.0,
                    dueDate = if (dueText.isEmpty()) 0L else date(dueText),
                    payments = emptyList(),
                    isLent = r.col(1).startsWith("Berilgan", ignoreCase = true),
                ),
            )
        }
        for (r in sheets["Qarz tolovlari"].dataRows()) {
            val name = r.col(0)
            val amount = num(r.col(3))
            if (name.isEmpty() || amount <= 0L) continue
            val lent = r.col(1).startsWith("Qaytarildi", ignoreCase = true)
            val index = debts.indexOfFirst { it.fullName == name && it.isLent == lent }
            if (index < 0) continue
            val debt = debts[index]
            debts[index] = debt.copy(
                payments = debt.payments + DebtPayment(Ids.next(), amount, date(r.col(2))),
            )
        }

        var monthlyNeed = 0L
        var targetMonths = 6
        for (r in sheets["Xulosa"].orEmpty()) {
            val key = r.col(0)
            if (key.startsWith("Himoya yostig") && key.contains("oylik")) monthlyNeed = num(r.col(1))
            if (key.startsWith("Himoya yostig") && key.contains("muddati")) {
                targetMonths = num(r.col(1)).toInt().coerceIn(1, 60)
            }
        }

        val limits = LinkedHashMap<String, Long>()
        for (r in sheets["Limitlar"].dataRows()) {
            val amount = num(r.col(1))
            if (r.col(0).isNotEmpty() && amount > 0L) limits[r.col(0)] = amount
        }

        val recurring = sheets["Takroriy"].dataRows().mapNotNull { r ->
            val amount = num(r.col(2))
            if (amount <= 0L) {
                null
            } else {
                Recurring(
                    id = Ids.next(),
                    isIncome = r.col(0).equals("Daromad", ignoreCase = true),
                    amount = amount,
                    category = r.col(1).ifEmpty { "Boshqa" },
                    note = r.col(3),
                    anchorDay = num(r.col(4)).toInt().coerceIn(1, 31),
                    nextDue = date(r.col(5)),
                )
            }
        }

        val templates = sheets["Shablonlar"].dataRows().mapNotNull { r ->
            val amount = num(r.col(2))
            if (amount <= 0L) {
                null
            } else {
                Template(
                    id = Ids.next(),
                    isIncome = r.col(0).equals("Daromad", ignoreCase = true),
                    amount = amount,
                    category = r.col(1).ifEmpty { "Boshqa" },
                    note = r.col(3),
                )
            }
        }

        val goals = ArrayList<Goal>()
        for (r in sheets["Maqsadlar"].dataRows()) {
            val name = r.col(0)
            val target = num(r.col(1))
            if (name.isEmpty() || target <= 0L) continue
            val deadlineText = r.col(3)
            goals.add(
                Goal(
                    id = Ids.next(),
                    name = name,
                    target = target,
                    deadline = if (deadlineText.isEmpty()) 0L else date(deadlineText),
                    entries = emptyList(),
                ),
            )
        }
        for (r in sheets["Maqsad yozuvlari"].dataRows()) {
            val index = goals.indexOfFirst { it.name == r.col(0) }
            val amount = num(r.col(3))
            if (index < 0 || amount <= 0L) continue
            val withdraw = r.col(2).startsWith("Yech", ignoreCase = true)
            val goal = goals[index]
            goals[index] = goal.copy(
                entries = goal.entries + CushionEntry(
                    id = Ids.next(),
                    amount = if (withdraw) -amount else amount,
                    note = r.col(4),
                    date = date(r.col(1)),
                ),
            )
        }

        val transfers = sheets["Otkazmalar"].dataRows().mapNotNull { r ->
            val from = wallets.firstOrNull { it.name == r.col(1) }
            val to = wallets.firstOrNull { it.name == r.col(2) }
            val amount = num(r.col(3))
            if (from == null || to == null || amount <= 0L) null
            else Transfer(Ids.next(), from.id, to.id, amount, date(r.col(0)))
        }
        val customExpense = ArrayList<String>()
        val customIncome = ArrayList<String>()
        for (r in sheets["Kategoriyalar"].dataRows()) {
            val name = r.col(1)
            if (name.isEmpty()) continue
            if (r.col(0).equals("Daromad", true)) customIncome.add(name) else customExpense.add(name)
        }
        val reflections = sheets["Refleksiya"].dataRows().mapNotNull { r ->
            if (r.col(0).isEmpty()) null
            else Reflection(Ids.next(), r.col(0), r.col(1), r.col(2), r.col(3))
        }
        val budget = HashMap<String, Int>()
        for (r in sheets["Byudjet"].dataRows()) budget[r.col(0)] = num(r.col(1)).toInt()

        return FinanceData(
            txns = txns,
            cushion = cushion,
            monthlyNeed = monthlyNeed,
            targetMonths = targetMonths,
            debts = debts,
            limits = limits,
            recurring = recurring,
            templates = templates,
            goals = goals,
            reflections = reflections,
            budgetNeeds = budget["Zarur %"] ?: 50,
            budgetWants = budget["Xohish %"] ?: 30,
            saveRate = budget["Jamg'arma taklifi %"] ?: 0,
            wallets = wallets,
            transfers = transfers,
            customExpense = customExpense,
            customIncome = customIncome,
        )
    }

    // ───── Yordamchi funksiyalar ─────

    private fun List<List<String>>?.dataRows(): List<List<String>> = this?.drop(1) ?: emptyList()

    private fun List<String>.col(i: Int): String = getOrElse(i) { "" }.trim()

    private fun num(s: String): Long {
        val t = s.replace(" ", "").replace(" ", "").replace(',', '.')
        return t.toDoubleOrNull()?.roundToLong() ?: 0L
    }

    private fun date(text: String): Long {
        val t = text.trim()
        val iso = Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})").find(t)
        if (iso != null) {
            return localNoon(iso.groupValues[1].toInt(), iso.groupValues[2].toInt(), iso.groupValues[3].toInt())
        }
        val dotted = Regex("^(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})").find(t)
        if (dotted != null) {
            return localNoon(dotted.groupValues[3].toInt(), dotted.groupValues[2].toInt(), dotted.groupValues[1].toInt())
        }
        val serial = t.toDoubleOrNull()
        if (serial != null && serial > 20000.0) {
            // Excel sana raqami (1899-12-30 dan beri kunlar)
            val utcMillis = ((serial - 25569.0) * 86_400_000.0).toLong()
            return Dates.fromPickerMillis(utcMillis)
        }
        return Dates.today()
    }

    private fun localNoon(year: Int, month: Int, day: Int): Long {
        val c = Calendar.getInstance()
        c.clear()
        c.set(year, (month - 1).coerceIn(0, 11), day.coerceIn(1, 31), 12, 0, 0)
        return c.timeInMillis
    }

    private fun columnIndex(ref: String): Int {
        var n = 0
        for (ch in ref) {
            when (ch) {
                in 'A'..'Z' -> n = n * 26 + (ch - 'A' + 1)
                in 'a'..'z' -> n = n * 26 + (ch - 'a' + 1)
                else -> break
            }
        }
        return n - 1
    }

    private fun newParser(bytes: ByteArray): XmlPullParser {
        val p = Xml.newPullParser()
        p.setInput(ByteArrayInputStream(bytes), "UTF-8")
        return p
    }

    private fun parseWorkbook(bytes: ByteArray): List<Pair<String, String>> {
        val p = newParser(bytes)
        val out = ArrayList<Pair<String, String>>()
        var event = p.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && p.name == "sheet") {
                val name = p.getAttributeValue(null, "name") ?: ""
                var rid = ""
                for (i in 0 until p.attributeCount) {
                    val attr = p.getAttributeName(i)
                    if (attr == "r:id" || attr.endsWith(":id")) rid = p.getAttributeValue(i)
                }
                if (name.isNotEmpty() && rid.isNotEmpty()) out.add(name to rid)
            }
            event = p.next()
        }
        return out
    }

    private fun parseRels(bytes: ByteArray): Map<String, String> {
        val p = newParser(bytes)
        val out = HashMap<String, String>()
        var event = p.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && p.name == "Relationship") {
                val id = p.getAttributeValue(null, "Id")
                val target = p.getAttributeValue(null, "Target")
                if (id != null && target != null) out[id] = target
            }
            event = p.next()
        }
        return out
    }

    private fun parseShared(bytes: ByteArray): List<String> {
        val p = newParser(bytes)
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var inSi = false
        var inT = false
        var inPhonetic = false
        var event = p.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "si" -> {
                        inSi = true
                        sb.setLength(0)
                    }
                    "rPh" -> inPhonetic = true
                    "t" -> inT = inSi && !inPhonetic
                }
                XmlPullParser.TEXT -> if (inT) sb.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "t" -> inT = false
                    "rPh" -> inPhonetic = false
                    "si" -> {
                        out.add(sb.toString())
                        inSi = false
                    }
                }
            }
            event = p.next()
        }
        return out
    }

    private fun parseSheet(bytes: ByteArray, shared: List<String>): List<List<String>> {
        val p = newParser(bytes)
        val rows = ArrayList<List<String>>()
        var row: MutableList<String>? = null
        var col = 0
        var type = ""
        var inText = false
        val text = StringBuilder()
        var event = p.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "row" -> row = ArrayList()
                    "c" -> {
                        type = p.getAttributeValue(null, "t") ?: ""
                        col = columnIndex(p.getAttributeValue(null, "r") ?: "")
                        text.setLength(0)
                    }
                    "v", "t" -> inText = true
                }
                XmlPullParser.TEXT -> if (inText) text.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "v", "t" -> inText = false
                    "c" -> {
                        val r = row
                        if (r != null) {
                            var value = text.toString()
                            if (type == "s") value = shared.getOrElse(value.trim().toIntOrNull() ?: -1) { "" }
                            val target = if (col < 0) r.size else col
                            while (r.size < target) r.add("")
                            r.add(value)
                        }
                    }
                    "row" -> {
                        rows.add(row ?: emptyList())
                        row = null
                    }
                }
            }
            event = p.next()
        }
        return rows
    }
}
