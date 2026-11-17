package uz.moliya.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun CushionScreen(
    data: FinanceData,
    onPlan: (Long, Int) -> Unit,
    onAdd: (CushionEntry) -> Unit,
    onDelete: (CushionEntry) -> Unit,
) {
    var showPlan by remember { mutableStateOf(false) }
    var entryMode by remember { mutableStateOf<Boolean?>(null) } // true = qo'shish, false = yechish

    val saved = data.cushionSaved
    val target = data.cushionTarget
    val need = data.monthlyNeed
    val hasPlan = need > 0
    val progress = if (target > 0) (saved.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    val monthsCovered = if (need > 0) saved.toDouble() / need.toDouble() else 0.0

    val status: Pair<String, Color> = when {
        !hasPlan -> "Reja belgilanmagan" to Brand.muted
        saved >= target -> "Maqsadga erishildi" to Brand.income
        monthsCovered >= 3.0 -> "Yaxshi holat" to Brand.primary
        monthsCovered >= 1.0 -> "Rivojlanmoqda" to Brand.warning
        else -> "Boshlang'ich bosqich" to Brand.expense
    }

    val pastExpenses = (-3..-1)
        .map { data.expense(Dates.monthStart(it), Dates.monthStart(it + 1)) }
        .filter { it > 0L }
    val suggestion = if (pastExpenses.isNotEmpty()) {
        pastExpenses.sum() / pastExpenses.size
    } else {
        data.expense(Dates.monthStart(0), Dates.monthStart(1))
    }

    val history = data.cushion.sortedWith(compareByDescending<CushionEntry> { it.date }.thenByDescending { it.id })

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp + LocalBottomInset.current),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            AppCard {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (hasPlan) {
                        ProgressRing(progress = progress, color = status.second) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "maqsadga",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            formatMoney(saved),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Maqsad: ${formatMoney(target)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        StatusPill(status.first, status.second)
                    } else {
                        Box(
                            Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Avval rejani belgilang",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Oylik zaruriy xarajatingiz va necha oylik zaxira kerakligini kiriting. Dastur maqsad summasini o'zi hisoblaydi.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(20.dp))
                        BigButton(
                            text = "Reja belgilash",
                            onClick = { showPlan = true },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        if (hasPlan) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BigButton(
                        text = "Qo'shish",
                        icon = Icons.Rounded.Add,
                        onClick = { entryMode = true },
                        modifier = Modifier.weight(1f),
                    )
                    OutlineBigButton(
                        text = "Yechish",
                        icon = Icons.Rounded.Remove,
                        color = Brand.expense,
                        onClick = { entryMode = false },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                AppCard {
                    InfoRow("Oylik zaruriy xarajat", formatMoney(need))
                    Spacer(Modifier.height(12.dp))
                    InfoRow("Maqsad muddati", "${data.targetMonths} oy")
                    Spacer(Modifier.height(12.dp))
                    InfoRow("Hozir qoplaydi", String.format(Locale.US, "%.1f oy", monthsCovered))
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { showPlan = true },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Rejani o'zgartirish", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item { SectionTitle("Tarix") }

            if (history.isEmpty()) {
                item {
                    Text(
                        "Hali yozuv yo'q. \"Qo'shish\" tugmasi bilan birinchi mablag'ni kiriting.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(history, key = { it.id }) { entry ->
                    EntryRow(entry = entry, onDelete = { onDelete(entry) })
                }
            }
        }
    }

    if (showPlan) {
        PlanSheet(
            currentNeed = need,
            currentMonths = data.targetMonths,
            suggestion = suggestion,
            onDismiss = { showPlan = false },
            onSave = onPlan,
        )
    }

    val mode = entryMode
    if (mode != null) {
        AmountEntrySheet(
            deposit = mode,
            available = saved,
            titleAdd = "Yostiqqa qo'shish",
            titleTake = "Yostiqdan yechish",
            availableLabel = "Yostiqdagi mablag'",
            onDismiss = { entryMode = null },
            onSave = onAdd,
        )
    }
}

@Composable
internal fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun EntryRow(entry: CushionEntry, onDelete: () -> Unit) {
    val deposit = entry.amount >= 0
    val color = if (deposit) Brand.income else Brand.expense
    AppCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (deposit) Icons.Rounded.Add else Icons.Rounded.Remove,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (deposit) "Qo'shildi" else "Yechildi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (entry.note.isBlank()) Dates.format(entry.date) else "${Dates.format(entry.date)} · ${entry.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                (if (deposit) "+" else "−") + formatMoney(Math.abs(entry.amount)),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "O'chirish",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanSheet(
    currentNeed: Long,
    currentMonths: Int,
    suggestion: Long,
    onDismiss: () -> Unit,
    onSave: (Long, Int) -> Unit,
) {
    var need by remember { mutableStateOf(if (currentNeed > 0) currentNeed.toString() else "") }
    var months by remember { mutableStateOf(currentMonths) }
    val needValue = parseMoney(need)

    FormSheet(title = "Yostiq rejasi", onDismiss = onDismiss) {
        Text(
            "Oylik zaruriy xarajat: ijara, oziq-ovqat, transport, aloqa kabi majburiy xarajatlar yig'indisi.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MoneyField(value = need, onChange = { need = it }, label = "Oylik zaruriy xarajat")
        if (suggestion > 0) {
            TextButton(
                onClick = { need = suggestion.toString() },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("O'rtacha xarajatdan olish: ${formatMoney(suggestion)}")
            }
        }
        Text(
            "Necha oylik zaxira kerak?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(3, 6, 9, 12).forEach { m ->
                FilterChip(
                    selected = months == m,
                    onClick = { months = m },
                    label = {
                        Text(
                            "$m oy",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                )
            }
        }
        if (needValue > 0) {
            Text(
                "Maqsad: ${formatMoney(needValue * months)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Brand.primary,
            )
        }
        BigButton(
            text = "Saqlash",
            enabled = needValue > 0,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(needValue, months)
                onDismiss()
            },
        )
    }
}

@Composable
internal fun AmountEntrySheet(
    deposit: Boolean,
    available: Long,
    titleAdd: String,
    titleTake: String,
    availableLabel: String,
    onDismiss: () -> Unit,
    onSave: (CushionEntry) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Dates.today()) }
    val value = parseMoney(amount)
    val tooMuch = !deposit && value > available

    FormSheet(
        title = if (deposit) titleAdd else titleTake,
        onDismiss = onDismiss,
    ) {
        if (!deposit) {
            Text(
                "$availableLabel: ${formatMoney(available)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MoneyField(
            value = amount,
            onChange = { amount = it },
            label = "Summa",
            isError = tooMuch,
            errorText = "Mavjud mablag'dan ko'p bo'lmasligi kerak",
        )
        NoteField(value = note, onChange = { note = it })
        DateField(millis = date, onChange = { date = it })
        BigButton(
            text = "Saqlash",
            enabled = value > 0 && !tooMuch,
            container = if (deposit) MaterialTheme.colorScheme.primary else Brand.expense,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(
                    CushionEntry(
                        id = Ids.next(),
                        amount = if (deposit) value else -value,
                        note = note.trim(),
                        date = date,
                    ),
                )
                onDismiss()
            },
        )
    }
}
