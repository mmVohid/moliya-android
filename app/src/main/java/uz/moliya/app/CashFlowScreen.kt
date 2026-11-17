@file:OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package uz.moliya.app

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import kotlinx.coroutines.delay
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun CashFlowScreen(
    data: FinanceData,
    quickAdd: Int,
    onQuickAddConsumed: () -> Unit,
    onAdd: (Txn) -> Unit,
    onUpdate: (Txn) -> Unit,
    onDelete: (Txn) -> Unit,
    onSaveLimits: (Map<String, Long>) -> Unit,
    onAddTemplate: (Template) -> Unit,
    onDeleteTemplate: (Long) -> Unit,
) {
    var showLimits by remember { mutableStateOf(false) }
    var offset by rememberSaveable { mutableStateOf(0) }
    var filter by rememberSaveable { mutableStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var addAsIncome by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Txn?>(null) }

    LaunchedEffect(quickAdd) {
        if (quickAdd != 0) {
            addAsIncome = quickAdd == 2
            showAdd = true
            onQuickAddConsumed()
        }
    }

    val start = Dates.monthStart(offset)
    val end = Dates.monthStart(offset + 1)
    val shown: List<Txn> = data.txns
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 112.dp + LocalBottomInset.current),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Box(Modifier.fillMaxWidth()) {
                    ScreenHeader("Daromad / Xarajat", "Pul oqimini kuzatib boring")
                    FilledTonalButton(
                        onClick = { showLimits = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 20.dp)
                            .heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if ((data.limits[""] ?: 0L) > 0L) "Limit: " + formatMoney(data.limits[""] ?: 0L) else "Oylik limit",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            run {
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
                        SummaryTile("Daromad", data.income(start, end), Brand.income, Icons.Rounded.TrendingUp, Modifier.weight(1f))
                        SummaryTile("Xarajat", data.expense(start, end), Brand.expense, Icons.Rounded.TrendingDown, Modifier.weight(1f))
                    }
                }

                item {
                    SegmentedToggle(
                        options = listOf("Hammasi", "Daromad", "Xarajat"),
                        selected = filter,
                        onSelect = { filter = it },
                    )
                }
            }

            if (shown.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.SwapVert,
                        title = "Yozuvlar yo'q",
                        text = "Birinchi daromad yoki xarajatni kiriting, qolganini ilova hisoblaydi.",
                        actionText = "Birinchi yozuvni qo'shish",
                        onAction = {
                            addAsIncome = false
                            showAdd = true
                        },
                    )
                }
            } else {
                val todayMs = Dates.today()
                shown.groupBy { it.date }.forEach { (day, list) ->
                    item(key = "day$day") {
                        val label = when (day) {
                            todayMs -> "Bugun"
                            todayMs - 24L * 60 * 60 * 1000 -> "Kecha"
                            else -> Dates.format(day)
                        }
                        val dayExpense = list.filter { !it.isIncome }.sumOf { it.amount }
                        val dayIncome = list.filter { it.isIncome }.sumOf { it.amount }
                        Row(Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (!data.hideBalance) {
                                Text(
                                    buildString {
                                        if (dayIncome > 0L) append("+").append(formatMoney(dayIncome))
                                        if (dayIncome > 0L && dayExpense > 0L) append("   ")
                                        if (dayExpense > 0L) append("−").append(formatMoney(dayExpense))
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    items(list, key = { it.id }) { txn ->
                        TxnRow(
                            modifier = Modifier.animateItem(),
                            txn = txn,
                            onEdit = { editing = txn },
                            onDelete = { onDelete(txn) },
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                addAsIncome = false
                showAdd = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 20.dp + LocalBottomInset.current),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Qo'shish", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
        )
    }

    if (showAdd) {
        TxnSheet(
            initial = null,
            startIncome = addAsIncome,
            data = data,
            onDismiss = { showAdd = false },
            onSave = onAdd,
            onAddTemplate = onAddTemplate,
            onDeleteTemplate = onDeleteTemplate,
        )
    }

    val toEdit = editing
    if (toEdit != null) {
        TxnSheet(
            initial = toEdit,
            startIncome = toEdit.isIncome,
            data = data,
            onDismiss = { editing = null },
            onSave = onUpdate,
            onAddTemplate = onAddTemplate,
            onDeleteTemplate = onDeleteTemplate,
        )
    }

    if (showLimits) {
        LimitsSheet(
            limits = data.limits,
            onSave = onSaveLimits,
            onDismiss = { showLimits = false },
        )
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: Long,
    color: Color,
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
            formatMoney(animatedLong(value)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TxnRow(txn: Txn, onEdit: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val color = if (txn.isIncome) Brand.income else Brand.expense
    val title = if (txn.note.isNotBlank()) txn.note else if (txn.isIncome) "Daromad" else "Xarajat"
    val foreign = txn.currency != "UZS"
    val details = buildString {
        append(Dates.format(txn.date))
        if (foreign) append(" · ").append(formatForeign(txn.foreignAmount, txn.currency))
    }
    AppCard(modifier = modifier, padding = 12.dp, onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LetterBadge(if (txn.isIncome) "+" else "−", color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    details,
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
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "O'chirish",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun filterDecimal(raw: String): String {
    var dot = false
    val sb = StringBuilder()
    for (ch in raw) {
        if (ch in '0'..'9') {
            sb.append(ch)
        } else if ((ch == '.' || ch == ',') && !dot) {
            dot = true
            sb.append('.')
        }
    }
    return sb.toString().take(12)
}

private fun trimNumber(v: Double): String =
    if (v % 1.0 == 0.0) String.format(Locale.US, "%.0f", v) else v.toString()

/** Shu valyutadagi eng so'nggi yozuvning kursi (yo'q bo'lsa bo'sh) */
private fun lastRate(data: FinanceData, currency: String): String {
    val last = data.txns
        .filter { it.currency == currency && it.rate > 0.0 }
        .maxWithOrNull(compareBy<Txn> { it.date }.thenBy { it.id })
    return if (last == null) "" else trimNumber(last.rate)
}

@Composable
private fun TxnSheet(
    initial: Txn?,
    startIncome: Boolean,
    data: FinanceData,
    onDismiss: () -> Unit,
    onSave: (Txn) -> Unit,
    onAddTemplate: (Template) -> Unit,
    onDeleteTemplate: (Long) -> Unit,
) {
    val context = LocalContext.current
    var isIncome by remember { mutableStateOf(initial?.isIncome ?: startIncome) }
    var amount by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: Dates.today()) }
    var saveAsTemplate by remember { mutableStateOf(false) }
    var walletId by remember { mutableStateOf(initial?.walletId ?: 0L) }
    var receipt by remember { mutableStateOf(initial?.receipt ?: "") }
    var cameraTarget by remember { mutableStateOf<Pair<java.io.File, android.net.Uri>?>(null) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) Receipts.save(context, uri)?.let { receipt = it }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val target = cameraTarget
        if (ok && target != null) Receipts.save(context, target.second)?.let { receipt = it }
        target?.first?.delete()
        cameraTarget = null
    }
    var manageTemplates by remember { mutableStateOf(false) }

    val categories = if (isIncome) data.incomeCategories else data.expenseCategories
    val amountUzs: Long = parseMoney(amount)
    val canSave = amountUzs > 0
    val finalCategory = category.ifEmpty { if (isIncome) "Boshqa" else "Boshqalar" }
    val templates = data.templates.filter { it.isIncome == isIncome }

    val saveButton: @Composable () -> Unit = {
        BigButton(
            text = "Saqlash",
            enabled = canSave,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(
                    Txn(
                        id = initial?.id ?: Ids.next(),
                        isIncome = isIncome,
                        amount = amountUzs,
                        category = finalCategory,
                        note = note.trim(),
                        date = date,
                        walletId = walletId,
                        receipt = receipt,
                    ),
                )
                onDismiss()
            },
        )
    }
    val amountFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (initial == null) {
            delay(350)
            try {
                amountFocus.requestFocus()
            } catch (e: IllegalStateException) {
                // maydon hali tayyor emas
            }
        }
    }

    FormSheet(
        title = if (initial == null) "Yangi yozuv" else "Yozuvni tahrirlash",
        onDismiss = onDismiss,
        footer = saveButton,
    ) {
        SegmentedToggle(
            options = listOf("Daromad", "Xarajat"),
            selected = if (isIncome) 0 else 1,
            onSelect = {
                isIncome = it == 0
                category = ""
            },
        )

        // 1. Summa
        MoneyField(value = amount, onChange = { amount = it }, label = "Summa", focusRequester = amountFocus)

        // Tez summa: bosilganda qo'shiladi
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(10_000L, 50_000L, 100_000L, 500_000L).forEach { step ->
                FilterChip(
                    selected = false,
                    onClick = { amount = (amountUzs + step).toString() },
                    label = { Text("+" + formatMoney(step).removeSuffix(" so'm")) },
                    modifier = Modifier.heightIn(min = 40.dp),
                )
            }
            if (amountUzs > 0L) {
                FilterChip(
                    selected = false,
                    onClick = { amount = "" },
                    label = { Text("Tozalash") },
                    modifier = Modifier.heightIn(min = 40.dp),
                )
            }
        }

        // 2. Izoh (ixtiyoriy)
        NoteField(value = note, onChange = { note = it })
        val recentNotes = remember(data.txns, isIncome) {
            data.txns.filter { it.isIncome == isIncome && it.note.isNotBlank() }
                .groupingBy { it.note }.eachCount()
                .entries.sortedByDescending { it.value }.take(5).map { it.key }
        }
        if (recentNotes.isNotEmpty() && note.isBlank()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                recentNotes.forEach { n ->
                    FilterChip(
                        selected = false,
                        onClick = { note = n },
                        label = { Text(n, maxLines = 1) },
                        modifier = Modifier.heightIn(min = 40.dp),
                    )
                }
            }
        }

        // 3. Sana
        DateField(millis = date, onChange = { date = it })

        // 4. Chekni ilova qilish
        Text(
            "Chekni ilova qilish (ixtiyoriy)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        val preview = remember(receipt) { Receipts.load(context, receipt) }
        if (preview != null) {
            androidx.compose.foundation.Image(
                bitmap = preview,
                contentDescription = "Chek rasmi",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlineBigButton(
                text = "Kamera",
                icon = Icons.Rounded.PhotoCamera,
                onClick = {
                    val target = Receipts.cameraTarget(context)
                    cameraTarget = target
                    camera.launch(target.second)
                },
                modifier = Modifier.weight(1f),
            )
            OutlineBigButton(
                text = "Galereya",
                icon = Icons.Rounded.Image,
                onClick = { gallery.launch("image/*") },
                modifier = Modifier.weight(1f),
            )
        }
        if (receipt.isNotEmpty()) {
            TextButton(onClick = { receipt = "" }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Rasmni olib tashlash") }
        }
    }
}
