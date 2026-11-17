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

/** Birinchi ochilish: faqat bildirishnoma ruxsati so'raladi. Qolganini foydalanuvchi Sozlamalardan yoqadi. */
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
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onReminders(true)
        onFinish()
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onReminders(true)
            onFinish()
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
}
