package uz.moliya.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun CashFlowScreen(
    data: FinanceData,
    onAdd: (Txn) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var offset by rememberSaveable { mutableStateOf(0) }
    var filter by rememberSaveable { mutableStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Txn?>(null) }

    val start = Dates.monthStart(offset)
    val end = Dates.monthStart(offset + 1)
    val income = data.income(start, end)
    val expense = data.expense(start, end)

    val shown = data.txns
        .filter { it.date >= start && it.date < end }
        .filter {
            when (filter) {
                1 -> it.isIncome
                2 -> !it.isIncome
                else -> true
            }
        }
        .sortedWith(compareByDescending<Txn> { it.date }.thenByDescending { it.id })

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ScreenHeader("Daromad / Xarajat", "Pul oqimini kuzatib boring") }

            item {
                MonthSwitcher(
                    label = Dates.monthLabel(offset),
                    onPrev = { offset -= 1 },
                    onNext = { if (offset < 0) offset += 1 },
                    nextEnabled = offset < 0,
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryTile("Daromad", income, Brand.income, Icons.Rounded.TrendingUp, Modifier.weight(1f))
                    SummaryTile("Xarajat", expense, Brand.expense, Icons.Rounded.TrendingDown, Modifier.weight(1f))
                }
            }

            item {
                SegmentedToggle(
                    options = listOf("Hammasi", "Daromad", "Xarajat"),
                    selected = filter,
                    onSelect = { filter = it },
                )
            }

            if (shown.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.SwapVert,
                        title = "Yozuvlar yo'q",
                        text = "Pastdagi \"Qo'shish\" tugmasi orqali birinchi daromad yoki xarajatni kiriting.",
                    )
                }
            } else {
                items(shown, key = { it.id }) { txn ->
                    TxnRow(txn = txn, onDelete = { toDelete = txn })
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Qo'shish", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
        )
    }

    if (showAdd) {
        AddTxnSheet(onDismiss = { showAdd = false }, onSave = onAdd)
    }

    val target = toDelete
    if (target != null) {
        ConfirmDialog(
            title = "Yozuvni o'chirish",
            text = "${target.category}: ${formatMoney(target.amount)} yozuvi o'chiriladi. Davom etasizmi?",
            confirmLabel = "O'chirish",
            onConfirm = {
                onDelete(target.id)
                toDelete = null
            },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: Long,
    color: androidx.compose.ui.graphics.Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier, padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            formatMoney(value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TxnRow(txn: Txn, onDelete: () -> Unit) {
    val color = Brand.categoryColor(txn.category)
    AppCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LetterBadge(txn.category, color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    txn.category,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (txn.note.isBlank()) Dates.format(txn.date) else "${Dates.format(txn.date)} · ${txn.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                (if (txn.isIncome) "+" else "−") + formatMoney(txn.amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (txn.isIncome) Brand.income else Brand.expense,
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

@Composable
private fun AddTxnSheet(onDismiss: () -> Unit, onSave: (Txn) -> Unit) {
    var isIncome by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Dates.today()) }

    val categories = if (isIncome) Categories.income else Categories.expense
    val canSave = parseMoney(amount) > 0 && category.isNotEmpty()

    FormSheet(title = "Yangi yozuv", onDismiss = onDismiss) {
        SegmentedToggle(
            options = listOf("Xarajat", "Daromad"),
            selected = if (isIncome) 1 else 0,
            onSelect = {
                isIncome = it == 1
                category = ""
            },
        )
        MoneyField(value = amount, onChange = { amount = it }, label = "Summa")
        Text(
            "Kategoriya",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        CategoryChips(categories = categories, selected = category, onSelect = { category = it })
        NoteField(value = note, onChange = { note = it })
        DateField(millis = date, onChange = { date = it })
        BigButton(
            text = "Saqlash",
            enabled = canSave,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(
                    Txn(
                        id = Ids.next(),
                        isIncome = isIncome,
                        amount = parseMoney(amount),
                        category = category,
                        note = note.trim(),
                        date = date,
                    ),
                )
                onDismiss()
            },
        )
    }
}
