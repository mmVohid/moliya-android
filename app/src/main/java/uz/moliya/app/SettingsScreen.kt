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

// ───────────────────────── Umumiy elementlar ─────────────────────────

@Composable
private fun SubHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Orqaga")
        }
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Page(title: String, onBack: () -> Unit, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { SubHeader(title, onBack) }
        content()
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun NavRow(icon: ImageVector, tint: Color, title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ───────────────────────── Asosiy ro'yxat ─────────────────────────

@Composable
private fun GroupLabel(text: String) {
    Text(
        text.uppercase(),
        modifier = Modifier.padding(start = 8.dp, top = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun Divider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun SettingsHome(vm: FinanceViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
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
            AppCard(padding = 0.dp) {
                NavRow(Icons.Rounded.Info, Color(0xFF6B7280), "Ilova haqida", "v${BuildConfig.VERSION_NAME}") { onOpen("about") }
            }
        }
    }
}

// ───────────────────────── Xavfsizlik ─────────────────────────

@Composable
private fun SecurityPage(vm: FinanceViewModel, onBack: () -> Unit) {
    val s = vm.settings
    val context = LocalContext.current
    val biometricAvailable = remember { Biometrics.available(context) }
    var pinMode by remember { mutableStateOf<PinMode?>(null) }
    var setPassword by remember { mutableStateOf(false) }
    val delays = listOf(0, 15, 60, 300)

    Page("Xavfsizlik va maxfiylik", onBack) {
        item {
            AppCard {
                Text("PIN-kod", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (s.hasPin) "Ilova PIN-kod bilan himoyalangan" else "Ilovani ochishda ${Pin.LENGTH} xonali PIN so'raladi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                if (!s.hasPin) {
                    BigButton(text = "PIN-kod o'rnatish", onClick = { pinMode = PinMode.SET }, modifier = Modifier.fillMaxWidth())
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlineBigButton(text = "O'zgartirish", onClick = { pinMode = PinMode.CHANGE }, modifier = Modifier.weight(1f))
                        OutlineBigButton(text = "O'chirish", color = Brand.expense, onClick = { pinMode = PinMode.REMOVE }, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Hint("Diqqat: PIN esdan chiqsa, ilovani qayta o'rnatish kerak bo'ladi va ma'lumotlar yo'qoladi. Excel nusxani muntazam oling.")
            }
        }
        item {
            AppCard {
                SwitchRow(
                    title = "Barmoq izi / yuz bilan ochish",
                    subtitle = when {
                        !biometricAvailable -> "Qurilmada barmoq izi yoki yuz sozlanmagan"
                        !s.hasPin -> "Avval PIN-kod o'rnating"
                        else -> "PIN o'rniga tezkor ochish"
                    },
                    checked = s.biometric && s.hasPin,
                    enabled = biometricAvailable && s.hasPin,
                    onChange = vm::setBiometric,
                )
            }
        }
        item {
            AppCard {
                SwitchRow(
                    title = "Ekranni himoyalash",
                    subtitle = "Skrinshot olishni va \"oxirgi ilovalar\" ro'yxatida ko'rinishni bloklaydi",
                    checked = s.secureScreen,
                    onChange = vm::setSecureScreen,
                )
            }
        }
        item {
            AppCard {
                SwitchRow(
                    title = "Ma'lumotlarni shifrlash (AES-256)",
                    subtitle = "Ma'lumot fayli telefon kaliti (Android Keystore) bilan shifrlanadi. Kalit telefondan chiqmaydi.",
                    checked = s.encryptData,
                    onChange = { on ->
                        vm.setEncryption(on)
                        android.widget.Toast.makeText(context, if (on) "Ma'lumot shifrlandi (AES-256)" else "Shifrlash o'chirildi", android.widget.Toast.LENGTH_SHORT).show()
                    },
                )
                Spacer(Modifier.height(4.dp))
                Hint("Excel zaxira va chek rasmlari shifrlanmaydi. Ilovani o'chirib qayta o'rnatsangiz, shifrlangan ma'lumotni tiklab bo'lmaydi, shuning uchun Excel nusxa oling.")
            }
        }
        item {
            AppCard {
                SwitchRow(
                    title = "Zaxirani parol bilan himoyalash",
                    subtitle = "Excel zaxira AES-256 bilan shifrlanadi. Fayl faqat Moliya'da, parol bilan tiklanadi.",
                    checked = s.backupEncrypted,
                    onChange = { on ->
                        if (on) setPassword = true else {
                            vm.setBackupPassword(null)
                            android.widget.Toast.makeText(context, "Zaxira paroli o'chirildi", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                )
                Spacer(Modifier.height(4.dp))
                Hint("Parolni unutsangiz, zaxirani tiklab bo'lmaydi. Shifrlangan fayl Excelda ochilmaydi. \"Claude'ga yuborish\" shifrlanmagan nusxa yuboradi.")
            }
        }
        if (s.hasPin) {
            item {
                AppCard {
                    Text("Qulflash vaqti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Hint("Ilova fonda shuncha turgach, qayta PIN so'raladi")
                    Spacer(Modifier.height(12.dp))
                    SegmentedToggle(
                        options = listOf("Darhol", "15 soniya", "1 daqiqa", "5 daqiqa"),
                        selected = delays.indexOf(s.lockDelaySec).coerceAtLeast(0),
                        onSelect = { vm.setLockDelay(delays[it]) },
                    )
                    Spacer(Modifier.height(8.dp))
                    SwitchRow(
                        title = "Ekran o'chganda qulflash",
                        subtitle = "Telefon ekrani o'chishi bilan ilova darhol qulflanadi",
                        checked = s.lockOnScreenOff,
                        onChange = vm::setLockOnScreenOff,
                    )
                }
            }
        }
    }

    if (setPassword) {
        var p1 by remember { mutableStateOf("") }
        var p2 by remember { mutableStateOf("") }
        val valid = p1.length >= 6 && p1 == p2
        AlertDialog(
            onDismissRequest = { setPassword = false },
            title = { Text("Zaxira paroli") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = p1,
                        onValueChange = { p1 = it },
                        label = { Text("Parol (kamida 6 belgi)") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        shape = RoundedCornerShape(14.dp),
                    )
                    OutlinedTextField(
                        value = p2,
                        onValueChange = { p2 = it },
                        label = { Text("Parolni takrorlang") },
                        singleLine = true,
                        isError = p2.isNotEmpty() && p1 != p2,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        shape = RoundedCornerShape(14.dp),
                    )
                    Hint("Parolni xavfsiz joyda saqlang. Uni tiklab bo'lmaydi.")
                }
            },
            confirmButton = {
                TextButton(enabled = valid, onClick = {
                    vm.setBackupPassword(p1)
                    setPassword = false
                    android.widget.Toast.makeText(context, "Zaxira paroli o'rnatildi", android.widget.Toast.LENGTH_SHORT).show()
                }) { Text("Yoqish") }
            },
            dismissButton = { TextButton(onClick = { setPassword = false }) { Text("Bekor qilish") } },
        )
    }

    val mode = pinMode
    if (mode != null) {
        PinFlowSheet(
            mode = mode,
            verify = vm::verifyPin,
            onSet = {
                vm.setPin(it)
                android.widget.Toast.makeText(context, "PIN-kod o'rnatildi", android.widget.Toast.LENGTH_SHORT).show()
            },
            onRemove = {
                vm.clearPin()
                android.widget.Toast.makeText(context, "PIN-kod o'chirildi", android.widget.Toast.LENGTH_SHORT).show()
            },
            onDismiss = { pinMode = null },
        )
    }
}

// ───────────────────────── Bildirishnomalar ─────────────────────────

@Composable
private fun NotificationsPage(vm: FinanceViewModel, onBack: () -> Unit) {
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
private fun LookPage(vm: FinanceViewModel, onBack: () -> Unit) {
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

// ───────────────────────── Ma'lumotlar va zaxira ─────────────────────────

@Composable
private fun DataPage(vm: FinanceViewModel, onOpenBackup: () -> Unit, onBack: () -> Unit) {
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

@Composable
private fun StatusLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

// ───────────────────────── Ilova haqida ─────────────────────────

@Composable
private fun AboutPage(onBack: () -> Unit) {
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
                        "6. Backup bo'limidan Excel oling. Sozlamalarda avto-zaxira va parol bilan himoya yoqiladi.",
                )
            },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Tushunarli") } },
        )
    }
}
