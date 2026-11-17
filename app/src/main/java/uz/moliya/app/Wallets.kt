package uz.moliya.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun WalletsCard(
    data: FinanceData,
    hidden: Boolean,
    onAdd: (Wallet) -> Unit,
    onDelete: (Wallet) -> Unit,
    onTransfer: (Transfer) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Wallet?>(null) }

    AppCard {
        Text("Hamyonlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        if (data.wallets.isEmpty()) {
            Text(
                "Naqd, karta kabi hamyonlar yarating. Yozuv kiritayotganda hamyon tanlanadi, o'tkazmalar esa ular o'rtasida pulni ko'chiradi.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        data.wallets.forEach { w ->
            val balance = data.walletBalance(w)
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(w.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    if (hidden) "•••••••" else (if (balance < 0) "−" else "") + formatMoney(Math.abs(balance)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (balance >= 0) MaterialTheme.colorScheme.onSurface else Brand.expense,
                )
                IconButton(onClick = { toDelete = w }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Hamyonni o'chirish", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlineBigButton(text = "Hamyon", icon = Icons.Rounded.Add, onClick = { showAdd = true }, modifier = Modifier.weight(1f))
            if (data.wallets.size >= 2) {
                OutlineBigButton(text = "O'tkazma", icon = Icons.Rounded.SwapHoriz, onClick = { showTransfer = true }, modifier = Modifier.weight(1f))
            }
        }
    }

    if (showAdd) AddWalletSheet(onDismiss = { showAdd = false }, onSave = onAdd)
    if (showTransfer) TransferSheet(wallets = data.wallets, onDismiss = { showTransfer = false }, onSave = onTransfer)
    val del = toDelete
    if (del != null) {
        ConfirmDialog(
            title = "Hamyonni o'chirish",
            text = "\"${del.name}\" va u bilan bog'liq o'tkazmalar o'chiriladi. Yozuvlar saqlanadi, lekin hamyonsiz qoladi.",
            confirmLabel = "O'chirish",
            onConfirm = {
                onDelete(del)
                toDelete = null
            },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun AddWalletSheet(onDismiss: () -> Unit, onSave: (Wallet) -> Unit) {
    var name by remember { mutableStateOf("") }
    var opening by remember { mutableStateOf("") }
    FormSheet(title = "Yangi hamyon", onDismiss = onDismiss) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(24) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nomi (masalan: Naqd, Humo)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        MoneyField(value = opening, onChange = { opening = it }, label = "Hozirgi qoldiq (ixtiyoriy)")
        BigButton(
            text = "Saqlash",
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(Wallet(Ids.next(), name.trim(), parseMoney(opening)))
                onDismiss()
            },
        )
    }
}

@Composable
private fun TransferSheet(wallets: List<Wallet>, onDismiss: () -> Unit, onSave: (Transfer) -> Unit) {
    var from by remember { mutableStateOf(wallets.first().id) }
    var to by remember { mutableStateOf(wallets[1].id) }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Dates.today()) }
    val value = parseMoney(amount)
    FormSheet(title = "Hamyonlar o'rtasida o'tkazma", onDismiss = onDismiss) {
        Text("Qayerdan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            wallets.forEach { w -> FilterChip(selected = from == w.id, onClick = { from = w.id }, label = { Text(w.name) }) }
        }
        Text("Qayerga", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            wallets.forEach { w -> FilterChip(selected = to == w.id, onClick = { to = w.id }, label = { Text(w.name) }) }
        }
        MoneyField(value = amount, onChange = { amount = it }, label = "Summa")
        DateField(millis = date, onChange = { date = it })
        BigButton(
            text = "O'tkazish",
            enabled = value > 0 && from != to,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(Transfer(Ids.next(), from, to, value, date))
                onDismiss()
            },
        )
    }
}
