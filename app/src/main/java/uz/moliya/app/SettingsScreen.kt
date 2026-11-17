@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package uz.moliya.app

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
private fun SettingsHome(vm: FinanceViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val s = vm.settings
    val data = vm.data
    var editName by remember { mutableStateOf(false) }

    val protection = buildList {
        if (s.hasPin) add("PIN")
        if (s.hasPin && s.biometric) add("barmoq izi")
        if (s.secureScreen) add("ekran himoyasi")
    }
    val notifCount = listOf(s.reminders, s.dailyReminder, s.backupReminder).count { it }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { SubHeader("Sozlamalar", onBack) }

        item {
            AppCard(onClick = { editName = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            (s.userName.firstOrNull()?.uppercase() ?: "M"),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (s.userName.isBlank()) "Ismingizni kiriting" else s.userName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            if (protection.isEmpty()) "Himoya: yoqilmagan" else "Himoya: " + protection.joinToString(" + "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.Rounded.Edit, contentDescription = "Ismni o'zgartirish", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            AppCard(padding = 0.dp) {
                NavRow(Icons.Rounded.Lock, Color(0xFF2E7D6B), "Xavfsizlik va maxfiylik", if (s.hasPin) "PIN: yoqilgan" else "PIN: o'chiq") { onOpen("security") }
                NavRow(Icons.Rounded.Notifications, Color(0xFFE57C23), "Bildirishnomalar", if (notifCount == 0) "O'chiq" else "$notifCount ta yoqilgan") { onOpen("notif") }
                NavRow(Icons.Rounded.Palette, Color(0xFF7B5EA7), "Ko'rinish", listOf("Tizim", "Yorug'", "Qorong'i")[s.theme.coerceIn(0, 2)]) { onOpen("look") }
                NavRow(Icons.Rounded.Storage, Color(0xFF2F80ED), "Ma'lumotlar va zaxira", if (s.autoBackup) "Avto: yoqilgan" else "Avto: o'chiq") { onOpen("data") }
                NavRow(Icons.Rounded.Info, Color(0xFF6B7280), "Ilova haqida", "v${BuildConfig.VERSION_NAME}") { onOpen("about") }
            }
        }
    }

    if (editName) {
        var name by remember { mutableStateOf(s.userName) }
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text("Ismingiz") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    label = { Text("Ism") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setUserName(name)
                    editName = false
                }) { Text("Saqlash") }
            },
            dismissButton = { TextButton(onClick = { editName = false }) { Text("Bekor qilish") } },
        )
    }
}

// ───────────────────────── Xavfsizlik ─────────────────────────

