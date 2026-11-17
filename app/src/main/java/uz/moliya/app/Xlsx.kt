package uz.moliya.app

import java.io.BufferedOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Tashqi kutubxonasiz oddiy .xlsx (Office Open XML) yozuvchi.
 * Excel, Google Sheets va LibreOffice ochadi.
 */
object XlsxExporter {

    private class Sheet(
        val name: String,
        val widths: List<Int>,
        val rows: List<List<Any>>,
        val boldRows: Set<Int> = setOf(0),
    )

    fun write(data: FinanceData, out: OutputStream) {
        val sheets = listOf(
            summarySheet(data),
            monthlySheet(data),
            txnSheet(data),
            cushionSheet(data),
            debtSheet(data),
            paymentSheet(data),
        )
        ZipOutputStream(BufferedOutputStream(out)).use { zip ->
            zip.entry("[Content_Types].xml", contentTypes(sheets.size))
            zip.entry("_rels/.rels", rootRels())
            zip.entry("xl/workbook.xml", workbook(sheets))
            zip.entry("xl/_rels/workbook.xml.rels", workbookRels(sheets.size))
            zip.entry("xl/styles.xml", styles())
            sheets.forEachIndexed { i, s -> zip.entry("xl/worksheets/sheet${i + 1}.xml", sheetXml(s)) }
        }
    }

    // ───── Varaqlar ─────

    private fun summarySheet(d: FinanceData): Sheet {
        val totalIncome = d.txns.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = d.txns.filter { !it.isIncome }.sumOf { it.amount }
        val start = Dates.monthStart(0)
        val end = Dates.monthStart(1)
        val rows = listOf(
            listOf("Ko'rsatkich", "Qiymat"),
            listOf("Hisobot yaratilgan vaqt", Dates.formatDateTime(System.currentTimeMillis())),
            listOf("Valyuta", "so'm"),
            listOf("", ""),
            listOf("Jami daromad", totalIncome),
            listOf("Jami xarajat", totalExpense),
            listOf("Umumiy balans (daromad - xarajat)", totalIncome - totalExpense),
            listOf("Joriy oy daromadi", d.income(start, end)),
            listOf("Joriy oy xarajati", d.expense(start, end)),
            listOf("", ""),
            listOf("Himoya yostig'i: jamg'arilgan", d.cushionSaved),
            listOf("Himoya yostig'i: oylik zaruriy xarajat", d.monthlyNeed),
            listOf("Himoya yostig'i: maqsad muddati (oy)", d.targetMonths),
            listOf("Himoya yostig'i: maqsad summasi", d.cushionTarget),
            listOf("", ""),
            listOf("Faol qarzlar soni", d.debts.count { it.remaining > 0 }),
            listOf("Qarz qoldig'i (jami)", d.debtRemaining),
        )
        return Sheet("Xulosa", listOf(44, 26), rows)
    }

    private fun monthlySheet(d: FinanceData): Sheet {
        val keys = d.txns.map { Dates.monthKey(it.date) }.distinct().sorted()
        val rows = mutableListOf<List<Any>>(listOf("Oy", "Daromad", "Xarajat", "Balans"))
        for (k in keys) {
            val inMonth = d.txns.filter { Dates.monthKey(it.date) == k }
            val inc = inMonth.filter { it.isIncome }.sumOf { it.amount }
            val exp = inMonth.filter { !it.isIncome }.sumOf { it.amount }
            rows.add(listOf(k, inc, exp, inc - exp))
        }
        return Sheet("Oylik", listOf(12, 18, 18, 18), rows)
    }

    private fun txnSheet(d: FinanceData): Sheet {
        val rows = mutableListOf<List<Any>>(listOf("Sana", "Turi", "Kategoriya", "Summa (so'm)", "Izoh"))
        for (t in d.txns.sortedWith(compareBy<Txn> { it.date }.thenBy { it.id })) {
            rows.add(
                listOf(
                    Dates.iso(t.date),
                    if (t.isIncome) "Daromad" else "Xarajat",
                    t.category,
                    t.amount,
                    t.note,
                ),
            )
        }
        return Sheet("Daromad-Xarajat", listOf(14, 12, 22, 18, 40), rows)
    }

    private fun cushionSheet(d: FinanceData): Sheet {
        val rows = mutableListOf<List<Any>>(listOf("Sana", "Amal", "Summa (so'm)", "Izoh"))
        for (c in d.cushion.sortedWith(compareBy<CushionEntry> { it.date }.thenBy { it.id })) {
            rows.add(
                listOf(
                    Dates.iso(c.date),
                    if (c.amount >= 0) "Qo'shildi" else "Yechildi",
                    Math.abs(c.amount),
                    c.note,
                ),
            )
        }
        return Sheet("Himoya yostigi", listOf(14, 14, 18, 40), rows)
    }

    private fun debtSheet(d: FinanceData): Sheet {
        val rows = mutableListOf<List<Any>>(
            listOf("Qarz nomi", "Umumiy summa", "To'langan", "Qoldiq", "Foiz (%)", "Muddat"),
        )
        for (debt in d.debts) {
            rows.add(
                listOf(
                    debt.name,
                    debt.total,
                    debt.paid,
                    debt.remaining,
                    debt.interest,
                    if (debt.dueDate > 0L) Dates.iso(debt.dueDate) else "",
                ),
            )
        }
        return Sheet("Qarzlar", listOf(26, 18, 18, 18, 12, 14), rows)
    }

