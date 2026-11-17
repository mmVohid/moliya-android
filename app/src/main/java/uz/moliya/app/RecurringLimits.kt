package uz.moliya.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun RecurringSheet(
    items: List<Recurring>,
    expenseCats: List<String>,
    incomeCats: List<String>,
    onAdd: (Recurring) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var adding by remember { mutableStateOf(items.isEmpty()) }

    FormSheet(title = if (adding) "Yangi takroriy yozuv" else "Takroriy yozuvlar", onDismiss = onDismiss) {
        if (adding) {
            AddRecurringForm(
                expenseCats = expenseCats,
                incomeCats = incomeCats,
                onSave = { r ->
                    onAdd(r)
                    adding = false
                },
                onCancel = { if (items.isEmpty()) onDismiss() else adding = false },
            )
        } else {
            Text(
                "Maosh, ijara, kredit to'lovi kabi har oy takrorlanadigan yozuvlar belgilangan sanada o'zi qo'shiladi. " +
                    "Takroriy yozuvni o'chirish avval qo'shilgan yozuvlarni o'chirmaydi.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val yearlyOut = items.filter { !it.isIncome }.sumOf { it.amount } * 12
            val yearlyIn = items.filter { it.isIncome }.sumOf { it.amount } * 12
            if (yearlyOut > 0 || yearlyIn > 0) {
                Text(
                    buildString {
                        if (yearlyOut > 0) append("Yiliga takroriy xarajat: ${formatMoney(yearlyOut)}")
                        if (yearlyOut > 0 && yearlyIn > 0) append("\n")
                        if (yearlyIn > 0) append("Yiliga takroriy daromad: ${formatMoney(yearlyIn)}")
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items.forEach { r -> RecurringRow(r, onDelete = { onDelete(r.id) }) }
            BigButton(
                text = "Yangi takroriy yozuv",
                icon = Icons.Rounded.Add,
                onClick = { adding = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RecurringRow(r: Recurring, onDelete: () -> Unit) {
    val color = if (r.isIncome) Brand.income else Brand.expense
    AppCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LetterBadge(r.category, Brand.categoryColor(r.category))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (r.note.isBlank()) r.category else "${r.category} · ${r.note}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Har oy ${r.anchorDay}-kuni · keyingisi ${Dates.format(r.nextDue)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                (if (r.isIncome) "+" else "−") + formatMoney(r.amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Takroriy yozuvni o'chirish",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AddRecurringForm(expenseCats: List<String>, incomeCats: List<String>, onSave: (Recurring) -> Unit, onCancel: () -> Unit) {
    var isIncome by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Dates.today()) }

    val categories = if (isIncome) incomeCats else expenseCats
    val canSave = parseMoney(amount) > 0 && category.isNotEmpty()

    SegmentedToggle(
        options = listOf("Xarajat", "Daromad"),
        selected = if (isIncome) 1 else 0,
        onSelect = {
            isIncome = it == 1
            category = ""
        },
    )
    MoneyField(value = amount, onChange = { amount = it }, label = "Summa")
    Text("Kategoriya", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    CategoryChips(categories = categories, selected = category, onSelect = { category = it })
    NoteField(value = note, onChange = { note = it }, label = "Nomi / izoh (masalan: Ijara)")
    DateField(millis = date, onChange = { date = it }, label = "Birinchi sana")
    Text(
        "Shu sanadan boshlab har oy shu kunda yozuv qo'shiladi. O'tgan sana tanlasangiz, o'sha sanadan hozirgacha bo'lgan yozuvlar ham qo'shiladi.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlineBigButton(text = "Bekor qilish", onClick = onCancel, modifier = Modifier.weight(1f))
        BigButton(
            text = "Saqlash",
            enabled = canSave,
            modifier = Modifier.weight(1f),
            onClick = {
                onSave(
                    Recurring(
                        id = Ids.next(),
                        isIncome = isIncome,
                        amount = parseMoney(amount),
                        category = category,
                        note = note.trim(),
                        anchorDay = Dates.dayOfMonth(date),
                        nextDue = date,
                    ),
                )
            },
        )
    }
}

@Composable
fun LimitsSheet(
    limits: Map<String, Long>,
    categories: List<String>,
    onSave: (Map<String, Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    val fields = remember {
        mutableStateMapOf<String, String>().apply {
            categories.forEach { c -> put(c, limits[c]?.toString() ?: "") }
        }
    }

    FormSheet(title = "Oylik limitlar", onDismiss = onDismiss) {
        Text(
            "Kategoriya uchun oylik xarajat limitini qo'ying. Limit 80% dan oshsa Bosh sahifada ogohlantiramiz, oshib ketsa xabar beramiz. Bo'sh qoldirsangiz, limitsiz.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        categories.forEach { category ->
            MoneyField(
                value = fields[category] ?: "",
                onChange = { fields[category] = it },
                label = category,
            )
        }
        BigButton(
            text = "Saqlash",
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val result = LinkedHashMap<String, Long>()
                fields.forEach { (category, text) ->
                    val value = parseMoney(text)
                    if (value > 0L) result[category] = value
                }
                onSave(result)
                onDismiss()
            },
        )
    }
}
