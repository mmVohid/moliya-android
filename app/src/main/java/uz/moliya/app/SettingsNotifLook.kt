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

// ───────────────────────── Bildirishnomalar ─────────────────────────

@Composable
internal fun NotificationsPage(vm: FinanceViewModel, onBack: () -> Unit) {
    val s = vm.settings
    val context = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showTime by remember { mutableStateOf(false) }
    var allowed by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (granted) pending?.invoke()
        pending = null
    }
    fun withPermission(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pending = action
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }

    Page("Bildirishnomalar", onBack) {
        item {
            AppCard {
                Text(
                    if (allowed) "Bildirishnomalar ruxsat etilgan" else "Bildirishnomalar o'chirilgan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (allowed) Brand.income else Brand.expense,
                )
                Spacer(Modifier.height(4.dp))
                Hint(if (allowed) "Eslatmalar telefon sozlamalariga ko'ra keladi." else "Eslatmalar kelishi uchun telefon sozlamalarida ruxsat bering.")
                Spacer(Modifier.height(12.dp))
                OutlineBigButton(
                    text = "Android sozlamalarini ochish",
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            AppCard {
                SwitchRow(
                    title = "Qarz muddati eslatmalari",
                    subtitle = "Muddatga 3 kun, 1 kun, bugun va 3 kun o'tganda, soat 9:00 da",
                    checked = s.reminders,
                    onChange = { on -> if (on) withPermission { vm.setReminders(true) } else vm.setReminders(false) },
                )
            }
        }
        item {
            AppCard {
                SwitchRow(
                    title = "Kunlik odat eslatmasi",
                    subtitle = "\"Bugungi xarajatlarni yozdingizmi?\" Bugun yozuv kiritgan bo'lsangiz, yuborilmaydi",
                    checked = s.dailyReminder,
                    onChange = { on ->
                        if (on) withPermission { vm.setDaily(true, s.dailyHour, s.dailyMinute) }
                        else vm.setDaily(false, s.dailyHour, s.dailyMinute)
                    },
                )
                if (s.dailyReminder) {
                    Spacer(Modifier.height(8.dp))
                    OutlineBigButton(
                        text = "Vaqt: " + String.format(java.util.Locale.US, "%02d:%02d", s.dailyHour, s.dailyMinute),
                        onClick = { showTime = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (showTime) {
        val state = rememberTimePickerState(initialHour = s.dailyHour, initialMinute = s.dailyMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text("Eslatma vaqti") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    vm.setDaily(true, state.hour, state.minute)
                    showTime = false
                }) { Text("Saqlash") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Bekor qilish") } },
        )
    }
}

// ───────────────────────── Ko'rinish ─────────────────────────

@Composable
internal fun LookPage(vm: FinanceViewModel, onBack: () -> Unit) {
    val s = vm.settings
    Page("Ko'rinish", onBack) {
        item {
            AppCard {
                Text("Mavzu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                SegmentedToggle(options = listOf("Tizim", "Yorug'", "Qorong'i"), selected = s.theme, onSelect = vm::setTheme)
                if (Build.VERSION.SDK_INT >= 31) {
                    Spacer(Modifier.height(8.dp))
                    SwitchRow(
                        title = "Dinamik rang",
                        subtitle = "Ilova rangi telefon fon rasmiga moslashadi",
                        checked = s.dynamicColor,
                        onChange = vm::setDynamicColor,
                    )
                }
            }
        }
        item {
            AppCard {
                Text("Raqam ko'rinishi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                SegmentedToggle(
                    options = listOf("1 500 000", "1.5 mln"),
                    selected = if (s.compactNumbers) 1 else 0,
                    onSelect = { vm.setCompactNumbers(it == 1) },
                )
            }
        }
        item {
            AppCard {
                Text("Hisob oyi boshlanish kuni", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Hint("Oylik 25-sanada tushsa, 25 deb qo'ying: oy balansi va hisobotlar 25 dan keyingi oyning 24 igacha hisoblanadi. Oddiy holatda 1 qoldiring.")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    IconButton(onClick = { vm.setMonthStartDay(s.monthStartDay - 1) }, enabled = s.monthStartDay > 1, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Kamaytirish")
                    }
                    Text(
                        "${s.monthStartDay}-sana",
                        modifier = Modifier.width(120.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = { vm.setMonthStartDay(s.monthStartDay + 1) }, enabled = s.monthStartDay < 28, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = "Oshirish")
                    }
                }
                Text(
                    "Joriy davr: " + Dates.monthLabel(0),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

