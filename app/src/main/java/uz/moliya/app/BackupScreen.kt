package uz.moliya.app

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.CheckCircle
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

@Composable
fun BackupScreen(
    data: FinanceData,
    onBackedUp: () -> Unit,
    lastBackupAt: Long,
    autoBackupOn: Boolean,
    encrypted: Boolean,
    onRestore: (FinanceData) -> Unit,
    notify: (String) -> Unit,
) {
    var pending by remember { mutableStateOf<FinanceData?>(null) }
    var encryptedPending by remember { mutableStateOf<ByteArray?>(null) }
    var askPassword by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(XLSX_MIME),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        val stream = context.contentResolver.openOutputStream(uri)
                            ?: return@withContext false
                        stream.use { it.write(BackupCrypto.export(context, data).first) }
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                if (ok) {
                    onBackedUp()
                    notify("Excel fayl saqlandi")
                } else {
                    notify("Saqlashda xatolik yuz berdi. Qayta urinib ko'ring.")
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val outcome: Pair<FinanceData?, String?> = withContext(Dispatchers.IO) {
                    try {
                        val stream = context.contentResolver.openInputStream(uri)
                        if (stream == null) {
                            Pair(null, "Faylni ochib bo'lmadi")
                        } else {
                            val bytes = stream.use { it.readBytes() }
                            if (BackupCrypto.isEncrypted(bytes)) {
                                encryptedPending = bytes
                                Pair(null, "")
                            } else {
                                Pair(XlsxImporter.read(java.io.ByteArrayInputStream(bytes)), null)
                            }
                        }
                    } catch (e: ImportException) {
                        Pair(null, e.message)
                    } catch (e: Exception) {
                        Pair(null, "Faylni o'qib bo'lmadi. Moliya'dan yuklab olingan .xlsx faylni tanlang.")
                    }
                }
                val imported = outcome.first
                if (imported != null) pending = imported else if (outcome.second != "") notify(outcome.second ?: "Faylni o'qib bo'lmadi")
            }
        }
    }

    val enc = encryptedPending
    if (enc != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { encryptedPending = null; askPassword = "" },
            title = { androidx.compose.material3.Text("Zaxira paroli") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = askPassword,
                    onValueChange = { askPassword = it },
                    label = { androidx.compose.material3.Text("Parol") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = askPassword.isNotEmpty(),
                    onClick = {
                        try {
                            val plain = BackupCrypto.decrypt(enc, askPassword)
                            pending = XlsxImporter.read(java.io.ByteArrayInputStream(plain))
                            encryptedPending = null
                            askPassword = ""
                        } catch (e: ImportException) {
                            notify(e.message ?: "Faylni o'qib bo'lmadi")
                            encryptedPending = null
                        } catch (e: Exception) {
                            notify("Parol noto'g'ri yoki fayl buzilgan")
                        }
                    },
                ) { androidx.compose.material3.Text("Ochish") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { encryptedPending = null; askPassword = "" }) {
                    androidx.compose.material3.Text("Bekor qilish")
                }
            },
        )
    }

    val recordCount = data.txns.size + data.cushion.size + data.debts.size

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp + LocalBottomInset.current),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader("Backup", "Ma'lumotlaringiz xavfsiz turishi uchun") }

        item {
            val now = System.currentTimeMillis()
            val ageDays = if (lastBackupAt > 0L) ((now - lastBackupAt) / 86_400_000L).toInt() else -1
            val state = when {
                ageDays < 0 -> 0
                ageDays <= 7 -> 2
                else -> 1
            }
            val color = when (state) {
                2 -> Brand.income
                1 -> Brand.warning
                else -> Brand.expense
            }
            val container = color.copy(alpha = 0.10f)
            AppCard(containerColor = container, padding = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (state == 2) Icons.Rounded.CheckCircle else if (state == 1) Icons.Rounded.Warning else Icons.Rounded.Backup,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (state) {
                                2 -> "Zaxira yangi"
                                1 -> "Zaxira eskirgan"
                                else -> "Zaxira olinmagan"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = color,
                        )
                        Text(
                            when {
                                ageDays < 0 -> "Hozir birinchi nusxani oling"
                                ageDays == 0 -> "Bugun, " + Dates.formatDateTime(lastBackupAt)
                                else -> "$ageDays kun oldin, " + Dates.formatDateTime(lastBackupAt)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoPill(if (autoBackupOn) "Avto-zaxira: yoqilgan" else "Avto-zaxira: o'chiq", autoBackupOn)
                    InfoPill(if (encrypted) "Parol: yoqilgan" else "Parol: yo'q", encrypted)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CountTile("Yozuvlar", data.txns.size, Modifier.weight(1f))
                CountTile("Qarzlar", data.debts.size, Modifier.weight(1f))
                CountTile("Yostiq", data.cushion.size, Modifier.weight(1f))
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BigButton(
                    text = "Excelga yuklab olish",
                    icon = Icons.Rounded.FileDownload,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { launcher.launch("moliya_${Dates.iso(System.currentTimeMillis())}" + if (encrypted) ".mbk" else ".xlsx") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlineBigButton(
                        text = "Tiklash",
                        icon = Icons.Rounded.FileUpload,
                        modifier = Modifier.weight(1f),
                        onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                    )
                    OutlineBigButton(
                        text = "Claude'ga",
                        icon = Icons.Rounded.Send,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val error = ClaudeShare.share(context, data)
                            if (error != null) notify(error) else onBackedUp()
                        },
                    )
                }
            }
        }

        item {
            AppCard {
                Text("Eslatma", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ma'lumot faqat shu telefonda saqlanadi. Ilova o'chirilsa yoki telefon almashsa yo'qoladi, shuning uchun Excel nusxani oyda kamida bir marta oling. " +
                        "\"Claude'ga\" tugmasi faylni tahlil va maslahat uchun tayyorlab yuboradi. Avto-zaxira va parol Sozlamalarda yoqiladi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    val toRestore = pending
    if (toRestore != null) {
        ConfirmDialog(
            title = "Ma'lumotlarni tiklash",
            text = "Fayldan ${toRestore.txns.size} ta daromad/xarajat, ${toRestore.cushion.size} ta yostiq yozuvi, " +
                "${toRestore.debts.size} ta qarz topildi. Hozirgi barcha ma'lumotlar shular bilan almashtiriladi. Davom etasizmi?",
            confirmLabel = "Tiklash",
            onConfirm = {
                onRestore(toRestore)
                pending = null
                notify("Ma'lumotlar tiklandi")
            },
            onDismiss = { pending = null },
        )
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}


@Composable
private fun InfoPill(text: String, on: Boolean) {
    val color = if (on) Brand.income else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}

@Composable
private fun CountTile(label: String, count: Int, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier, padding = 14.dp) {
        Text("$count", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
