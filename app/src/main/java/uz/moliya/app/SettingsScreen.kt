@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package uz.moliya.app

import androidx.compose.material.icons.rounded.Share
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

@Composable
fun SettingsScreen(vm: FinanceViewModel, onOpenBackup: () -> Unit, onBack: () -> Unit) {
    var page by rememberSaveable { mutableStateOf("home") }
    BackHandler(enabled = page != "home") { page = "home" }
    val back = { page = "home" }
    when (page) {
        "security" -> SecurityPage(vm, back)
        "notif" -> NotificationsPage(vm, back)
        "look" -> LookPage(vm, back)
        "data" -> DataPage(vm, onOpenBackup, back)
        "about" -> AboutPage(back)
        else -> SettingsHome(vm, onBack, onOpen = { page = it })
    }
}

// ───────────────────────── Asosiy ro'yxat ─────────────────────────

@Composable
internal fun SettingsHome(vm: FinanceViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val s = vm.settings
    val notifCount = listOf(s.reminders, s.dailyReminder).count { it }
    val securityStatus = buildList {
        if (s.hasPin) add("PIN")
        if (s.encryptData) add("AES-256")
    }.ifEmpty { listOf("O'chiq") }.joinToString(" · ")

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SubHeader("Sozlamalar", onBack) }

        item { GroupLabel("Asosiy") }
        item {
            AppCard(padding = 0.dp) {
                NavRow(Icons.Rounded.Palette, Color(0xFF7B5EA7), "Ko'rinish", listOf("Tizim", "Yorug'", "Qorong'i")[s.theme.coerceIn(0, 2)]) { onOpen("look") }
                Divider()
                NavRow(Icons.Rounded.Notifications, Color(0xFFE57C23), "Bildirishnomalar", if (notifCount == 0) "O'chiq" else "$notifCount ta yoqilgan") { onOpen("notif") }
            }
        }

        item { GroupLabel("Xavfsizlik") }
        item {
            AppCard(padding = 0.dp) {
                NavRow(Icons.Rounded.Storage, Color(0xFF2F80ED), "Ma'lumotlar va zaxira", if (s.autoBackup) "Avto: yoqilgan" else "Avto: o'chiq") { onOpen("data") }
                Divider()
                NavRow(Icons.Rounded.Lock, Color(0xFFD94F70), "Xavfsizlik va maxfiylik", securityStatus) { onOpen("security") }
            }
        }

        item { GroupLabel("Boshqa") }
        item {
            val context = LocalContext.current
            AppCard(padding = 0.dp) {
                NavRow(Icons.Rounded.Share, Color(0xFF2E7D6B), "Do'stga ulashish", "") {
                    val text = "Moliya: shaxsiy moliyani kuzatish ilovasi. Ma'lumotlar faqat telefoningizda saqlanadi.\n" +
                        "Yuklab olish: https://github.com/mmVohid/moliya-android/releases/tag/latest"
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(send, "Moliya'ni ulashish").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                Divider()
                NavRow(Icons.Rounded.Info, Color(0xFF6B7280), "Ilova haqida", "v${BuildConfig.VERSION_NAME}") { onOpen("about") }
            }
        }
    }
}

