package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.di.ViewModelFactory
import com.example.presentation.addedit.AddEditExpenseScreen
import com.example.presentation.calendar.CalendarScreen
import com.example.presentation.camera.CameraHomeScreen
import com.example.presentation.daydetail.DayDetailScreen
import com.example.presentation.history.ExpenseHistoryScreen
import com.example.presentation.navigation.Screen
import com.example.presentation.review.ReceiptReviewScreen
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.settings.SettingsViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as SnapSpendApp).container
        val viewModelFactory = ViewModelFactory(
            useCases = appContainer.expenseUseCases,
            preferencesManager = appContainer.preferencesManager
        )

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
            val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                dynamicColor = false // Disable dynamic colors to enforce the custom palette
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Screen.CameraHome.route
                    ) {
                        // 1. Camera Home
                        composable(Screen.CameraHome.route) {
                            CameraHomeScreen(
                                onNavigateToReview = { photoPath ->
                                    navController.navigate(Screen.ReceiptReview.createRoute(photoPath))
                                },
                                onNavigateToHistory = {
                                    navController.navigate(Screen.History.route)
                                },
                                onNavigateToCalendar = {
                                    navController.navigate(Screen.Calendar.route)
                                },
                                onNavigateToSettings = {
                                    navController.navigate(Screen.Settings.route)
                                },
                                viewModelFactory = viewModelFactory
                            )
                        }

                        // 2. Receipt Review
                        composable(
                            route = Screen.ReceiptReview.route,
                            arguments = listOf(
                                navArgument("photoPath") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                }
                            )
                        ) { backStackEntry ->
                            val photoPath = backStackEntry.arguments?.getString("photoPath") ?: ""
                            ReceiptReviewScreen(
                                photoPath = photoPath,
                                onNavigateToCamera = {
                                    navController.popBackStack()
                                },
                                onNavigateToAddExpense = { path ->
                                    navController.navigate(Screen.AddEditExpense.createRoute(photoPath = path))
                                },
                                viewModelFactory = viewModelFactory
                            )
                        }

                        // 3. Add / Edit Expense
                        composable(
                            route = Screen.AddEditExpense.route,
                            arguments = listOf(
                                navArgument("photoPath") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                },
                                navArgument("expenseId") {
                                    type = NavType.LongType
                                    defaultValue = -1L
                                }
                            )
                        ) { backStackEntry ->
                            val photoPath = backStackEntry.arguments?.getString("photoPath")
                            val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: -1L
                            AddEditExpenseScreen(
                                photoPath = photoPath,
                                expenseId = expenseId,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                viewModelFactory = viewModelFactory
                            )
                        }

                        // 4. Calendar
                        composable(Screen.Calendar.route) {
                            CalendarScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToDayDetail = { dateMillis ->
                                    navController.navigate(Screen.DayDetail.createRoute(dateMillis))
                                },
                                viewModelFactory = viewModelFactory
                            )
                        }

                        // 5. Day Detail
                        composable(
                            route = Screen.DayDetail.route,
                            arguments = listOf(
                                navArgument("dateTimeMillis") {
                                    type = NavType.LongType
                                }
                            )
                        ) { backStackEntry ->
                            val dateMillis = backStackEntry.arguments?.getLong("dateTimeMillis") ?: 0L
                            DayDetailScreen(
                                dateTimeMillis = dateMillis,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToEdit = { id ->
                                    navController.navigate(Screen.AddEditExpense.createRoute(expenseId = id))
                                },
                                viewModelFactory = viewModelFactory
                            )
                        }

                        // 6. Expense History
                        composable(Screen.History.route) {
                            ExpenseHistoryScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToEdit = { id ->
                                    navController.navigate(Screen.AddEditExpense.createRoute(expenseId = id))
                                },
                                viewModelFactory = viewModelFactory
                            )
                        }

                        // 7. Settings
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                viewModelFactory = viewModelFactory
                            )
                        }
                    }
                }
            }
        }
    }
}
