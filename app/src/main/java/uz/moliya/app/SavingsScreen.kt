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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** "Jamg'arma" yorlig'i: Himoya yostig'i va Maqsadlar o'rtasida almashadi. */
@Composable
fun SavingsHost(
    data: FinanceData,
    onPlan: (Long, Int) -> Unit,
    onAddCushion: (CushionEntry) -> Unit,
    onDeleteCushion: (CushionEntry) -> Unit,
    onAddGoal: (Goal) -> Unit,
    onDeleteGoal: (Goal) -> Unit,
    onAddGoalEntry: (Long, CushionEntry) -> Unit,
    onDeleteGoalEntry: (Long, CushionEntry) -> Unit,
) {
    var section by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            ScreenHeader("Jamg'arma", "Zaxira va shaxsiy maqsadlar")
            SegmentedToggle(
                options = listOf("Himoya yostig'i", "Maqsadlar"),
                selected = section,
                onSelect = { section = it },
            )
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.weight(1f)) {
            if (section == 0) {
                CushionScreen(data = data, onPlan = onPlan, onAdd = onAddCushion, onDelete = onDeleteCushion)
            } else {
                GoalsScreen(
                    data = data,
                    onAddGoal = onAddGoal,
                    onDeleteGoal = onDeleteGoal,
                    onAddEntry = onAddGoalEntry,
                    onDeleteEntry = onDeleteGoalEntry,
                )
            }
        }
    }
}

@Composable
fun GoalsScreen(
    data: FinanceData,
    onAddGoal: (Goal) -> Unit,
    onDeleteGoal: (Goal) -> Unit,
    onAddEntry: (Long, CushionEntry) -> Unit,
    onDeleteEntry: (Long, CushionEntry) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var entryFor by remember { mutableStateOf<Pair<Long, Boolean>?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp + LocalBottomInset.current),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            BigButton(
                text = "Yangi maqsad",
                icon = Icons.Rounded.Add,
                onClick = { showAdd = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (data.goals.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Flag,
                    title = "Maqsadlar yo'q",
                    text = "Telefon, safar, ta'lim kabi orzularingiz uchun maqsad qo'ying va oz-ozdan jamg'aring.",
                )
            }
        } else {
            items(data.goals, key = { it.id }) { goal ->
                GoalCard(
                    modifier = Modifier.animateItem(),
                    goal = goal,
                    expanded = expandedId == goal.id,
                    onToggle = { expandedId = if (expandedId == goal.id) null else goal.id },
                    onDeposit = { entryFor = goal.id to true },
                    onTake = { entryFor = goal.id to false },
                    onDelete = { onDeleteGoal(goal) },
                    onDeleteEntry = { onDeleteEntry(goal.id, it) },
                )
            }
        }
    }

    if (showAdd) {
        AddGoalSheet(onDismiss = { showAdd = false }, onSave = onAddGoal)
    }

    val ef = entryFor
    if (ef != null) {
        val goal = data.goals.firstOrNull { it.id == ef.first }
        if (goal != null) {
            AmountEntrySheet(
                deposit = ef.second,
                available = goal.saved,
                titleAdd = "Maqsadga qo'shish",
                titleTake = "Maqsaddan yechish",
                availableLabel = "Jamg'arilgan",
                onDismiss = { entryFor = null },
                onSave = { onAddEntry(goal.id, it) },
            )
        }
    }
}

@Composable
private fun GoalCard(
    modifier: Modifier = Modifier,
    goal: Goal,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDeposit: () -> Unit,
    onTake: () -> Unit,
    onDelete: () -> Unit,
    onDeleteEntry: (CushionEntry) -> Unit,
) {
    val done = goal.target > 0 && goal.saved >= goal.target
    val now = Dates.today()
    val overdue = !done && goal.deadline in 1 until now
    val statusColor = when {
        done -> Brand.income
        overdue -> Brand.expense
        else -> Brand.primary
    }
    val perMonth = if (!done && goal.deadline > now) goal.remaining / Dates.monthsLeft(goal.deadline) else 0L

    AppCard(modifier = modifier, onClick = onToggle) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${formatMoney(goal.saved)} / ${formatMoney(goal.target)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(
                when {
                    done -> "Erishildi"
                    overdue -> "Muddat o'tdi"
                    else -> "${(goal.progress * 100).toInt()}%"
                },
                statusColor,
            )
        }
        Spacer(Modifier.height(12.dp))
        ProgressBar(progress = goal.progress, color = statusColor)

        if (!done && goal.deadline > 0L) {
            Spacer(Modifier.height(8.dp))
            Text(
                if (overdue) "Muddat: ${Dates.format(goal.deadline)} · qoldi ${formatMoney(goal.remaining)}"
                else "Muddat: ${Dates.format(goal.deadline)} · oyiga ${formatMoney(perMonth)} kerak",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (expanded) {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigButton(
                    text = "Qo'shish",
                    icon = Icons.Rounded.Add,
                    onClick = onDeposit,
                    modifier = Modifier.weight(1f),
                )
                OutlineBigButton(
                    text = "Yechish",
                    icon = Icons.Rounded.Remove,
                    color = Brand.expense,
                    onClick = onTake,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            val history = goal.entries.sortedWith(compareByDescending<CushionEntry> { it.date }.thenByDescending { it.id })
            if (history.isEmpty()) {
                Text(
                    "Hali yozuv yo'q.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                history.forEach { e ->
                    val dep = e.amount >= 0
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (dep) "Qo'shildi" else "Yechildi",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                if (e.note.isBlank()) Dates.format(e.date) else "${Dates.format(e.date)} · ${e.note}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            (if (dep) "+" else "−") + formatMoney(Math.abs(e.amount)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (dep) Brand.income else Brand.expense,
                        )
                        IconButton(onClick = { onDeleteEntry(e) }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Yozuvni o'chirish",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            OutlineBigButton(
                text = "Maqsadni o'chirish",
                icon = Icons.Rounded.Delete,
                color = Brand.expense,
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AddGoalSheet(onDismiss: () -> Unit, onSave: (Goal) -> Unit) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var hasDeadline by remember { mutableStateOf(false) }
    var deadline by remember { mutableStateOf(Dates.today()) }
    val value = parseMoney(target)

    FormSheet(title = "Yangi maqsad", onDismiss = onDismiss) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Maqsad nomi (masalan: Yangi telefon)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        MoneyField(value = target, onChange = { target = it }, label = "Maqsad summasi")
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { hasDeadline = !hasDeadline },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Muddat belgilash", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = hasDeadline, onCheckedChange = { hasDeadline = it })
        }
        if (hasDeadline) DateField(millis = deadline, onChange = { deadline = it })
        BigButton(
            text = "Saqlash",
            enabled = name.isNotBlank() && value > 0,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(
                    Goal(
                        id = Ids.next(),
                        name = name.trim(),
                        target = value,
                        deadline = if (hasDeadline) deadline else 0L,
                        entries = emptyList(),
                    ),
                )
                onDismiss()
            },
        )
    }
}
