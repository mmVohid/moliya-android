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
import androidx.compose.runtime.remember
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
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
fun ReportScreen(
    data: FinanceData,
    onBack: () -> Unit,
    onSaveReflection: (Reflection) -> Unit,
    onSetBudget: (Int, Int, Int) -> Unit,
) {
    var showBudget by remember { mutableStateOf(false) }
    var showReflection by remember { mutableStateOf(false) }
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
        Math.max(1, ((System.currentTimeMillis() - start) / 86_400_000L).toInt() + 1)
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
    val totalLimit = data.limits[""] ?: 0L
    if (totalLimit > 0L && expense > totalLimit) insights.add("Oylik xarajat limitidan ${formatMoney(expense - totalLimit)} oshgan.")
    if (monthTxns.isEmpty()) insights.add("Bu oyda yozuv yo'q.")

    if (showBudget) {
        BudgetSheet(data.budgetNeeds, data.budgetWants, data.saveRate, onSetBudget) { showBudget = false }
    }
    if (showReflection) {
        val key = Dates.monthKey(start)
        ReflectionSheet(data.reflections.firstOrNull { it.monthKey == key }, key, onSaveReflection) { showReflection = false }
    }

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
                        Dates.format(biggest.date) + if (biggest.note.isNotBlank()) " · ${biggest.note}" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            val key = Dates.monthKey(start)
            val refl = data.reflections.firstOrNull { it.monthKey == key }
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Oy refleksiyasi",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(onClick = { showReflection = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(if (refl == null) "Yozish" else "Tahrirlash")
                    }
                }
                if (refl == null) {
                    Text(
                        "3 ta qisqa savolga javob bering. Bu odatlaringizni anglashga yordam beradi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    ReflectionLine("Eng keraksiz xarajat", refl.wasteful)
                    ReflectionLine("Yaxshilaganim", refl.improved)
                    ReflectionLine("Keyingi oy maqsadi", refl.nextGoal)
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

@Composable
private fun BudgetLine(label: String, actual: Long, plan: Long, limit: Boolean) {
    val over = limit && actual > plan
    val color = if (limit) { if (over) Brand.expense else Brand.primary } else { if (actual >= plan) Brand.income else Brand.warning }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(
            "${formatMoney(actual)} / ${formatMoney(plan)}",
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
    Spacer(Modifier.height(6.dp))
    ProgressBar(progress = if (plan > 0) actual.toFloat() / plan.toFloat() else 0f, color = color, height = 6.dp)
}

@Composable
private fun ReflectionLine(title: String, text: String) {
    if (text.isBlank()) return
    Spacer(Modifier.height(8.dp))
    Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
internal fun BudgetSheet(needs: Int, wants: Int, saveRate: Int, onSave: (Int, Int, Int) -> Unit, onDismiss: () -> Unit) {
    var n by remember { mutableStateOf(needs.toFloat()) }
    var w by remember { mutableStateOf(wants.toFloat()) }
    var r by remember { mutableStateOf(saveRate.toFloat()) }
    val save = 100 - n.toInt() - w.toInt()
    FormSheet(title = "Byudjet sozlamalari", onDismiss = onDismiss) {
        Text("Zarur xarajatlar: ${n.toInt()}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Slider(value = n, onValueChange = { n = it.coerceAtMost(100f - w) }, valueRange = 0f..100f, steps = 19)
        Text("Xohishlar: ${w.toInt()}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Slider(value = w, onValueChange = { w = it.coerceAtMost(100f - n) }, valueRange = 0f..100f, steps = 19)
        Text("Jamg'arma: $save%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Brand.primary)
        Text(
            "O'zingizga birinchi to'lang: daromad kiritganda jamg'armaga ajratish taklifi: " +
                if (r.toInt() == 0) "o'chiq" else "${r.toInt()}%",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Slider(value = r, onValueChange = { r = it }, valueRange = 0f..50f, steps = 9)
        BigButton(
            text = "Saqlash",
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(n.toInt(), w.toInt(), r.toInt())
                onDismiss()
            },
        )
    }
}

@Composable
private fun ReflectionSheet(existing: Reflection?, monthKey: String, onSave: (Reflection) -> Unit, onDismiss: () -> Unit) {
    var a by remember { mutableStateOf(existing?.wasteful ?: "") }
    var b by remember { mutableStateOf(existing?.improved ?: "") }
    var c by remember { mutableStateOf(existing?.nextGoal ?: "") }
    FormSheet(title = "Oy refleksiyasi", onDismiss = onDismiss) {
        OutlinedTextField(value = a, onValueChange = { a = it.take(200) }, modifier = Modifier.fillMaxWidth(), label = { Text("Eng keraksiz xarajatim nima edi?") }, minLines = 2, shape = RoundedCornerShape(14.dp))
        OutlinedTextField(value = b, onValueChange = { b = it.take(200) }, modifier = Modifier.fillMaxWidth(), label = { Text("Bu oy nimani yaxshiladim?") }, minLines = 2, shape = RoundedCornerShape(14.dp))
        OutlinedTextField(value = c, onValueChange = { c = it.take(200) }, modifier = Modifier.fillMaxWidth(), label = { Text("Keyingi oy asosiy maqsadim?") }, minLines = 2, shape = RoundedCornerShape(14.dp))
        BigButton(
            text = "Saqlash",
            enabled = a.isNotBlank() || b.isNotBlank() || c.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(Reflection(existing?.id ?: Ids.next(), monthKey, a.trim(), b.trim(), c.trim()))
                onDismiss()
            },
        )
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
