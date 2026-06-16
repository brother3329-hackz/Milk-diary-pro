package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.ui.screens.*
import com.example.ui.theme.MilkDiaryTheme
import com.example.ui.viewmodel.MilkViewModel
import com.example.ui.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val app = application as MilkDiaryApplication
            val factory = ViewModelFactory(app, app.repository)
            val viewModel: MilkViewModel by viewModels { factory }

            MilkDiaryTheme {
                MainAppShell(viewModel)
            }
        }
    }
}

enum class NavigationTab(val route: String, val label: String, val icon: ImageVector) {
    Dashboard("dashboard", "Home", Icons.Default.Home),
    Ledger("ledger", "Ledger", Icons.Default.List),
    Report("report", "Reports", Icons.Default.Assessment),
    Analytics("analytics", "Analytics", Icons.Default.TrendingUp),
    Settings("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun MainAppShell(viewModel: MilkViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavigationTab.Dashboard.route

    val context = LocalContext.current

    // Observe snackbar/notifications in ViewModel
    val uiMessage = viewModel.uiMessage
    LaunchedEffect(uiMessage) {
        if (uiMessage != null) {
            Toast.makeText(context, uiMessage, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Scaffold(
        bottomBar = {
            // Hide Bottom bar when we are in Add/Edit form screen to maximize focus
            if (currentRoute != "add_edit_entry") {
                NavigationBar {
                    NavigationTab.values().forEach { tab ->
                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // Show Quick Log Floating Action Button on Dashboard & Ledger tabs
            if (currentRoute == NavigationTab.Dashboard.route || currentRoute == NavigationTab.Ledger.route) {
                FloatingActionButton(
                    onClick = {
                        viewModel.resetForm()
                        navController.navigate("add_edit_entry")
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_quick_add")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Entry")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavigationTab.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavigationTab.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAddEntry = {
                        viewModel.resetForm()
                        navController.navigate("add_edit_entry")
                    },
                    onNavigateToEditEntry = { entry ->
                        viewModel.loadEntryToForm(entry)
                        navController.navigate("add_edit_entry")
                    }
                )
            }

            composable(NavigationTab.Ledger.route) {
                LedgerScreen(
                    viewModel = viewModel,
                    onNavigateToEdit = { entry ->
                        viewModel.loadEntryToForm(entry)
                        navController.navigate("add_edit_entry")
                    }
                )
            }

            composable(NavigationTab.Report.route) {
                ReportScreen(
                    viewModel = viewModel
                )
            }

            composable(NavigationTab.Analytics.route) {
                AnalyticsScreen(
                    viewModel = viewModel
                )
            }

            composable(NavigationTab.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel
                )
            }

            composable("add_edit_entry") {
                AddEditEntryScreen(
                    viewModel = viewModel,
                    onCompleted = {
                        navController.popBackStack()
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
