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

// ───────────────────────── Ilova haqida ─────────────────────────

@Composable
internal fun AboutPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<UpdateResult?>(null) }
    var showHelp by remember { mutableStateOf(false) }

    Page("Ilova haqida", onBack) {
        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Moliya", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(
                        "v${BuildConfig.VERSION_NAME} (${BuildConfig.GIT_SHA.take(7)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Ma'lumotlaringiz faqat shu telefonda saqlanadi va hech qayerga yuborilmaydi. " +
                        "PIN ilovani qulflaydi, ma'lumotni shifrlash alohida yoqiladi. " +
                        "Excel zaxira va chek rasmlari parolsiz himoyasiz, ularni xavfsiz joyda saqlang. " +
                        "PIN yoki parol esdan chiqsa, tiklab bo'lmaydi.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            AppCard(padding = 0.dp) {
                NavRow(Icons.Rounded.Info, Color(0xFF2E7D6B), "Qisqa qo'llanma", "") { showHelp = true }
                Divider()
                NavRow(Icons.Rounded.Edit, Color(0xFFE57C23), "Muammo haqida yozish", "") {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mmVohid/moliya-android/issues/new"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
                Divider()
                NavRow(Icons.Rounded.Code, Color(0xFF24292F), "Manba kodini ko'rish", "GitHub") {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mmVohid/moliya-android"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
        }
        item {
            AppCard {
                Text("Yangilanish", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    when (val r = result) {
                        is UpdateResult.Latest -> "Sizda eng so'nggi versiya o'rnatilgan."
                        is UpdateResult.Available -> "Yangi versiya chiqqan. Yuklab oling, fayl ochilgach \"Yangilash\" ni bosing. Ma'lumotlaringiz saqlanadi."
                        is UpdateResult.Failed -> "Tekshirib bo'lmadi: ${r.message}"
                        null -> "Internet faqat tugmani bosganingizda ishlatiladi. Hech qanday ma'lumot yuborilmaydi."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (result) {
                        is UpdateResult.Latest, is UpdateResult.Available -> Brand.income
                        is UpdateResult.Failed -> Brand.expense
                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(Modifier.height(12.dp))
                val available = result as? UpdateResult.Available
                if (available != null) {
                    BigButton(
                        text = "Yangi versiyani yuklab olish",
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(available.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    BigButton(
                        text = if (checking) "Tekshirilmoqda..." else if (result is UpdateResult.Failed) "Qayta urinish" else "Yangilanishni tekshirish",
                        enabled = !checking,
                        onClick = {
                            checking = true
                            scope.launch {
                                result = UpdateChecker.check()
                                checking = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Qisqa qo'llanma") },
            text = {
                Text(
                    "1. Bosh sahifadagi \"Daromad\" va \"Xarajat\" tugmalari bilan yozuv kiriting. Faqat summa majburiy.\n\n" +
                        "2. Pul oqimida yozuvni bossangiz tahrirlanadi. O'chirsangiz, \"Qaytarish\" chiqadi.\n\n" +
                        "3. \"Oylik limit\" tugmasi bilan bir oyda jami qancha sarflashni belgilang.\n\n" +
                        "4. Qarzlar bo'limida olgan va bergan qarzlaringizni, muddatlarini kuzating.\n\n" +
                        "5. Jamg'armada himoya yostig'i va shaxsiy maqsadlar bor.\n\n" +
                        "6. Backup bo'limidan Excel oling. Sozlamalarda avto-zaxira va parol bilan himoya yoqiladi.\n\n" +
                        "7. Tezkor panel: telefonning tepa panelini tortib, qalamcha (tahrirlash) belgisini bosing va Daromad, Xarajat, Qarz plitkalarini qo'shing.",
                )
            },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Tushunarli") } },
        )
    }
}
