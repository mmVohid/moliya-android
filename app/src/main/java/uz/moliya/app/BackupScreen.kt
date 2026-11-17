package uz.moliya.app

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
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    notify: (String) -> Unit,
) {
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
                        stream.use { XlsxExporter.write(data, it) }
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

    val recordCount = data.txns.size + data.cushion.size + data.debts.size

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader("Backup", "Ma'lumotlaringizni Excelga saqlang") }

        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            if (data.lastBackup == 0L) "Hali zaxira olinmagan" else "Oxirgi zaxira",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            if (data.lastBackup == 0L) "Hozir birinchi nusxani oling" else Dates.formatDateTime(data.lastBackup),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Saqlanadigan ma'lumot: ${data.txns.size} ta daromad/xarajat, ${data.cushion.size} ta yostiq yozuvi, ${data.debts.size} ta qarz.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                BigButton(
                    text = "Excelga yuklab olish",
                    icon = Icons.Rounded.FileDownload,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { launcher.launch("moliya_${Dates.iso(System.currentTimeMillis())}.xlsx") },
                )
            }
        }

        item {
            AppCard {
                Text(
                    "Maslahat olish uchun",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                Step(1, "\"Excelga yuklab olish\" tugmasini bosing va joyni tanlang.")
                Spacer(Modifier.height(10.dp))
                Step(2, "Faylni Claude bilan suhbatga biriktiring.")
                Spacer(Modifier.height(10.dp))
                Step(3, "Maqsadingizni yozing, masalan: \"Oylik xarajatlarimni tahlil qil va tejash yo'llarini ayt\".")
            }
        }

        item {
            AppCard {
                Text(
                    "Muhim",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ma'lumotlar faqat shu telefonda, ilova ichida saqlanadi va internetga chiqmaydi. " +
                        "Ilova o'chirilsa yoki telefon almashtirilsa ma'lumotlar yo'qoladi, shuning uchun Excel nusxani oyda kamida bir marta oling." +
                        if (recordCount == 0) " Hozircha ma'lumot kiritilmagan." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
