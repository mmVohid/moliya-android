package uz.moliya.app

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.LaunchedEffect
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

/** Birinchi ochilish: ortiqcha ekranlarsiz, ketma-ket faqat ruxsat/taklif oynalari. Hammasini Sozlamalardan o'zgartirish mumkin. */
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
        if (index >= 3) onFinish() else index += 1
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
                // ruxsat olinmadi
            }
        }
        next()
    }

    // Tizim oynalari (bildirishnoma, papka) darhol; qolganlari qisqa ha/yo'q oynasi
    LaunchedEffect(index) {
        when (index) {
            0 -> if (android.os.Build.VERSION.SDK_INT >= 33) {
                notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onReminders(true)
                next()
            }
            3 -> folderLauncher.launch(null)
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))

    if (index == 1) {
        AlertDialog(
            onDismissRequest = { next() },
            title = { Text("Ekranni himoyalaymizmi?") },
            text = { Text("Skrinshot olish va oxirgi ilovalarda summalar ko'rinishi bloklanadi.") },
            confirmButton = { TextButton(onClick = { onSecureScreen(true); next() }) { Text("Ha") } },
            dismissButton = { TextButton(onClick = { next() }) { Text("Hozir emas") } },
        )
    }
    if (index == 2 && !showPin) {
        AlertDialog(
            onDismissRequest = { next() },
            title = { Text("PIN-kod o'rnatamizmi?") },
            text = { Text("Ilovani 4 xonali PIN bilan qulflaysiz.") },
            confirmButton = { TextButton(onClick = { showPin = true }) { Text("Ha") } },
            dismissButton = { TextButton(onClick = { next() }) { Text("Hozir emas") } },
        )
    }
    if (index == 2 && showPin) {
        PinFlowSheet(
            mode = PinMode.SET,
            verify = verifyPin,
            onSet = {
                onSetPin(it)
                showPin = false
                next()
            },
            onRemove = onClearPin,
            onDismiss = {
                showPin = false
                next()
            },
        )
    }
}
