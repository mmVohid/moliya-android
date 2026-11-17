package uz.moliya.app

import androidx.compose.ui.unit.Density
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.WindowManager
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
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            android.content.IntentFilter(Intent.ACTION_SCREEN_OFF),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        if (vm.settings.reminders) Reminders.schedule(applicationContext)
        if (vm.settings.autoBackup) AutoBackup.schedule(applicationContext)
        handleIntent(intent)
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
            DisposableEffect(settings.secureScreen) {
                if (settings.secureScreen) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                onDispose { }
            }
            val baseDensity = LocalDensity.current
            val factor = when (settings.textScale) {
                1 -> 1.15f
                2 -> 1.3f
                else -> 1f
            }
            CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, baseDensity.fontScale * factor)) {
            MoliyaTheme(dark = dark, dynamic = settings.dynamicColor) {
                if (vm.locked) {
                    LockScreen(
                        verify = vm::verifyPin,
                        biometricEnabled = settings.biometric,
                        onUnlock = vm::unlock,
                    )
                } else if (!settings.onboarded) {
                    SetupScreen(
                        settings = settings,
                        onReminders = vm::setReminders,
                        onSecureScreen = vm::setSecureScreen,
                        onSetPin = vm::setPin,
                        verifyPin = vm::verifyPin,
                        onClearPin = vm::clearPin,
                        onPickFolder = vm::setBackupFolder,
                        onAutoBackup = vm::setAutoBackup,
                        onFinish = vm::completeOnboarding,
                    )
                } else {
                    MoliyaApp(vm)
                }
            }
            }
        }
    }

    private val screenOffReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (vm.settings.lockOnScreenOff) vm.lock()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: IllegalArgumentException) {
            // ro'yxatdan o'tmagan
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.getStringExtra(EXTRA_QUICK)) {
            "expense" -> vm.requestQuickAdd(false)
            "income" -> vm.requestQuickAdd(true)
        }
        intent?.removeExtra(EXTRA_QUICK)
    }

    override fun onStart() {
        super.onStart()
        vm.refreshSettings()
        // Fonda sozlamadagi vaqtdan ortiq turgan bo'lsa, qayta qulflaymiz
        if (stoppedAt != 0L && SystemClock.elapsedRealtime() - stoppedAt > vm.settings.lockDelaySec * 1000L) vm.lock()
    }

    companion object {
        const val EXTRA_QUICK = "quick"
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
    TabItem("Jamg'arma", Icons.Rounded.Paid),
    TabItem("Qarzlar", Icons.Rounded.CreditCard),
    TabItem("Backup", Icons.Rounded.Backup),
)

