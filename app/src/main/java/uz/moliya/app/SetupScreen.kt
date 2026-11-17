package uz.moliya.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private data class SetupStep(val icon: ImageVector, val title: String, val text: String, val action: String)

private val steps = listOf(
    SetupStep(
        Icons.Rounded.Notifications,
        "Eslatmalarni yoqamizmi?",
        "Qarz muddati yaqinlashganda bildirishnoma yuboramiz, shunda hech narsani unutmaysiz.",
        "Ha, yoqish",
    ),
    SetupStep(
        Icons.Rounded.Security,
        "Ekranni himoyalaymizmi?",
        "Skrinshot olish va \"oxirgi ilovalar\" ro'yxatida summalar ko'rinishi bloklanadi.",
        "Ha, himoyalash",
    ),
    SetupStep(
        Icons.Rounded.Lock,
        "PIN-kod o'rnatamizmi?",
        "Ilovani 4 xonali PIN bilan qulflang. Telefoningiz qo'lga tushsa ham ma'lumotlar xavfsiz bo'ladi.",
        "Ha, PIN o'rnatish",
    ),
    SetupStep(
        Icons.Rounded.CloudUpload,
        "Avtomatik zaxira kerakmi?",
        "Papka tanlang. Har hafta Excel nusxa o'sha yerga o'zi saqlanadi, ma'lumot yo'qolmaydi.",
        "Ha, papka tanlash",
    ),
)

/** Birinchi ochilishdagi ixtiyoriy takliflar. Har bir qadamni "Hozir emas" bilan o'tkazib yuborish mumkin. */
@Composable
fun SetupScreen(
    settings: AppSettings,
    onReminders: (Boolean) -> Unit,
    onSecureScreen: (Boolean) -> Unit,
    onSetPin: (String) -> Unit,
    verifyPin: (String) -> Boolean,
    onClearPin: () -> Unit,
    onPickFolder: (String) -> Unit,
    onAutoBackup: (Boolean) -> Unit,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    var index by rememberSaveable { mutableStateOf(0) }
    var showPin by remember { mutableStateOf(false) }

    fun next() {
        if (index >= steps.lastIndex) onFinish() else index += 1
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onReminders(true)
        next()
    }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                onPickFolder(uri.toString())
                onAutoBackup(true)
            } catch (e: SecurityException) {
                // ruxsat olinmadi, o'tkazib yuboramiz
            }
        }
        next()
    }

    val step = steps[index]

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Sozlash ${index + 1}/${steps.size}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onFinish, modifier = Modifier.heightIn(min = 48.dp)) { Text("O'tkazib yuborish") }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(step.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(46.dp))
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    step.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    step.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    steps.indices.forEach { i ->
                        Box(
                            Modifier
                                .size(if (i == index) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (i == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                }
            }

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                BigButton(
                    text = step.action,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        when (index) {
                            0 -> {
                                if (Build.VERSION.SDK_INT >= 33 &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                                    PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onReminders(true)
                                    next()
                                }
                            }
                            1 -> {
                                onSecureScreen(true)
                                next()
                            }
                            2 -> if (settings.hasPin) next() else showPin = true
                            else -> folderLauncher.launch(null)
                        }
                    },
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { next() }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Hozir emas", fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "Hammasini keyinroq Sozlamalardan o'zgartirish mumkin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (showPin) {
        PinFlowSheet(
            mode = PinMode.SET,
            verify = verifyPin,
            onSet = {
                onSetPin(it)
                showPin = false
                next()
            },
            onRemove = onClearPin,
            onDismiss = { showPin = false },
        )
    }
}
