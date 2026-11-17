package uz.moliya.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun ReportScreen(data: FinanceData, onBack: () -> Unit) {
    var offset by rememberSaveable { mutableStateOf(0) }
    val start = Dates.monthStart(offset)
    val end = Dates.monthStart(offset + 1)
    val pStart = Dates.monthStart(offset - 1)

    val income = data.income(start, end)
    val expense = data.expense(start, end)
    val remainder = income - expense
    val prevIncome = data.income(pStart, start)
    val prevExpense = data.expense(pStart, start)
    val rate = data.savingsRate(start, end)
    val prevRate = data.savingsRate(pStart, start)

    val monthTxns = data.txns.filter { it.date >= start && it.date < end }
    val byCat = monthTxns.filter { !it.isIncome }
        .groupBy { it.category }
        .map { it.key to it.value.sumOf { t -> t.amount } }
        .sortedByDescending { it.second }
    val prevByCat = data.txns.filter { !it.isIncome && it.date >= pStart && it.date < start }
        .groupBy { it.category }
        .mapValues { it.value.sumOf { t -> t.amount } }
    val biggest = monthTxns.filter { !it.isIncome }.maxByOrNull { it.amount }

    val days = if (offset == 0) {
        Math.max(1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
    } else {
        Math.max(1, ((end - start) / 86_400_000L).toInt())
    }
    val dailyAvg = expense / days

    val insights = ArrayList<String>()
    if (income > 0 && rate != null) {
        when {
            rate >= 0.20f -> insights.add("Ajoyib: daromadingizning ${formatPercent(rate * 100.0)} qismini tejadingiz.")
            rate >= 0.10f -> insights.add("Yaxshi yo'ldasiz. 20% ga yetish uchun xarajatni ${formatMoney(Math.max(0L, expense - (income * 0.8).toLong()))} ga kamaytirish kerak.")
            rate >= 0f -> insights.add("Jamg'arma darajasi past (${formatPercent(rate * 100.0)}). Eng katta xarajat kategoriyasini ko'rib chiqing.")
            else -> insights.add("Xarajat daromaddan ${formatMoney(-remainder)} ortiq. Bu oy zaxiradan yoki qarzdan foydalangan bo'lishingiz mumkin.")
        }
    }
    if (prevExpense > 0 && expense > 0 && offset <= 0) {
        val diff = expense - prevExpense
        val pct = Math.abs(diff).toDouble() / prevExpense.toDouble() * 100.0
        if (pct >= 10.0) {
            insights.add(
                if (diff > 0) "Xarajat o'tgan oyga nisbatan ${formatPercent(pct)} oshgan."
                else "Xarajat o'tgan oyga nisbatan ${formatPercent(pct)} kamaygan. Barakalla!",
            )
        }
    }
    val growth = byCat.firstOrNull { (c, v) -> val p = prevByCat[c] ?: 0L; p > 0 && v > p * 3 / 2 && v - p > 50_000L }
    if (growth != null) insights.add("\"${growth.first}\" kategoriyasi o'tgan oydan 1.5 baravardan ko'p oshgan.")
    val breaches = data.limits.filter { (c, l) -> (byCat.firstOrNull { it.first == c }?.second ?: 0L) > l }
    if (breaches.isNotEmpty()) insights.add("Limitdan oshgan kategoriyalar: ${breaches.keys.joinToString(", ")}.")
    if (monthTxns.isEmpty()) insights.add("Bu oyda yozuv yo'q.")

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Orqaga")
                }
                Spacer(Modifier.width(4.dp))
                Column { ScreenHeader("Oylik hisobot", "Tahlil va tavsiyalar") }
            }
        }
        item {
            MonthSwitcher(
                label = Dates.monthLabel(offset),
                onPrev = { offset -= 1 },
                onNext = { if (offset < 0) offset += 1 },
                nextEnabled = offset < 0,
            )
        }
        item {
            AppCard {
                Text("Jamg'arma darajasi", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (rate == null) "—" else formatPercent(rate * 100.0),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = savingsColor(rate),
                )
                if (rate != null && prevRate != null) {
                    val d = (rate - prevRate) * 100.0
                    Text(
                        (if (d >= 0) "▲ " else "▼ ") + formatPercent(Math.abs(d)) + " o'tgan oyga nisbatan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (d >= 0) Brand.income else Brand.expense,
                    )
                }
                Spacer(Modifier.height(12.dp))
                ProgressBar(progress = ((rate ?: 0f) / 0.2f).coerceIn(0f, 1f), color = savingsColor(rate))
                Spacer(Modifier.height(6.dp))
                Text("Maqsad: 20%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            AppCard {
                ReportRow("Daromad", formatMoney(income), Brand.income, delta(income, prevIncome, true))
                Spacer(Modifier.height(12.dp))
                ReportRow("Xarajat", formatMoney(expense), Brand.expense, delta(expense, prevExpense, false))
                Spacer(Modifier.height(12.dp))
                ReportRow("Qoldiq", formatMoney(remainder), if (remainder >= 0) Brand.income else Brand.expense, null)
                Spacer(Modifier.height(12.dp))
                ReportRow("Kunlik o'rtacha xarajat", formatMoney(dailyAvg), MaterialTheme.colorScheme.onSurface, null)
            }
        }
        if (biggest != null) {
            item {
                AppCard {
                    Text("Eng katta xarajat", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(formatMoney(biggest.amount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Brand.expense)
                    Text(
                        "${biggest.category} · ${Dates.format(biggest.date)}" + if (biggest.note.isNotBlank()) " · ${biggest.note}" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (byCat.isNotEmpty()) {
            item {
                AppCard {
                    Text("Kategoriyalar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    byCat.forEach { (cat, value) ->
                        val prev = prevByCat[cat] ?: 0L
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(cat, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                val share = if (expense > 0) value.toDouble() / expense.toDouble() * 100.0 else 0.0
                                Text(
                                    formatPercent(share) + " xarajatdan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatMoney(value), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                val d = value - prev
                                if (prev > 0 || value > 0) {
                                    Text(
                                        (if (d > 0) "▲ " else if (d < 0) "▼ " else "") + formatMoney(Math.abs(d)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (d > 0) Brand.expense else if (d < 0) Brand.income else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        ProgressBar(progress = if (expense > 0) value.toFloat() / expense.toFloat() else 0f, color = Brand.categoryColor(cat), height = 6.dp)
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }
        }
        if (insights.isNotEmpty()) {
            item {
                AppCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = Brand.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Xulosalar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    insights.forEach {
                        Text("• $it", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

private fun delta(now: Long, prev: Long, higherIsGood: Boolean): Pair<String, Boolean>? {
    if (prev <= 0L) return null
    val d = now - prev
    if (d == 0L) return null
    val good = if (higherIsGood) d > 0 else d < 0
    return ((if (d > 0) "▲ " else "▼ ") + formatMoney(Math.abs(d))) to good
}

@Composable
private fun ReportRow(label: String, value: String, color: androidx.compose.ui.graphics.Color, delta: Pair<String, Boolean>?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(horizontalAlignment = Alignment.End) {
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = color)
            if (delta != null) {
                Text(
                    delta.first,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (delta.second) Brand.income else Brand.expense,
                )
            }
        }
    }
}
