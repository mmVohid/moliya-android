package uz.moliya.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun PinDots(filled: Int, error: Boolean, modifier: Modifier = Modifier) {
    val active = if (error) Brand.expense else MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.outline
    Row(
        modifier.semantics { contentDescription = "PIN: $filled / ${Pin.LENGTH} xona kiritildi" },
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        for (i in 0 until Pin.LENGTH) {
            Box(
                Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (i < filled) active else empty.copy(alpha = 0.5f)),
            )
        }
    }
}

@Composable
private fun PinKey(label: String, description: String, onClick: () -> Unit, enabled: Boolean) {
    Box(
        Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PinPad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leftSlot: (@Composable () -> Unit)? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { ch ->
                    PinKey(ch.toString(), ch.toString(), { onDigit(ch) }, enabled)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                if (leftSlot != null) leftSlot()
            }
            PinKey("0", "0", { onDigit('0') }, enabled)
            Box(
                Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .clickable(enabled = enabled, role = Role.Button, onClick = onDelete)
                    .semantics { contentDescription = "Oxirgi raqamni o'chirish" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Backspace, contentDescription = null, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
fun LockScreen(
    verify: (String) -> Boolean,
    biometricEnabled: Boolean,
    onUnlock: () -> Unit,
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var failures by remember { mutableStateOf(0) }
    var lockedUntil by remember { mutableStateOf(0L) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    val waiting = lockedUntil > now

    fun tryBiometric() {
        val activity = context.findFragmentActivity()
        if (activity != null) Biometrics.prompt(activity, onUnlock)
    }

    LaunchedEffect(Unit) {
        if (biometricEnabled) tryBiometric()
    }

    LaunchedEffect(lockedUntil) {
        while (System.currentTimeMillis() < lockedUntil) {
            now = System.currentTimeMillis()
            delay(500)
        }
        now = System.currentTimeMillis()
    }

    fun onDigit(c: Char) {
        if (waiting || pin.length >= Pin.LENGTH) return
        error = false
        pin += c
        if (pin.length == Pin.LENGTH) {
            if (verify(pin)) {
                onUnlock()
            } else {
                failures += 1
                error = true
                pin = ""
                if (failures >= 5) {
                    lockedUntil = System.currentTimeMillis() + 30_000L
                    now = System.currentTimeMillis()
                    failures = 0
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("PIN-kodni kiriting", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        val message = when {
            waiting -> "Ko'p marta xato kiritildi. ${((lockedUntil - now) / 1000 + 1)} soniyadan keyin urinib ko'ring."
            error -> "PIN noto'g'ri. Qaytadan urinib ko'ring."
            else -> "Moliya ma'lumotlaringiz himoyalangan"
        }
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (error || waiting) Brand.expense else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(28.dp))
        PinDots(filled = pin.length, error = error)
        Spacer(Modifier.height(36.dp))
        PinPad(
            onDigit = { onDigit(it) },
            onDelete = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
            enabled = !waiting,
            leftSlot = if (biometricEnabled) {
                {
                    Box(
                        Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button) { tryBiometric() }
                            .semantics { contentDescription = "Barmoq izi yoki yuz bilan ochish" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
            } else {
                null
            },
        )
    }
}

enum class PinMode { SET, CHANGE, REMOVE }

private enum class PinStep { VERIFY, NEW, CONFIRM }

@Composable
fun PinFlowSheet(
    mode: PinMode,
    verify: (String) -> Boolean,
    onSet: (String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableStateOf(if (mode == PinMode.SET) PinStep.NEW else PinStep.VERIFY) }
    var pin by remember { mutableStateOf("") }
    var first by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val title = when (step) {
        PinStep.VERIFY -> "Joriy PIN-kodni kiriting"
        PinStep.NEW -> "Yangi PIN-kod (${Pin.LENGTH} xona)"
        PinStep.CONFIRM -> "PIN-kodni takrorlang"
    }

    fun onDigit(c: Char) {
        if (pin.length >= Pin.LENGTH) return
        error = null
        pin += c
        if (pin.length < Pin.LENGTH) return
        val entered = pin
        pin = ""
        when (step) {
            PinStep.VERIFY -> {
                if (verify(entered)) {
                    if (mode == PinMode.REMOVE) {
                        onRemove()
                        onDismiss()
                    } else {
                        step = PinStep.NEW
                    }
                } else {
                    error = "PIN noto'g'ri"
                }
            }
            PinStep.NEW -> {
                first = entered
                step = PinStep.CONFIRM
            }
            PinStep.CONFIRM -> {
                if (entered == first) {
                    onSet(entered)
                    onDismiss()
                } else {
                    error = "PIN-kodlar mos kelmadi. Qaytadan kiriting."
                    first = ""
                    step = PinStep.NEW
                }
            }
        }
    }

    FormSheet(title = title, onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                error ?: " ",
                style = MaterialTheme.typography.bodyMedium,
                color = Brand.expense,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            PinDots(filled = pin.length, error = error != null)
            Spacer(Modifier.height(24.dp))
            PinPad(
                onDigit = { onDigit(it) },
                onDelete = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
            )
        }
    }
}