@Composable
fun MoliyaApp(vm: FinanceViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showReport by rememberSaveable { mutableStateOf(false) }
    var showNet by rememberSaveable { mutableStateOf(false) }
    var showYear by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val data = vm.data

    val hapticRef = LocalHapticFeedback.current
    val notify: (String) -> Unit = { message ->
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }
    val undoable: (String, () -> Unit) -> Unit = { message, restore ->
        hapticRef.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = message,
                actionLabel = "Qaytarish",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) restore()
        }
    }

    val undoableAction: (String, String, () -> Unit) -> Unit = { message, label, action ->
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(message = message, actionLabel = label, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) action()
        }
    }

    LaunchedEffect(vm.quickAdd) {
        if (vm.quickAdd != 0) {
            tab = 1
            showSettings = false
            showReport = false
            showNet = false
            showYear = false
        }
    }

    BackHandler(enabled = showSettings || showReport || showNet || showYear) {
        showSettings = false
        showReport = false
        showNet = false
        showYear = false
    }

    if (showSettings || showReport || showNet || showYear) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                if (showSettings) {
                    SettingsScreen(
                        vm = vm,
                        onOpenBackup = {
                            showSettings = false
                            tab = 4
                        },
                        onBack = { showSettings = false },
                    )
                } else if (showReport) {
                    ReportScreen(
                        data = data,
                        onBack = { showReport = false },
                        onSaveReflection = vm::saveReflection,
                        onSetBudget = vm::setBudget,
                    )
                } else if (showNet) {
                    NetWorthScreen(
                        data = data,
                        hidden = data.hideBalance,
                        onBack = { showNet = false },
                        onAddWallet = vm::addWallet,
                        onDeleteWallet = { vm.deleteWallet(it.id) },
                        onTransfer = vm::addTransfer,
                    )
                } else {
                    YearScreen(data = data, onBack = { showYear = false })
                }
            }
        }
        return
    }

    val density = LocalDensity.current
    var navHeight by remember { mutableStateOf(80.dp) }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            CompositionLocalProvider(LocalBottomInset provides navHeight) {
                when (tab) {
                    0 -> HomeScreen(
                        data = data,
                        onOpenTab = { tab = it },
                        onToggleHide = { vm.setHideBalance(!data.hideBalance) },
                        onOpenSettings = { showSettings = true },
                        lastBackupAt = vm.lastBackupAt,
                    userName = vm.settings.userName,
                    cards = vm.settings.homeCards,
                    backupReminder = vm.settings.backupReminder,
                        autoBackupOn = vm.settings.autoBackup && vm.settings.backupFolder.isNotEmpty(),
                        firstEntryAt = vm.settings.firstEntryAt,
                        onQuickAdd = { income ->
                            vm.requestQuickAdd(income)
                        },
                        onOpenReport = { showReport = true },
                        onOpenNetWorth = { showNet = true },
                        onOpenYear = { showYear = true },
                    )
                    1 -> CashFlowScreen(
                        data = data,
                        quickAdd = vm.quickAdd,
                        onQuickAddConsumed = vm::consumeQuickAdd,
                        onAdd = { txn ->
                            val before = data
                            vm.addTxn(txn)
                            warnIfOverLimit(before, txn, notify)
                            if (txn.isIncome && before.saveRate > 0) {
                                val part = txn.amount * before.saveRate / 100
                                if (part > 0 && (before.monthlyNeed > 0 || before.goals.isNotEmpty())) {
                                    undoableAction(
                                        "Jamg'armaga ${formatMoney(part)} ajrating (${before.saveRate}%)",
                                        "Ajratish",
                                    ) {
                                        val entry = CushionEntry(Ids.next(), part, "Daromaddan ajratma", Dates.today())
                                        val goal = before.goals.firstOrNull { it.remaining > 0 }
                                        if (before.monthlyNeed > 0) vm.addCushion(entry)
                                        else if (goal != null) vm.addGoalEntry(goal.id, entry)
                                    }
                                }
                            }
                        },
                        onUpdate = vm::updateTxn,
                        onDelete = { txn ->
                            vm.deleteTxn(txn.id)
                            undoable("Yozuv o'chirildi") { vm.restoreTxn(txn) }
                        },
                        onSaveLimits = vm::setLimits,
                        onAddTemplate = vm::addTemplate,
                        onDeleteTemplate = vm::deleteTemplate,
                    )
                    2 -> SavingsHost(
                        data = data,
                        onPlan = vm::setPlan,
                        onAddCushion = vm::addCushion,
                        onDeleteCushion = { e ->
                            vm.deleteCushion(e.id)
                            undoable("Yozuv o'chirildi") { vm.restoreCushion(e) }
                        },
                        onAddGoal = vm::addGoal,
                        onDeleteGoal = { g ->
                            vm.deleteGoal(g.id)
                            undoable("\"${g.name}\" o'chirildi") { vm.restoreGoal(g) }
                        },
                        onAddGoalEntry = vm::addGoalEntry,
                        onDeleteGoalEntry = { goalId, e ->
                            vm.deleteGoalEntry(goalId, e.id)
                            undoable("Yozuv o'chirildi") { vm.restoreGoalEntry(goalId, e) }
                        },
                    )
                    3 -> DebtsScreen(
                        data = data,
                        onAddDebt = vm::addDebt,
                        onDeleteDebt = { debt ->
                            vm.deleteDebt(debt.id)
                            undoable("\"${debt.name}\" qarzi o'chirildi") { vm.restoreDebt(debt) }
                        },
                        onPay = vm::addPayment,
                        onDeletePayment = { debtId, payment ->
                            vm.deletePayment(debtId, payment.id)
                            undoable("To'lov o'chirildi") { vm.restorePayment(debtId, payment) }
                        },
                    )
                    else -> BackupScreen(
                        data = data,
                        onBackedUp = vm::markBackup,
                        onRestore = vm::replaceData,
                        notify = notify,
                    )
                }
            }
            Box(Modifier.align(Alignment.BottomCenter)) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
            modifier = Modifier.onSizeChanged { navHeight = with(density) { it.height.toDp() } },
        ) {
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
            }
            SnackbarHost(
                snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = navHeight),
            )
        }
    }
}

/** Yangi xarajat oylik umumiy limitdan oshirsa, xabar beradi (faqat joriy oy uchun). */
private fun warnIfOverLimit(before: FinanceData, txn: Txn, notify: (String) -> Unit) {
    if (txn.isIncome) return
    val limit = before.limits[""] ?: return
    val start = Dates.monthStart(0)
    val end = Dates.monthStart(1)
    if (txn.date < start || txn.date >= end) return
    val spentBefore = before.expense(start, end)
    val spentAfter = spentBefore + txn.amount
    if (spentAfter > limit && spentBefore <= limit) {
        notify("Oylik limit oshdi: ${formatMoney(spentAfter)} / ${formatMoney(limit)}")
    } else if (spentAfter > limit) {
        notify("Limitdan ${formatMoney(spentAfter - limit)} ortiq")
    } else if (spentAfter * 10 >= limit * 8 && spentBefore * 10 < limit * 8) {
        notify("Oylik limitning 80% dan oshdi")
    }
}
