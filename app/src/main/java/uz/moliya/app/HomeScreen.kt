package uz.moliya.app

import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    data: FinanceData,
    onOpenTab: (Int) -> Unit,
    onToggleHide: () -> Unit,
    onOpenSettings: () -> Unit,
    lastBackupAt: Long,
    userName: String,
    cards: Int,
    backupReminder: Boolean,
    autoBackupOn: Boolean,
    firstEntryAt: Long,
    onQuickAdd: (Boolean) -> Unit,
    onOpenReport: () -> Unit,
    onOpenNetWorth: () -> Unit,
    onOpenYear: () -> Unit,
) {
    val start = Dates.monthStart(0)
    val end = Dates.monthStart(1)
    val income = data.income(start, end)
    val expense = data.expense(start, end)
    val balance = income - expense

    val offsets = (-11..0).toList()
    val incomes = offsets.map { data.income(Dates.monthStart(it), Dates.monthStart(it + 1)) }
    val expenses = offsets.map { data.expense(Dates.monthStart(it), Dates.monthStart(it + 1)) }
    val labels = offsets.map { Dates.shortMonth(it) }


    val weekMs = 7L * 24 * 60 * 60 * 1000
    // Avto-zaxira yoqilmagan bo'lsa: oxirgi nusxadan (yo'q bo'lsa birinchi yozuvdan) 7 kun o'tgach ogohlantiramiz
    val reference = if (lastBackupAt > 0L) lastBackupAt else firstEntryAt
    val staleBackup = backupReminder && !data.isEmpty && !autoBackupOn && reference > 0L &&
        System.currentTimeMillis() - reference > weekMs
    val staleDays = ((System.currentTimeMillis() - reference) / (24L * 60 * 60 * 1000)).toInt()

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp + LocalBottomInset.current),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Box(Modifier.fillMaxWidth()) {
                ScreenHeader("Moliya", Dates.monthLabel(0))
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp)
                        .size(48.dp),
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Sozlamalar")
                }
            }
        }

        item {
            HeroCard(
                balance = balance,
                income = income,
                expense = expense,
                hidden = data.hideBalance,
                onToggleHide = onToggleHide,
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigButton(
                    text = "Daromad",
                    icon = Icons.Rounded.Add,
                    container = Brand.income,
                    onClick = { onQuickAdd(true) },
                    modifier = Modifier.weight(1f),
                )
                BigButton(
                    text = "Xarajat",
                    icon = Icons.Rounded.Remove,
                    container = Brand.expense,
                    onClick = { onQuickAdd(false) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (staleBackup) {
            item {
                AppCard(
                    onClick = { onOpenTab(4) },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    padding = 16.dp,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = Brand.warning,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Zaxira nusxa kerak",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Brand.warning,
                            )
                            Text(
                                if (lastBackupAt == 0L) "Birinchi yozuvdan $staleDays kun o'tdi, hali zaxira olinmagan. Backup bo'limiga o'ting."
                                else "Oxirgi zaxira $staleDays kun oldin (${Dates.format(lastBackupAt)}). Yangilab qo'ying.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
            }
        }

        run {
        if (cards and 4 != 0) item { SectionTitle("Bu oy") }
        if (cards and 4 != 0) item { SavingsRateCard(data = data, onClick = onOpenReport) }
        if (cards and 1 != 0) item { SectionTitle("Oxirgi 12 oy") }
        if (cards and 1 != 0) item {
            AppCard {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot("Daromad", Brand.income)
                    LegendDot("Xarajat", Brand.expense)
                }
                Spacer(Modifier.height(16.dp))
                val details = offsets.mapIndexed { i, off ->
                    if (data.hideBalance) Dates.monthLabel(off) + ": •••••••"
                    else Dates.monthLabel(off) + ":  +" + formatMoney(incomes[i]) + "   −" + formatMoney(expenses[i])
                }
                DualBarChart(incomes, expenses, labels, details)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Oy ustiga bosing: tafsilot yuqorida ko'rinadi. Oldingi oylar uchun chapga suring.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (cards and 8 != 0) item { SectionTitle("Jamg'arma va qarzlar") }
        if (cards and 8 != 0) item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CushionMini(data, Modifier.weight(1f).fillMaxHeight()) { onOpenTab(2) }
                DebtMini(data, Modifier.weight(1f).fillMaxHeight()) { onOpenTab(3) }
            }
        }
        if (cards and 16 != 0) item { SectionTitle("Tahlil") }
        if (cards and 16 != 0) item {
            AppCard(padding = 4.dp) {
                NavRow(Icons.Rounded.Assessment, "Oylik hisobot", "Tejash, byudjet va xulosalar", onOpenReport)
                NavRow(Icons.Rounded.AccountBalance, "Sof boylik", "Hamyonlar, jamg'arma va qarzlar", onOpenNetWorth)
                NavRow(Icons.Rounded.CalendarMonth, "Yillik hisobot", "Butun yil bo'yicha natija", onOpenYear)
            }
        }
        }
    }
}

@Composable
private fun HeroCard(
    balance: Long,
    income: Long,
    expense: Long,
    hidden: Boolean,
    onToggleHide: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Brand.heroStart, Brand.heroEnd)))
            .padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Oy balansi",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
                IconButton(onClick = onToggleHide, modifier = Modifier.size(48.dp)) {
                    Icon(
                        if (hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (hidden) "Balansni ko'rsatish" else "Balansni yashirish",
                        tint = Color.White,
                    )
                }
            }
            Column(Modifier.padding(end = 12.dp)) {
                Text(
                    if (hidden) maskedMoney(0L, true) else formatMoney(animatedLong(balance)),
                    fontSize = 32.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeroStat("Daromad", income, hidden, Modifier.weight(1f))
                    HeroStat("Xarajat", expense, hidden, Modifier.weight(1f))
                }
            }
        }
    }
}