    private fun paymentSheet(d: FinanceData): Sheet {
        val rows = mutableListOf<List<Any>>(listOf("Qarz nomi", "Sana", "To'lov (so'm)"))
        for (debt in d.debts) {
            for (p in debt.payments.sortedBy { it.date }) {
                rows.add(listOf(debt.name, Dates.iso(p.date), p.amount))
            }
        }
        return Sheet("Qarz tolovlari", listOf(26, 14, 18), rows)
    }

    // ───── XML ─────

    private fun ZipOutputStream.entry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private const val XML_HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
    private const val NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val NS_PKG_REL = "http://schemas.openxmlformats.org/package/2006/relationships"

    private fun esc(s: String): String {
        val sb = StringBuilder(s.length + 8)
        for (ch in s) {
            when {
                ch == '&' -> sb.append("&amp;")
                ch == '<' -> sb.append("&lt;")
                ch == '>' -> sb.append("&gt;")
                ch == '"' -> sb.append("&quot;")
                ch == '\'' -> sb.append("&apos;")
                ch.code < 0x20 && ch != '\n' && ch != '\t' && ch != '\r' -> Unit
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun contentTypes(sheetCount: Int): String {
        val sb = StringBuilder(XML_HEAD)
        sb.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
        sb.append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
        sb.append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        sb.append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
        for (i in 1..sheetCount) {
            sb.append("<Override PartName=\"/xl/worksheets/sheet$i.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
        }
        sb.append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
        sb.append("</Types>")
        return sb.toString()
    }

    private fun rootRels(): String =
        XML_HEAD + "<Relationships xmlns=\"$NS_PKG_REL\">" +
            "<Relationship Id=\"rId1\" Type=\"$NS_REL/officeDocument\" Target=\"xl/workbook.xml\"/>" +
            "</Relationships>"

    private fun workbook(sheets: List<Sheet>): String {
        val sb = StringBuilder(XML_HEAD)
        sb.append("<workbook xmlns=\"$NS_MAIN\" xmlns:r=\"$NS_REL\"><sheets>")
        sheets.forEachIndexed { i, s ->
            sb.append("<sheet name=\"${esc(s.name)}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>")
        }
        sb.append("</sheets></workbook>")
        return sb.toString()
    }

    private fun workbookRels(sheetCount: Int): String {
        val sb = StringBuilder(XML_HEAD)
        sb.append("<Relationships xmlns=\"$NS_PKG_REL\">")
        for (i in 1..sheetCount) {
            sb.append("<Relationship Id=\"rId$i\" Type=\"$NS_REL/worksheet\" Target=\"worksheets/sheet$i.xml\"/>")
        }
        sb.append("<Relationship Id=\"rId${sheetCount + 1}\" Type=\"$NS_REL/styles\" Target=\"styles.xml\"/>")
        sb.append("</Relationships>")
        return sb.toString()
    }

    private fun styles(): String =
        XML_HEAD + "<styleSheet xmlns=\"$NS_MAIN\">" +
            "<fonts count=\"2\">" +
            "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
            "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
            "</fonts>" +
            "<fills count=\"3\">" +
            "<fill><patternFill patternType=\"none\"/></fill>" +
            "<fill><patternFill patternType=\"gray125\"/></fill>" +
            "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFCCEFEA\"/><bgColor indexed=\"64\"/></patternFill></fill>" +
            "</fills>" +
            "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
            "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
            "<cellXfs count=\"3\">" +
            "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
            "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>" +
            "<xf numFmtId=\"3\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>" +
            "</cellXfs>" +
            "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>" +
            "</styleSheet>"

    private fun sheetXml(s: Sheet): String {
        val sb = StringBuilder(XML_HEAD)
        sb.append("<worksheet xmlns=\"$NS_MAIN\">")
        sb.append("<cols>")
        s.widths.forEachIndexed { i, w ->
            sb.append("<col min=\"${i + 1}\" max=\"${i + 1}\" width=\"$w\" customWidth=\"1\"/>")
        }
        sb.append("</cols><sheetData>")
        s.rows.forEachIndexed { r, row ->
            sb.append("<row r=\"${r + 1}\">")
            val bold = r in s.boldRows
            row.forEachIndexed { c, value -> sb.append(cell(c, r, value, bold)) }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun cell(col: Int, row: Int, value: Any, bold: Boolean): String {
        val ref = "${'A' + col}${row + 1}"
        return when (value) {
            is Long, is Int -> "<c r=\"$ref\" s=\"2\"><v>$value</v></c>"
            is Double -> "<c r=\"$ref\"><v>$value</v></c>"
            else -> {
                val style = if (bold) " s=\"1\"" else ""
                "<c r=\"$ref\" t=\"inlineStr\"$style><is><t xml:space=\"preserve\">${esc(value.toString())}</t></is></c>"
            }
        }
    }
}
