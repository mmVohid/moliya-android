@file:OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package uz.moliya.app

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
    onAddCategory: (Boolean, String) -> Unit,
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
                            if (data.limits.isNotEmpty()) "Limit (${data.limits.size})" else "Limit",
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
                items(shown, key = { it.id }) { txn ->
                    TxnRow(
                        modifier = Modifier.animateItem(),
                        txn = txn,
                        onEdit = { editing = txn },
                        onDelete = { onDelete(txn) },
                    )
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
            onAddCategory = onAddCategory,
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
            onAddCategory = onAddCategory,
        )
    }

    if (showLimits) {
        LimitsSheet(
            limits = data.limits,
            categories = data.expenseCategories,
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
    val color = Brand.categoryColor(txn.category)
    val foreign = txn.currency != "UZS"
    val details = buildString {
        append(Dates.format(txn.date))
        if (foreign) append(" · ").append(formatForeign(txn.foreignAmount, txn.currency))
        if (txn.note.isNotBlank()) append(" · ").append(txn.note)
    }
    AppCard(modifier = modifier, padding = 12.dp, onClick = onEdit) {
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
    onAddCategory: (Boolean, String) -> Unit,
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
    val canSave = amountUzs > 0 && category.isNotEmpty()
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
                        category = category,
                        note = note.trim(),
                        date = date,
                        walletId = walletId,
                        receipt = receipt,
                    ),
                )
                if (initial == null && saveAsTemplate) {
                    onAddTemplate(
                        Template(
                            id = Ids.next(),
                            isIncome = isIncome,
                            amount = amountUzs,
                            category = category,
                            note = note.trim(),
                        ),
                    )
                }
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
            options = listOf("Xarajat", "Daromad"),
            selected = if (isIncome) 1 else 0,
            onSelect = {
                isIncome = it == 1
                category = ""
            },
        )

        if (initial == null && templates.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Shablonlar",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = { manageTemplates = !manageTemplates }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (manageTemplates) "Tayyor" else "Boshqarish")
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                templates.forEach { t ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            if (manageTemplates) {
                                onDeleteTemplate(t.id)
                            } else {
                                amount = t.amount.toString()
                                category = t.category
                                note = t.note
                            }
                        },
                        label = {
                            Text(
                                "${t.note.ifBlank { t.category }} · ${formatMoney(t.amount)}",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                        trailingIcon = if (manageTemplates) {
                            { Icon(Icons.Rounded.Close, contentDescription = "Shablonni o'chirish", modifier = Modifier.size(18.dp)) }
                        } else {
                            null
                        },
                        modifier = Modifier.heightIn(min = 40.dp),
                    )
                }
            }
        }

        // 1. Summa
        MoneyField(value = amount, onChange = { amount = it }, label = "Summa", focusRequester = amountFocus)

        // 2. Izoh (ixtiyoriy)
        NoteField(value = note, onChange = { note = it })

        // 3. Sana
        DateField(millis = date, onChange = { date = it })

        // 4. Chekni ilova qilish
        Text(
            "Chekni ilova qilish",
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

        // 5. Kategoriya (ro'yxat + Yangi kategoriya)
        var menuOpen by remember { mutableStateOf(false) }
        var showNewCategory by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = menuOpen, onExpandedChange = { menuOpen = it }) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                label = { Text("Kategoriya") },
                placeholder = { Text("Tanlang") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuOpen) },
                shape = RoundedCornerShape(14.dp),
            )
            ExposedDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                categories.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c, style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            category = c
                            menuOpen = false
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text("+ Yangi kategoriya", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    },
                    onClick = {
                        menuOpen = false
                        showNewCategory = true
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        if (showNewCategory) {
            var name by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showNewCategory = false },
                title = { Text("Yangi kategoriya") },
                text = {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(24) },
                        label = { Text("Kategoriya nomi") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = name.isNotBlank(),
                        onClick = {
                            onAddCategory(isIncome, name)
                            category = name.trim()
                            showNewCategory = false
                        },
                    ) { Text("Qo'shish") }
                },
                dismissButton = { TextButton(onClick = { showNewCategory = false }) { Text("Bekor qilish") } },
            )
        }

        if (data.wallets.isNotEmpty()) {
            Text("Hamyon", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = walletId == 0L, onClick = { walletId = 0L }, label = { Text("Belgilanmagan") }, modifier = Modifier.heightIn(min = 40.dp))
                data.wallets.forEach { w ->
                    FilterChip(selected = walletId == w.id, onClick = { walletId = w.id }, label = { Text(w.name) }, modifier = Modifier.heightIn(min = 40.dp))
                }
            }
        }

        if (initial == null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { saveAsTemplate = !saveAsTemplate },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Shablon sifatida saqlash",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(checked = saveAsTemplate, onCheckedChange = { saveAsTemplate = it })
            }
        }

    }
}
