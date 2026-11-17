package uz.moliya.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch

/** BiometricPrompt FragmentActivity talab qiladi (u ComponentActivity'ning avlodi, Compose ishlayveradi). */
class MainActivity : FragmentActivity() {

    private val vm: FinanceViewModel by viewModels()
    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (vm.settings.reminders) Reminders.schedule(applicationContext)
        setContent {
            val settings = vm.settings
            val dark = when (settings.theme) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
                )
                onDispose { }
            }
            MoliyaTheme(dark = dark) {
                if (vm.locked) {
                    LockScreen(
                        verify = vm::verifyPin,
                        biometricEnabled = settings.biometric,
                        onUnlock = vm::unlock,
                    )
                } else {
                    MoliyaApp(vm)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Fonda 15 soniyadan ortiq turgan bo'lsa, qayta qulflaymiz
        if (stoppedAt != 0L && SystemClock.elapsedRealtime() - stoppedAt > 15_000L) vm.lock()
        vm.applyRecurring()
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = SystemClock.elapsedRealtime()
    }
}

private data class TabItem(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("Bosh", Icons.Rounded.Home),
    TabItem("Pul oqimi", Icons.Rounded.SwapVert),
    TabItem("Yostiq", Icons.Rounded.Shield),
    TabItem("Qarzlar", Icons.Rounded.CreditCard),
    TabItem("Backup", Icons.Rounded.Backup),
)

@Composable
fun MoliyaApp(vm: FinanceViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val data = vm.data

    val notify: (String) -> Unit = { message -> scope.launch { snackbar.showSnackbar(message) } }

    BackHandler(enabled = showSettings) { showSettings = false }

    if (showSettings) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                SettingsScreen(
                    settings = vm.settings,
                    verifyPin = vm::verifyPin,
                    onSetPin = vm::setPin,
                    onClearPin = vm::clearPin,
                    onBiometric = vm::setBiometric,
                    onTheme = vm::setTheme,
                    onReminders = vm::setReminders,
                    onBack = { showSettings = false },
                )
            }
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = {
                            Text(
                                item.label,
                                maxLines = 1,
                                fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Medium,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> HomeScreen(
                    data = data,
                    onOpenTab = { tab = it },
                    onToggleHide = { vm.setHideBalance(!data.hideBalance) },
                    onOpenSettings = { showSettings = true },
                )
                1 -> CashFlowScreen(
                    data = data,
                    onAdd = { txn ->
                        val before = data
                        vm.addTxn(txn)
                        warnIfOverLimit(before, txn, notify)
                    },
                    onDelete = vm::deleteTxn,
                    onAddRecurring = vm::addRecurring,
                    onDeleteRecurring = vm::deleteRecurring,
                    onSaveLimits = vm::setLimits,
                )
                2 -> CushionScreen(
                    data = data,
                    onPlan = vm::setPlan,
                    onAdd = vm::addCushion,
                    onDelete = vm::deleteCushion,
                )
                3 -> DebtsScreen(
                    data = data,
                    onAddDebt = vm::addDebt,
                    onDeleteDebt = vm::deleteDebt,
                    onPay = vm::addPayment,
                    onDeletePayment = vm::deletePayment,
                )
                else -> BackupScreen(
                    data = data,
                    onBackedUp = vm::markBackup,
                    onRestore = vm::replaceData,
                    notify = notify,
                )
            }
        }
    }
}

/** Yangi xarajat kategoriya limitidan oshirsa, xabar beradi (faqat joriy oy uchun). */
private fun warnIfOverLimit(before: FinanceData, txn: Txn, notify: (String) -> Unit) {
    if (txn.isIncome) return
    val limit = before.limits[txn.category] ?: return
    val start = Dates.monthStart(0)
    val end = Dates.monthStart(1)
    if (txn.date < start || txn.date >= end) return
    val spentBefore = before.categorySpent(txn.category, start, end)
    val spentAfter = spentBefore + txn.amount
    if (spentAfter > limit && spentBefore <= limit) {
        notify("\"${txn.category}\" limiti oshdi: ${formatMoney(spentAfter)} / ${formatMoney(limit)}")
    } else if (spentAfter > limit) {
        notify("\"${txn.category}\" limitdan ${formatMoney(spentAfter - limit)} ortiq")
    } else if (spentAfter * 10 >= limit * 8 && spentBefore * 10 < limit * 8) {
        notify("\"${txn.category}\" limitining 80% dan oshdi")
    }
}
