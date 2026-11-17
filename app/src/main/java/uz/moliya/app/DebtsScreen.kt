package uz.moliya.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun DebtsScreen(
    data: FinanceData,
    onAddDebt: (Debt) -> Unit,
    onDeleteDebt: (Long) -> Unit,
    onPay: (Long, DebtPayment) -> Unit,
    onDeletePayment: (Long, Long) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var payFor by remember { mutableStateOf<Long?>(null) }
    var deleteTarget by remember { mutableStateOf<Debt?>(null) }
    var expandedId by remember { mutableStateOf<Long?>(null) }

    val totalAll = data.debts.sumOf { it.total }
    val paidAll = data.debts.sumOf { it.paid }
    val remainingAll = data.debtRemaining
    val overall = if (totalAll > 0) (paidAll.toFloat() / totalAll.toFloat()).coerceIn(0f, 1f) else 0f

    // Faol qarzlar yuqorida, yopilganlar pastda
    val sorted = data.debts.sortedWith(compareBy<Debt> { it.remaining == 0L }.thenBy { it.dueDate.takeIf { d -> d > 0L } ?: Long.MAX_VALUE })

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ScreenHeader("Qarzlar nazorati", "Qarzlaringizni to'lab borishni kuzating") }

            if (data.debts.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.CreditCard,
                        title = "Qarzlar yo'q",
                        text = "Qarzingiz bo'lsa, \"Qarz qo'shish\" tugmasi orqali kiriting va to'lovlarni kuzating.",
                    )
                }
            } else {
                item {
                    AppCard {
                        Text(
                            "Umumiy qoldiq",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatMoney(remainingAll),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingAll > 0) Brand.expense else Brand.income,
                        )
                        Spacer(Modifier.height(14.dp))
                        ProgressBar(progress = overall, color = Brand.primary)
                        Spacer(Modifier.height(10.dp))
                        Row {
                            Text(
                                "To'langan: ${formatMoney(paidAll)}",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "${(overall * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                items(sorted, key = { it.id }) { debt ->
                    DebtCard(
                        debt = debt,
                        expanded = expandedId == debt.id,
                        onToggle = { expandedId = if (expandedId == debt.id) null else debt.id },
                        onPay = { payFor = debt.id },
                        onDelete = { deleteTarget = debt },
                        onDeletePayment = { paymentId -> onDeletePayment(debt.id, paymentId) },
                    )
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
            text = { Text("Qarz qo'shish", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
        )
    }

    if (showAdd) {
        AddDebtSheet(onDismiss = { showAdd = false }, onSave = onAddDebt)
    }

    val payId = payFor
    val payDebt = data.debts.firstOrNull { it.id == payId }
    if (payDebt != null) {
        PaymentSheet(
            debt = payDebt,
            onDismiss = { payFor = null },
            onSave = { payment -> onPay(payDebt.id, payment) },
        )
    }

    val del = deleteTarget
    if (del != null) {
        ConfirmDialog(
            title = "Qarzni o'chirish",
            text = "\"${del.name}\" va uning barcha to'lov tarixi o'chiriladi. Davom etasizmi?",
            confirmLabel = "O'chirish",
            onConfirm = {
                onDeleteDebt(del.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun DebtCard(
    debt: Debt,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPay: () -> Unit,
    onDelete: () -> Unit,
    onDeletePayment: (Long) -> Unit,
) {
    val closed = debt.remaining == 0L
    val overdue = !closed && debt.dueDate > 0L && debt.dueDate < System.currentTimeMillis()

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                debt.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Qarzni o'chirish",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Qoldiq",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            formatMoney(debt.remaining),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (closed) Brand.income else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        ProgressBar(progress = debt.progress, color = if (closed) Brand.income else Brand.primary)
        Spacer(Modifier.height(8.dp))
        Row {
            Text(
                "To'langan: ${formatMoney(debt.paid)}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Jami: ${formatMoney(debt.total)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (closed) StatusPill("To'langan", Brand.income)
            if (overdue) StatusPill("Muddati o'tgan", Brand.expense)
            if (!closed && !overdue && debt.dueDate > 0L) {
                StatusPill("Muddat: ${Dates.format(debt.dueDate)}", Brand.muted)
            }
            if (debt.interest > 0.0) StatusPill("Foiz: ${formatPercent(debt.interest)}", Brand.warning)
        }

        if (!closed) {
            Spacer(Modifier.height(16.dp))
            BigButton(
                text = "To'lov qo'shish",
                icon = Icons.Rounded.Add,
                onClick = onPay,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (debt.payments.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text("To'lovlar tarixi (${debt.payments.size})", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (expanded) {
                debt.payments.sortedByDescending { it.date }.forEach { payment ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            Dates.format(payment.date),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            formatMoney(payment.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Brand.income,
                        )
                        IconButton(onClick = { onDeletePayment(payment.id) }, modifier = Modifier.size(44.dp)) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "To'lovni o'chirish",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddDebtSheet(onDismiss: () -> Unit, onSave: (Debt) -> Unit) {
    var name by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var alreadyPaid by remember { mutableStateOf("") }
    var interest by remember { mutableStateOf("") }
    var hasDue by remember { mutableStateOf(false) }
    var due by remember { mutableStateOf(Dates.today()) }

    val totalValue = parseMoney(total)
    val paidValue = parseMoney(alreadyPaid)
    val paidTooMuch = paidValue > totalValue && alreadyPaid.isNotEmpty()
    val canSave = name.isNotBlank() && totalValue > 0 && !paidTooMuch

    FormSheet(title = "Yangi qarz", onDismiss = onDismiss) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Qarz nomi (masalan: Bank krediti)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            shape = RoundedCornerShape(14.dp),
        )
        MoneyField(value = total, onChange = { total = it }, label = "Umumiy qarz summasi")
        MoneyField(
            value = alreadyPaid,
            onChange = { alreadyPaid = it },
            label = "Allaqachon to'langan (ixtiyoriy)",
            isError = paidTooMuch,
            errorText = "Umumiy summadan ko'p bo'lmasligi kerak",
        )
        OutlinedTextField(
            value = interest,
            onValueChange = { raw -> interest = raw.filter { ch -> ch in '0'..'9' || ch == '.' || ch == ',' }.take(6) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Yillik foiz (ixtiyoriy)") },
            suffix = { Text("%") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            shape = RoundedCornerShape(14.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable { hasDue = !hasDue },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "To'lov muddati bor",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(checked = hasDue, onCheckedChange = { hasDue = it })
        }
        if (hasDue) {
            DateField(millis = due, onChange = { due = it }, label = "Muddat")
        }
        BigButton(
            text = "Saqlash",
            enabled = canSave,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val payments = if (paidValue > 0) {
                    listOf(DebtPayment(id = Ids.next(), amount = paidValue, date = Dates.today()))
                } else {
                    emptyList()
                }
                onSave(
                    Debt(
                        id = Ids.next(),
                        name = name.trim(),
                        total = totalValue,
                        interest = parseDecimal(interest),
                        dueDate = if (hasDue) due else 0L,
                        payments = payments,
                    ),
                )
                onDismiss()
            },
        )
    }
}

@Composable
private fun PaymentSheet(debt: Debt, onDismiss: () -> Unit, onSave: (DebtPayment) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Dates.today()) }
    val value = parseMoney(amount)
    val tooMuch = value > debt.remaining

    FormSheet(title = "To'lov qo'shish", onDismiss = onDismiss) {
        Text(
            "${debt.name}. Qoldiq: ${formatMoney(debt.remaining)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MoneyField(
            value = amount,
            onChange = { amount = it },
            label = "To'lov summasi",
            isError = tooMuch,
            errorText = "Qoldiqdan ko'p bo'lmasligi kerak",
        )
        DateField(millis = date, onChange = { date = it })
        BigButton(
            text = "Saqlash",
            enabled = value > 0 && !tooMuch,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(DebtPayment(id = Ids.next(), amount = value, date = date))
                onDismiss()
            },
        )
    }
}
