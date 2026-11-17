@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package uz.moliya.app

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Widgets
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

// ───────────────────────── Ma'lumotlar va zaxira ─────────────────────────

@Composable
internal fun DataPage(vm: FinanceViewModel, onOpenBackup: () -> Unit, onBack: () -> Unit) {
    val s = vm.settings
    val context = LocalContext.current
    var confirmWipe by remember { mutableStateOf(false) }
    var typeWipe by remember { mutableStateOf(false) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { vm.refreshSettings() }
    val keeps = listOf(2, 4, 8, 12)
    val weekdays = listOf(0 to "Avto", 2 to "Du", 3 to "Se", 4 to "Ch", 5 to "Pa", 6 to "Ju", 7 to "Sh", 1 to "Ya")
    val data = vm.data
    val sizeKb = (java.io.File(context.filesDir, "moliya.json").length() / 1024L).coerceAtLeast(1L)
    val records = data.txns.size + data.cushion.size + data.debts.size

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                vm.setBackupFolder(uri.toString())
                vm.setAutoBackup(true)
            } catch (e: SecurityException) {
                // ruxsat olinmadi
            }
        }
    }

    Page("Ma'lumotlar va zaxira", onBack) {
        item {
            AppCard {
                Text("Holat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                StatusLine("Yozuvlar", "$records ta")
                StatusLine("Ma'lumot hajmi", "$sizeKb KB" + if (s.encryptData) " · shifrlangan" else "")
                StatusLine("Oxirgi zaxira", vm.lastBackupAt.takeIf { it > 0L }?.let { Dates.formatDateTime(it) } ?: "olinmagan")
            }
        }
        item {
            AppCard {
                SwitchRow(
                    title = "Haftalik Excel zaxira",
                    subtitle = if (s.backupFolder.isEmpty()) "Papka tanlanmagan. Yoqilganda papka so'raladi."
                    else "Tanlangan papkaga avtomatik saqlanadi" +
                        if (s.lastAutoBackup > 0L) ". Oxirgisi: ${Dates.formatDateTime(s.lastAutoBackup)}" else "",
                    checked = s.autoBackup,
                    onChange = { on ->
                        if (!on) {
                            vm.setAutoBackup(false)
                            android.widget.Toast.makeText(context, "Avto-zaxira o'chirildi", android.widget.Toast.LENGTH_SHORT).show()
                        } else if (s.backupFolder.isEmpty()) folderLauncher.launch(null)
                        else vm.setAutoBackup(true)
                    },
                )
                if (!s.autoBackup) {
                    SwitchRow(
                        title = "Zaxira eslatmasi",
                        subtitle = "7 kundan ortiq zaxira olinmasa, Bosh sahifada ogohlantiradi",
                        checked = s.backupReminder,
                        onChange = vm::setBackupReminder,
                    )
                }
                if (s.backupFolder.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { folderLauncher.launch(null) }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Papkani o'zgartirish", fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(
                            enabled = !working,
                            onClick = {
                                working = true
                                scope.launch {
                                    val error = withContext(Dispatchers.IO) { AutoBackup.backupNow(context) }
                                    vm.refreshSettings()
                                    working = false
                                    android.widget.Toast.makeText(
                                        context,
                                        error ?: "Zaxira papkaga saqlandi",
                                        android.widget.Toast.LENGTH_LONG,
                                    ).show()
                                }
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text(if (working) "Saqlanmoqda..." else "Hozir zaxira olish", fontWeight = FontWeight.SemiBold) }
                    }
                }
                if (s.autoBackup) {
                    TextButton(onClick = { advanced = !advanced }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(if (advanced) "Qo'shimcha sozlamalarni yopish" else "Qo'shimcha sozlamalar", fontWeight = FontWeight.SemiBold)
                    }
                    if (advanced) {
                        Text("Nechta fayl saqlansin", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        SegmentedToggle(
                            options = keeps.map { "$it ta" },
                            selected = keeps.indexOf(s.backupKeep).coerceAtLeast(0),
                            onSelect = { vm.setBackupKeep(keeps[it]) },
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Qaysi kuni", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            weekdays.forEach { (value, label) ->
                                FilterChip(
                                    selected = s.backupWeekday == value,
                                    onClick = { vm.setBackupWeekday(value) },
                                    label = { Text(label) },
                                    modifier = Modifier.heightIn(min = 40.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Hint("Avto: yoqilgan paytdan 7 kun. Kun tanlansa, shu kuni ertalab soat 3 atrofida saqlanadi.")
                    }
                }
            }
        }
        item {
            BigButton(text = "Excel: yuklab olish va tiklash", onClick = onOpenBackup, modifier = Modifier.fillMaxWidth())
        }
        item {
            AppCard {
                Text("Xavfli hudud", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Brand.expense)
                Spacer(Modifier.height(4.dp))
                Hint("Barcha yozuvlar, qarzlar, jamg'arma va maqsadlar o'chadi. PIN va sozlamalar qoladi. Avval Excel zaxira oling.")
                Spacer(Modifier.height(12.dp))
                OutlineBigButton(
                    text = "Hamma ma'lumotni o'chirish",
                    icon = Icons.Rounded.Delete,
                    color = Brand.expense,
                    onClick = { confirmWipe = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (confirmWipe) {
        ConfirmDialog(
            title = "Hamma ma'lumot o'chirilsinmi?",
            text = "Bu amalni qaytarib bo'lmaydi. Keyingi qadamda yana tasdiq so'raladi.",
            confirmLabel = "Davom etish",
            onConfirm = {
                confirmWipe = false
                typeWipe = true
            },
            onDismiss = { confirmWipe = false },
        )
    }
    if (typeWipe) {
        var typed by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { typeWipe = false },
            title = { Text("Oxirgi tasdiq") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Davom etish uchun \"O'CHIRISH\" so'zini yozing.")
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it.take(12) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = typed.trim().uppercase() == "O'CHIRISH",
                    onClick = {
                        vm.wipeAll()
                        typeWipe = false
                    },
                ) { Text("Hammasini o'chirish", color = Brand.expense) }
            },
            dismissButton = { TextButton(onClick = { typeWipe = false }) { Text("Bekor qilish") } },
        )
    }
}