private fun maskedMoney(value: Long, hidden: Boolean): String =
    if (hidden) "••••••• so'm" else formatMoney(value)

@Composable
private fun HeroStat(label: String, value: Long, hidden: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
        Spacer(Modifier.height(2.dp))
        Text(
            if (hidden) maskedMoney(0L, true) else formatMoney(animatedLong(value)),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LegendDot(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DualBarChart(income: List<Long>, expense: List<Long>, labels: List<String>, details: List<String>) {
    var selected by remember { mutableStateOf(labels.lastIndex) }
    val maxValue = maxOf(1L, income.maxOrNull() ?: 0L, expense.maxOrNull() ?: 0L)
    val scroll = rememberScrollState()
    // Joriy oy (o'ngdagi) ko'rinib turishi uchun oxirigacha suramiz
    LaunchedEffect(scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
    Text(
        details.getOrElse(selected) { "" },
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(10.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .height(170.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { i, label ->
            Column(
                Modifier
                    .width(40.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (i == selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent)
                    .clickable { selected = i },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Bar(income[i], maxValue, Brand.income)
                    Bar(expense[i], maxValue, Brand.expense)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun RowScope.Bar(value: Long, maxValue: Long, color: Color) {
    if (value <= 0L) {
        Box(
            Modifier
                .width(10.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brand.track),
        )
    } else {
        val fraction = (value.toFloat() / maxValue.toFloat()).coerceIn(0.03f, 1f)
        Box(
            Modifier
                .width(10.dp)
                .fillMaxHeight(fraction)
                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.6f)))),
        )
    }
}

@Composable
private fun CategoryBar(name: String, amount: Long, share: Float, limit: Long) {
    val base = Brand.categoryColor(name)
    val ratio = if (limit > 0L) amount.toFloat() / limit.toFloat() else 0f
    val barColor = when {
        limit <= 0L -> base
        ratio > 1f -> Brand.expense
        ratio >= 0.8f -> Brand.warning
        else -> base
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                formatMoney(amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(6.dp))
        ProgressBar(progress = if (limit > 0L) ratio else share, color = barColor)
        if (limit > 0L) {
            Spacer(Modifier.height(4.dp))
            Text(
                if (ratio > 1f) "Limit oshdi: ${formatMoney(amount - limit)} ortiqcha (limit ${formatMoney(limit)})"
                else "Limitning ${(ratio * 100).toInt()}% ishlatildi (limit ${formatMoney(limit)})",
                style = MaterialTheme.typography.bodySmall,
                color = if (ratio >= 0.8f) barColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CushionMini(data: FinanceData, modifier: Modifier, onClick: () -> Unit) {
    val target = data.cushionTarget
    val progress = if (target > 0) (data.cushionSaved.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    AppCard(modifier = modifier, onClick = onClick, padding = 16.dp) {
        Icon(Icons.Rounded.Shield, contentDescription = null, tint = Brand.primary, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(10.dp))
        Text("Himoya yostig'i", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(
            if (target > 0) "${(progress * 100).toInt()}%" else "—",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        ProgressBar(progress = progress, color = Brand.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            if (target > 0) "maqsadga" else "Reja belgilanmagan",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DebtMini(data: FinanceData, modifier: Modifier, onClick: () -> Unit) {
    AppCard(modifier = modifier, onClick = onClick, padding = 16.dp) {
        Icon(Icons.Rounded.CreditCard, contentDescription = null, tint = Brand.primary, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(10.dp))
        Text("Men qarzdorman", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            formatMoney(data.debtOwed),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Brand.expense,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text("Menga qarzdor", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            formatMoney(data.debtLent),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Brand.income,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}


@Composable
private fun SavingsRateCard(data: FinanceData, onClick: () -> Unit) {
    val rates = (-5..0).map { data.savingsRate(Dates.monthStart(it), Dates.monthStart(it + 1)) }
    val current = rates.last()
    val color = savingsColor(current)
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Tejash darajasi",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (current == null) "—" else formatPercent(current.toDouble() * 100.0),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                Text(
                    if (current == null) "Bu oy daromad kiritilmagan" else "Daromaddan qolgan ulush. Yaxshi daraja: 20%+",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                Modifier.height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                rates.forEach { r ->
                    val h = if (r == null) 4f else (r.coerceIn(0f, 0.5f) / 0.5f * 52f).coerceAtLeast(4f)
                    Box(
                        Modifier
                            .width(10.dp)
                            .height(h.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(savingsColor(r).copy(alpha = if (r == null) 0.25f else 1f)),
                    )
                }
            }
        }
    }
}

@Composable
fun savingsColor(rate: Float?): Color = when {
    rate == null -> Brand.muted
    rate >= 0.20f -> Brand.income
    rate >= 0.10f -> Brand.warning
    else -> Brand.expense
}


@Composable
private fun WelcomeStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Brand.primary)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun NavRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Brand.primary, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
