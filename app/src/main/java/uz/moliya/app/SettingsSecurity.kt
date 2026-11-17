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

// ───────────────────────── Xavfsizlik ─────────────────────────

@Composable
internal fun SecurityPage(vm: FinanceViewModel, onBack: () -> Unit) {
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

