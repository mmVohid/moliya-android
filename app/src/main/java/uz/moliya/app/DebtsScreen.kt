package uz.moliya.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.rounded.Call
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import android.content.Intent
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
import androidx.compose.material.icons.rounded.Route
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun DebtsScreen(
    data: FinanceData,
    quickAdd: Boolean,
    onQuickAddConsumed: () -> Unit,
    onAddDebt: (Debt) -> Unit,
    onDeleteDebt: (Debt) -> Unit,
    onPay: (Long, DebtPayment) -> Unit,
    onDeletePayment: (Long, DebtPayment) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    LaunchedEffect(quickAdd) {
        if (quickAdd) {
            showAdd = true
            onQuickAddConsumed()
        }
    }
    var showStrategy by remember { mutableStateOf(false) }
    var payFor by remember { mutableStateOf<Long?>(null) }
    var deleteTarget by remember { mutableStateOf<Debt?>(null) }
    var expandedId by remember { mutableStateOf<Long?>(null) }
    // 0 = hammasi, 1 = olingan, 2 = berilgan
    var filter by rememberSaveable { mutableStateOf(0) }

    val owed = data.debtOwed
    val lent = data.debtLent
    val net = lent - owed

    val shown = data.debts
        .filter {
            when (filter) {
                1 -> !it.isLent
                2 -> it.isLent
                else -> true
            }
        }
        .sortedWith(
            compareBy<Debt> { it.remaining == 0L }
                .thenBy { it.dueDate.takeIf { d -> d > 0L } ?: Long.MAX_VALUE },
        )

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 112.dp + LocalBottomInset.current),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ScreenHeader("Qarzlar nazorati", "Olgan va bergan qarzlaringizni yozib boring") }

            if (data.debts.count { !it.isLent && it.remaining > 0 } >= 2) {
                item {
                    OutlineBigButton(
                        text = "Qarzni yopish rejasi",
                        icon = Icons.Rounded.Route,
                        onClick = { showStrategy = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DebtTotalTile("Men qarzdorman", owed, Brand.expense, Modifier.weight(1f))
                    DebtTotalTile("Menga qarzdor", lent, Brand.income, Modifier.weight(1f))
                }
            }

            if (data.debts.isNotEmpty()) {
                item {
                    AppCard(padding = 16.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Sof holat",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                (if (net > 0) "+" else "") + formatMoney(net),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (net >= 0) Brand.income else Brand.expense,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Menga qaytishi kerak bo'lgan pul minus men qaytarishim kerak bo'lgan pul.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    SegmentedToggle(
                        options = listOf("Hammasi", "Olingan", "Berilgan"),
                        selected = filter,
                        onSelect = { filter = it },
                    )
                }
            }

            if (shown.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.CreditCard,
                        title = if (data.debts.isEmpty()) "Qarzlar yo'q" else "Bu bo'limda qarz yo'q",
                        text = "Olgan yoki bergan qarzingizni kiriting va qaytarilishini kuzating.",
                        actionText = "Qarz qo'shish",
                        onAction = { showAdd = true },
                    )
                }
            } else {
                items(shown, key = { it.id }) { debt ->
                    DebtCard(
                        debt = debt,
                        expanded = expandedId == debt.id,
                        onToggle = { expandedId = if (expandedId == debt.id) null else debt.id },
                        onPay = { payFor = debt.id },
                        onDelete = { deleteTarget = debt },
                        onDeletePayment = { payment -> onDeletePayment(debt.id, payment) },
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 20.dp + LocalBottomInset.current),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Qarz qo'shish", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
        )
    }

    if (showAdd) {
        AddDebtSheet(
            startAsLent = filter == 2,
            onDismiss = { showAdd = false },
            onSave = onAddDebt,
        )
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

    if (showStrategy) DebtStrategySheet(debts = data.debts, onDismiss = { showStrategy = false })

    val del = deleteTarget
    if (del != null) {
        ConfirmDialog(
            title = "Qarzni o'chirish",
            text = "\"${del.fullName}\" va uning barcha to'lov tarixi o'chiriladi. Davom etasizmi?",
            confirmLabel = "O'chirish",
            onConfirm = {
                onDeleteDebt(del)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun DebtTotalTile(label: String, value: Long, color: Color, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier, padding = 16.dp) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
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
private fun DebtCard(
    debt: Debt,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPay: () -> Unit,
    onDelete: () -> Unit,
    onDeletePayment: (DebtPayment) -> Unit,
) {
    val closed = debt.remaining == 0L
    val overdue = !closed && debt.dueDate > 0L && debt.dueDate < System.currentTimeMillis()
    val typeColor = if (debt.isLent) Brand.income else Brand.expense
    val paidWord = if (debt.isLent) "Qaytarilgan" else "To'langan"

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    debt.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (debt.phone.isNotBlank()) {
                    val ctx = LocalContext.current
                    Row(
                        Modifier
                            .heightIn(min = 32.dp)
                            .clickable {
                                ctx.startActivity(
                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + debt.phone.filter { it.isDigit() || it == '+' }))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Call, contentDescription = "Qo'ng'iroq qilish", tint = Brand.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(debt.phone, style = MaterialTheme.typography.bodyMedium, color = Brand.primary)
                    }
                }
                Spacer(Modifier.height(6.dp))
                StatusPill(
                    if (debt.isLent) "Berilgan qarz" else "Olingan qarz",
                    typeColor,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Qarzni o'chirish",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (debt.isLent) "Sizga qaytarilishi kerak" else "Siz qaytarishingiz kerak",
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
                "$paidWord: ${formatMoney(debt.paid)}",
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
            if (closed) StatusPill(if (debt.isLent) "Qaytarilgan" else "To'langan", Brand.income)
            if (overdue) StatusPill("Muddati o'tgan", Brand.expense)
            if (!closed && !overdue && debt.dueDate > 0L) {
                StatusPill("Muddat: ${Dates.format(debt.dueDate)}", Brand.muted)
            }
            if (debt.interest > 0.0) StatusPill("Foiz: ${formatPercent(debt.interest)}", Brand.warning)
        }

        if (!closed) {
            Spacer(Modifier.height(16.dp))
            BigButton(
                text = if (debt.isLent) "Qaytgan pulni qo'shish" else "To'lov qo'shish",
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
                Text(
                    (if (debt.isLent) "Qaytarishlar tarixi" else "To'lovlar tarixi") + " (${debt.payments.size})",
                    fontWeight = FontWeight.SemiBold,
                )
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
                        IconButton(onClick = { onDeletePayment(payment) }, modifier = Modifier.size(44.dp)) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Yozuvni o'chirish",
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
private fun AddDebtSheet(startAsLent: Boolean, onDismiss: () -> Unit, onSave: (Debt) -> Unit) {
    var isLent by remember { mutableStateOf(startAsLent) }
    var name by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var hasDue by remember { mutableStateOf(false) }
    var due by remember { mutableStateOf(Dates.today()) }

    val totalValue = parseMoney(total)
    val canSave = name.isNotBlank() && totalValue > 0

    FormSheet(title = "Yangi qarz", onDismiss = onDismiss) {
        SegmentedToggle(
            options = listOf("Olgan qarzim", "Bergan qarzim"),
            selected = if (isLent) 1 else 0,
            onSelect = { isLent = it == 1 },
        )
        // 1. Ism
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(30) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (isLent) "Ismi (kimga bergansiz?)" else "Ismi (kimdan olgansiz?)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            shape = RoundedCornerShape(14.dp),
        )
        // 2. Qarz summasi
        MoneyField(value = total, onChange = { total = it }, label = "Qarz summasi")
        // 3. Telefon raqami
        OutlinedTextField(
            value = phone,
            onValueChange = { raw -> phone = raw.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' || ch == '-' }.take(18) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Telefon raqami") },
            placeholder = { Text("+998 90 123 45 67") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            shape = RoundedCornerShape(14.dp),
        )
        // 4. Qaytarish muddati
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable { hasDue = !hasDue },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Qaytarish muddati bor",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(checked = hasDue, onCheckedChange = { hasDue = it })
        }
        if (hasDue) {
            DateField(millis = due, onChange = { due = it }, label = "Muddat")
        }
        // 5. Saqlash
        BigButton(
            text = "Saqlash",
            enabled = canSave,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(
                    Debt(
                        id = Ids.next(),
                        name = name.trim(),
                        total = totalValue,
                        interest = 0.0,
                        dueDate = if (hasDue) due else 0L,
                        payments = emptyList(),
                        isLent = isLent,
                        phone = phone.trim(),
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

    FormSheet(
        title = if (debt.isLent) "Qaytgan pulni qo'shish" else "To'lov qo'shish",
        onDismiss = onDismiss,
    ) {
        Text(
            "${debt.fullName}. Qoldiq: ${formatMoney(debt.remaining)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MoneyField(
            value = amount,
            onChange = { amount = it },
            label = if (debt.isLent) "Qaytgan summa" else "To'lov summasi",
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
