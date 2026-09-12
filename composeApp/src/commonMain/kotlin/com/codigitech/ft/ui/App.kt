package com.codigitech.ft.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.codigitech.ft.platform.AppLockManager
import com.codigitech.ft.platform.AppSettings
import com.codigitech.ft.platform.RecurringSyncCoordinator
import com.codigitech.ft.ui.accounts.AccountDetailScreen
import com.codigitech.ft.ui.accounts.AccountsScreen
import com.codigitech.ft.ui.categories.CategoriesScreen
import com.codigitech.ft.ui.dashboard.DashboardScreen
import com.codigitech.ft.ui.insights.InsightsScreen
import com.codigitech.ft.ui.lock.LockScreen
import com.codigitech.ft.ui.navigation.Routes
import com.codigitech.ft.ui.recurring.RecurringScreen
import com.codigitech.ft.ui.settings.SettingsScreen
import com.codigitech.ft.ui.theme.AppIcons
import com.codigitech.ft.ui.theme.FinanceTheme
import com.codigitech.ft.ui.theme.Motion
import com.codigitech.ft.ui.theme.Motion.popEnter
import com.codigitech.ft.ui.theme.Motion.popExit
import com.codigitech.ft.ui.theme.Motion.pushEnter
import com.codigitech.ft.ui.theme.Motion.pushExit
import com.codigitech.ft.ui.transactions.EditTransactionScreen
import com.codigitech.ft.ui.transactions.TransactionListScreen
import kotlinx.coroutines.launch
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.DASHBOARD, "Home", Icons.Default.Home),
    Tab(Routes.TRANSACTIONS, "History", Icons.AutoMirrored.Filled.List),
    Tab(Routes.INSIGHTS, "Insights", AppIcons.BarChart),
    Tab(Routes.ACCOUNTS, "Accounts", AppIcons.Wallet),
    Tab(Routes.SETTINGS, "Settings", Icons.Default.Settings),
)

@Composable
fun App() {
    KoinContext {
        val settings = koinInject<AppSettings>()
        val lock = koinInject<AppLockManager>()
        val sync = koinInject<RecurringSyncCoordinator>()
        val themeMode by settings.themeMode.collectAsStateWithLifecycle()
        val locked by lock.locked.collectAsStateWithLifecycle()

        val scope = rememberCoroutineScope()
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) { lock.onBackground() }
        // ON_START fires on first launch and every return from the background, so a rule that fell
        // due while the app sat in the task switcher overnight is still posted without a relaunch.
        LifecycleEventEffect(Lifecycle.Event.ON_START) { scope.launch { sync.sync() } }

        FinanceTheme(themeMode) {
            Box(Modifier.fillMaxSize()) {
                MainNavigation()
                // Crossfade rather than a hard swap so unlocking reveals the app instead of popping it in.
                Crossfade(locked, animationSpec = tween(Motion.MEDIUM), label = "lock") { isLocked ->
                    if (isLocked) LockScreen()
                }
            }
        }
    }
}

@Composable
private fun MainNavigation() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = Routes.isTab(currentRoute)

    // Inner screens own their own Scaffold (and status-bar inset); this outer one only hosts the bottom bar.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(showBar, enter = Motion.barEnter, exit = Motion.barExit) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = {
                when {
                    Routes.isEditor(targetState.destination.route) -> Motion.sheetEnter
                    Routes.isTab(initialState.destination.route) && Routes.isTab(targetState.destination.route) -> Motion.tabEnter
                    else -> pushEnter()
                }
            },
            exitTransition = {
                when {
                    Routes.isEditor(targetState.destination.route) -> Motion.tabExit
                    Routes.isTab(initialState.destination.route) && Routes.isTab(targetState.destination.route) -> Motion.tabExit
                    else -> pushExit()
                }
            },
            popEnterTransition = { if (Routes.isEditor(initialState.destination.route)) Motion.tabEnter else popEnter() },
            popExitTransition = { if (Routes.isEditor(initialState.destination.route)) Motion.sheetExit else popExit() },
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onAddTransaction = { navController.navigate(Routes.newTransaction()) },
                    onOpenTransaction = { id, transferId -> navController.navigate(Routes.editTransaction(id, transferId)) },
                    onOpenAccount = { navController.navigate(Routes.accountDetail(it)) },
                    onOpenAccounts = { navController.switchTab(Routes.ACCOUNTS) },
                    onOpenInsights = { navController.switchTab(Routes.INSIGHTS) },
                    onOpenRecurring = { navController.navigate(Routes.RECURRING) },
                    onSeeAll = { navController.switchTab(Routes.TRANSACTIONS) },
                )
            }
            composable(
                Routes.TRANSACTIONS,
                arguments = listOf(navArgument("accountId") { type = NavType.LongType; defaultValue = -1L }),
            ) { entry ->
                val accountId = entry.arguments?.read { getLong("accountId") }?.takeIf { it >= 0 }
                TransactionListScreen(
                    presetAccountId = accountId,
                    onAddTransaction = { navController.navigate(Routes.newTransaction()) },
                    onOpenTransaction = { id, transferId -> navController.navigate(Routes.editTransaction(id, transferId)) },
                )
            }
            composable(Routes.INSIGHTS) { InsightsScreen() }
            composable(Routes.ACCOUNTS) {
                AccountsScreen(onOpenAccount = { navController.navigate(Routes.accountDetail(it)) })
            }
            composable(Routes.ACCOUNT_DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.read { getLong("id") } ?: return@composable
                AccountDetailScreen(
                    accountId = id,
                    onBack = { navController.popBackStack() },
                    onAddTransaction = { navController.navigate(Routes.newTransaction(id)) },
                    onOpenTransaction = { txnId, transferId -> navController.navigate(Routes.editTransaction(txnId, transferId)) },
                )
            }
            composable(
                Routes.EDIT_TRANSACTION,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("transferId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("accountId") { type = NavType.LongType; defaultValue = -1L },
                ),
            ) { entry ->
                val args = entry.arguments
                EditTransactionScreen(
                    transactionId = args?.read { getLong("id") }?.takeIf { it >= 0 },
                    transferId = args?.read { getString("transferId") }?.takeIf { it.isNotEmpty() },
                    presetAccountId = args?.read { getLong("accountId") }?.takeIf { it >= 0 },
                    onDone = { navController.popBackStack() },
                    settled = transition.currentState == EnterExitState.Visible,
                )
            }
            composable(Routes.CATEGORIES) { CategoriesScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.RECURRING) { RecurringScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                    onOpenRecurring = { navController.navigate(Routes.RECURRING) },
                )
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