@Composable
private fun SecurityPage(vm: FinanceViewModel, onBack: () -> Unit) {
    val s = vm.settings
    val context = LocalContext.current
    val biometricAvailable = remember { Biometrics.available(context) }
    var pinMode by remember { mutableStateOf<PinMode?>(null) }
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
                Text("Qulflash vaqti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Hint("Ilova fonda shuncha turgach, qayta PIN so'raladi")
                Spacer(Modifier.height(12.dp))
                SegmentedToggle(
                    options = listOf("Darhol", "15 soniya", "1 daqiqa", "5 daqiqa"),
                    selected = delays.indexOf(s.lockDelaySec).coerceAtLeast(0),
                    onSelect = { vm.setLockDelay(delays[it]) },
                )
                if (!s.hasPin) {
                    Spacer(Modifier.height(8.dp))
                    Hint("Qulflash faqat PIN o'rnatilganda ishlaydi.")
                }
            }
        }
    }

    val mode = pinMode
    if (mode != null) {
        PinFlowSheet(
            mode = mode,
            verify = vm::verifyPin,
            onSet = vm::setPin,
            onRemove = vm::clearPin,
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
        item {
            AppCard {
                SwitchRow(
                    title = "Zaxira eslatmasi",
                    subtitle = "Zaxira nusxa 7 kundan ortiq olinmasa, Bosh sahifada ogohlantiradi (avto-zaxira yoqilmagan bo'lsa)",
                    checked = s.backupReminder,
                    onChange = vm::setBackupReminder,
                )
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
    val cardNames = listOf(
        1 to "Oxirgi 12 oy grafigi",
        2 to "Bu oy xarajatlari",
        4 to "Jamg'arma darajasi",
        8 to "Jamg'arma va qarzlar",
        16 to "Tahlil ro'yxati",
    )
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
                SwitchRow(
                    title = "Balansni yashirish",
                    subtitle = "Summalar •••••• bilan almashtiriladi",
                    checked = vm.data.hideBalance,
                    onChange = vm::setHideBalance,
                )
            }
        }
        item {
            AppCard {
                Text("Matn o'lchami", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                SegmentedToggle(options = listOf("Oddiy", "Katta", "Juda katta"), selected = s.textScale, onSelect = vm::setTextScale)
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
    }
}

// ───────────────────────── Ma'lumotlar va zaxira ─────────────────────────

@Composable
private fun DataPage(vm: FinanceViewModel, onOpenBackup: () -> Unit, onBack: () -> Unit) {
    val s = vm.settings
    val context = LocalContext.current
    var confirmWipe by remember { mutableStateOf(false) }
    var typeWipe by remember { mutableStateOf(false) }
    val keeps = listOf(2, 4, 8, 12)
    val weekdays = listOf(0 to "Avto", 2 to "Du", 3 to "Se", 4 to "Ch", 5 to "Pa", 6 to "Ju", 7 to "Sh", 1 to "Ya")

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
                Text("Hisob oyi boshlanish kuni", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Hint("Oylik 25-sanada tushsa, 25 deb qo'ying: oy balansi va hisobotlar 25 dan 24 gacha hisoblanadi.")
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
                        if (!on) vm.setAutoBackup(false)
                        else if (s.backupFolder.isEmpty()) folderLauncher.launch(null)
                        else vm.setAutoBackup(true)
                    },
                )
                if (s.backupFolder.isNotEmpty()) {
                    TextButton(onClick = { folderLauncher.launch(null) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Papkani o'zgartirish", fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(8.dp))
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
                Hint("Avto: yoqilgan paytdan 7 kun. Kun tanlansa, shu kuni ertalab soat 3 atrofida saqlanadi. Fayllar PIN bilan himoyalanmaydi, papkani xavfsiz joyda tanlang.")
            }
        }
        item {
            AppCard {
                Text("Excel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Oxirgi zaxira: " + (vm.lastBackupAt.takeIf { it > 0L }?.let { Dates.formatDateTime(it) } ?: "hali olinmagan"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                BigButton(text = "Yuklab olish va tiklash", onClick = onOpenBackup, modifier = Modifier.fillMaxWidth())
            }
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
                Text("Moliya", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Versiya ${BuildConfig.VERSION_NAME} (${BuildConfig.GIT_SHA.take(7)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Ma'lumotlaringiz faqat shu telefonda saqlanadi. Hech qayerga yuborilmaydi.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            AppCard {
                Text("Cheklovlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Hint("• PIN-kod ilovani qulflaydi, lekin ma'lumotni shifrlamaydi.")
                Hint("• Excel zaxira fayllari himoyasiz. Ularni xavfsiz joyda saqlang.")
                Hint("• PIN esdan chiqsa, tiklab bo'lmaydi.")
                Hint("• Chek rasmlari telefonda qoladi va Excelga kirmaydi.")
            }
        }
        item {
            AppCard {
                Text("Yangilanish", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Hint("Internet faqat siz tugmani bosganingizda ishlatiladi. Hech qanday ma'lumot yuborilmaydi.")
                Spacer(Modifier.height(12.dp))
                BigButton(
                    text = if (checking) "Tekshirilmoqda..." else "Yangilanishni tekshirish",
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
                when (val r = result) {
                    is UpdateResult.Latest -> {
                        Spacer(Modifier.height(12.dp))
                        Text("Sizda eng so'nggi versiya o'rnatilgan.", color = Brand.income, fontWeight = FontWeight.SemiBold)
                    }
                    is UpdateResult.Available -> {
                        Spacer(Modifier.height(12.dp))
                        Text("Yangi versiya bor!", color = Brand.income, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        OutlineBigButton(
                            text = "Yuklab olish",
                            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        Hint("Yuklangan faylni oching va \"Yangilash\" ni tanlang. Ma'lumotlaringiz saqlanadi.")
                    }
                    is UpdateResult.Failed -> {
                        Spacer(Modifier.height(12.dp))
                        Text("Tekshirib bo'lmadi: ${r.message}", color = Brand.expense)
                    }
                    null -> {}
                }
            }
        }
        item {
            AppCard(padding = 0.dp) {
                NavRow(Icons.Rounded.Info, Color(0xFF2E7D6B), "Qisqa qo'llanma", "") { showHelp = true }
                NavRow(Icons.Rounded.Edit, Color(0xFFE57C23), "Muammo haqida yozish", "") {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mmVohid/moliya-android/issues/new"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
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
                        "3. Limit tugmasi bilan kategoriya bo'yicha oylik chegara qo'ying.\n\n" +
                        "4. Qarzlar bo'limida olgan va bergan qarzlaringizni kuzating.\n\n" +
                        "5. Jamg'arma bo'limida himoya yostig'i va shaxsiy maqsadlar bor.\n\n" +
                        "6. Backup bo'limidan Excel oling. \"Claude'ga yuborish\" tugmasi tahlil va maslahat olishga tayyorlaydi.",
                )
            },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Tushunarli") } },
        )
    }
}
