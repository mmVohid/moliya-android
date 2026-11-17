package uz.moliya.app

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
private fun BackHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Orqaga")
        }
        Spacer(Modifier.width(4.dp))
        Column { ScreenHeader(title, subtitle) }
    }
}

private fun savedUntil(data: FinanceData, endExclusive: Long): Long {
    val cushion = data.cushion.filter { it.date < endExclusive }.sumOf { it.amount }.coerceAtLeast(0L)
    val goals = data.goals.sumOf { g -> g.entries.filter { it.date < endExclusive }.sumOf { it.amount }.coerceAtLeast(0L) }
    return cushion + goals
}

@Composable
fun NetWorthScreen(
    data: FinanceData,
    hidden: Boolean,
    onBack: () -> Unit,
    onAddWallet: (Wallet) -> Unit,
    onDeleteWallet: (Wallet) -> Unit,
    onTransfer: (Transfer) -> Unit,
) {
    val cash = data.wallets.sumOf { data.walletBalance(it) }
    val savings = data.cushionSaved + data.goals.sumOf { it.saved }
    val lent = data.debtLent
    val owed = data.debtOwed
    val net = cash + savings + lent - owed
    val history = (-11..0).map { savedUntil(data, Dates.monthStart(it + 1)) }
    val labels = (-11..0).map { Dates.shortMonth(it) }
    val growth = history.last() - history.first()

    fun m(v: Long) = if (hidden) "•••••••" else formatMoney(v)

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { BackHeader("Sof boylik", "Aktivlar va majburiyatlar farqi", onBack) }
        item {
            AppCard {
                Text("Sof boylik", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (hidden) "•••••••" else (if (net < 0) "−" else "") + formatMoney(Math.abs(net)),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (net >= 0) Brand.income else Brand.expense,
                )
                Spacer(Modifier.height(16.dp))
                if (data.wallets.isNotEmpty()) {
                    NetRow("Hamyonlar", (if (cash < 0) "−" else "") + m(Math.abs(cash)), if (cash >= 0) Brand.income else Brand.expense)
                    Spacer(Modifier.height(10.dp))
                }
                NetRow("Jamg'arma (yostiq + maqsadlar)", m(savings), Brand.income)
                Spacer(Modifier.height(10.dp))
                NetRow("Menga qarzdorlar", m(lent), Brand.income)
                Spacer(Modifier.height(10.dp))
                NetRow("Mening qarzlarim", "−" + m(owed), Brand.expense)
            }
        }
        item { WalletsCard(data, hidden, onAddWallet, onDeleteWallet, onTransfer) }
        item {
            AppCard {
                Text("Jamg'arma o'sishi (12 oy)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (hidden) "—" else (if (growth >= 0) "▲ " else "▼ ") + formatMoney(Math.abs(growth)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (growth >= 0) Brand.income else Brand.expense,
                )
                Spacer(Modifier.height(12.dp))
                if (hidden) {
                    Text("Balans yashirilgan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LineChart(history, Brand.primary)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(labels.first(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(labels.last(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Text(
                "Eslatma: qarz yozuvlarida yaratilish sanasi saqlanmagani uchun grafik faqat jamg'arma o'sishini ko'rsatadi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NetRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun LineChart(values: List<Long>, color: Color) {
    val track = Brand.track
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
        val min = Math.min(0L, values.minOrNull() ?: 0L).toFloat()
        val range = (max - min).coerceAtLeast(1f)
        val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f
        drawLine(track, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = stepX * i
            val y = size.height - (v - min) / range * (size.height - 8f) - 4f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 6f, cap = StrokeCap.Round))
        val last = values.lastIndex
        drawCircle(
            color,
            radius = 9f,
            center = Offset(stepX * last, size.height - (values[last] - min) / range * (size.height - 8f) - 4f),
        )
    }
}

@Composable
fun YearScreen(data: FinanceData, onBack: () -> Unit) {
    var yearOffset by rememberSaveable { mutableStateOf(0) }
    val curMonth = Calendar.getInstance().get(Calendar.MONTH)
    val firstOffset = -curMonth + 12 * yearOffset
    val year = Calendar.getInstance().get(Calendar.YEAR) + yearOffset
    val months = (0..11).map { firstOffset + it }
    val incomes = months.map { data.income(Dates.monthStart(it), Dates.monthStart(it + 1)) }
    val expenses = months.map { data.expense(Dates.monthStart(it), Dates.monthStart(it + 1)) }
    val totalIn = incomes.sum()
    val totalOut = expenses.sum()
    val yStart = Dates.monthStart(firstOffset)
    val yEnd = Dates.monthStart(firstOffset + 12)
    val topCats = data.txns.filter { !it.isIncome && it.date >= yStart && it.date < yEnd }
        .groupBy { it.category }.map { it.key to it.value.sumOf { t -> t.amount } }
        .sortedByDescending { it.second }.take(5)
    val rate = if (totalIn > 0) (totalIn - totalOut).toDouble() / totalIn.toDouble() * 100.0 else null
    val maxVal = (incomes + expenses).maxOrNull()?.coerceAtLeast(1L) ?: 1L

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { BackHeader("Yillik hisobot", "$year-yil", onBack) }
        item {
            MonthSwitcher(
                label = "$year",
                onPrev = { yearOffset -= 1 },
                onNext = { if (yearOffset < 0) yearOffset += 1 },
                nextEnabled = yearOffset < 0,
            )
        }
        item {
            AppCard {
                NetRow("Jami daromad", formatMoney(totalIn), Brand.income)
                Spacer(Modifier.height(10.dp))
                NetRow("Jami xarajat", formatMoney(totalOut), Brand.expense)
                Spacer(Modifier.height(10.dp))
                NetRow("Qoldiq", formatMoney(totalIn - totalOut), if (totalIn >= totalOut) Brand.income else Brand.expense)
                Spacer(Modifier.height(10.dp))
                NetRow(
                    "O'rtacha jamg'arma darajasi",
                    if (rate == null) "—" else formatPercent(rate),
                    savingsColor(rate?.let { (it / 100.0).toFloat() }),
                )
            }
        }
        item {
            AppCard {
                Text("Oylar bo'yicha", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                months.forEachIndexed { i, off ->
                    Text(Dates.monthNames[(i) % 12], style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    ProgressBar(progress = incomes[i].toFloat() / maxVal.toFloat(), color = Brand.income, height = 5.dp)
                    Spacer(Modifier.height(3.dp))
                    ProgressBar(progress = expenses[i].toFloat() / maxVal.toFloat(), color = Brand.expense, height = 5.dp)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("+${formatMoney(incomes[i])}", style = MaterialTheme.typography.bodySmall, color = Brand.income)
                        Text("−${formatMoney(expenses[i])}", style = MaterialTheme.typography.bodySmall, color = Brand.expense)
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
        if (topCats.isNotEmpty()) {
            item {
                AppCard {
                    Text("Eng katta xarajat kategoriyalari", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    topCats.forEach { (c, v) ->
                        NetRow(c, formatMoney(v), MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
