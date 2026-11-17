package uz.moliya.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun SettingsScreen(
    settings: AppSettings,
    verifyPin: (String) -> Boolean,
    onSetPin: (String) -> Unit,
    onClearPin: () -> Unit,
    onBiometric: (Boolean) -> Unit,
    onTheme: (Int) -> Unit,
    onReminders: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val biometricAvailable = remember { Biometrics.available(context) }
    var pinMode by remember { mutableStateOf<PinMode?>(null) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> onReminders(granted) }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Orqaga")
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Sozlamalar",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        item { SectionTitle("Xavfsizlik") }
        item {
            AppCard {
                Text("PIN-kod", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (settings.hasPin) "Ilova PIN-kod bilan himoyalangan" else "Ilovani ochishda ${Pin.LENGTH} xonali PIN so'raladi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                if (!settings.hasPin) {
                    BigButton(
                        text = "PIN-kod o'rnatish",
                        onClick = { pinMode = PinMode.SET },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlineBigButton(
                            text = "O'zgartirish",
                            onClick = { pinMode = PinMode.CHANGE },
                            modifier = Modifier.weight(1f),
                        )
                        OutlineBigButton(
                            text = "O'chirish",
                            color = Brand.expense,
                            onClick = { pinMode = PinMode.REMOVE },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Diqqat: PIN-kod esdan chiqsa, ilovani qayta o'rnatish kerak bo'ladi va ma'lumotlar yo'qoladi. Excel nusxani muntazam oling.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            AppCard {
                SwitchRow(
                    title = "Barmoq izi / yuz bilan ochish",
                    subtitle = when {
                        !biometricAvailable -> "Qurilmada barmoq izi yoki yuz sozlanmagan"
                        !settings.hasPin -> "Avval PIN-kod o'rnating"
                        else -> "PIN o'rniga tezkor ochish"
                    },
                    checked = settings.biometric && settings.hasPin,
                    enabled = biometricAvailable && settings.hasPin,
                    onChange = onBiometric,
                )
            }
        }

        item { SectionTitle("Ko'rinish") }
        item {
            AppCard {
                Text("Mavzu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                SegmentedToggle(
                    options = listOf("Tizim", "Yorug'", "Qorong'i"),
                    selected = settings.theme,
                    onSelect = onTheme,
                )
            }
        }

        item { SectionTitle("Eslatmalar") }
        item {
            AppCard {
                SwitchRow(
                    title = "Qarz muddati eslatmalari",
                    subtitle = "Muddatga 3 kun, 1 kun, bugun va 3 kun o'tganda, soat 9:00 da",
                    checked = settings.reminders,
                    enabled = true,
                    onChange = { on ->
                        if (!on) {
                            onReminders(false)
                        } else if (Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onReminders(true)
                        }
                    },
                )
            }
        }

        item {
            Text(
                "Moliya 1.0. Ma'lumotlar faqat shu telefonda saqlanadi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    val mode = pinMode
    if (mode != null) {
        PinFlowSheet(
            mode = mode,
            verify = verifyPin,
            onSet = onSetPin,
            onRemove = onClearPin,
            onDismiss = { pinMode = null },
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
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
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
