package com.example.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import java.text.DateFormatSymbols
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDayDetail: (Long) -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
    viewModel: CalendarViewModel = viewModel(factory = viewModelFactory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CalendarEffect.NavigateToDayDetail -> {
                    onNavigateToDayDetail(effect.dateMillis)
                }
            }
        }
    }

    // Days of Week labels
    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    // Setup Calendar computations
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, state.year)
        set(Calendar.MONTH, state.month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1 = Sun, 2 = Mon ...
    val totalDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val emptyPrecedingCells = firstDayOfWeek - 1

    val monthName = DateFormatSymbols().months[state.month]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spending Calendar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("calendar_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Switcher Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.sendIntent(CalendarIntent.ChangeMonth(-1)) },
                        modifier = Modifier.testTag("prev_month_button")
                    ) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Prev month")
                    }

                    Text(
                        text = "$monthName ${state.year}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = { viewModel.sendIntent(CalendarIntent.ChangeMonth(1)) },
                        modifier = Modifier.testTag("next_month_button")
                    ) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next month")
                    }
                }
            }

            // Days of the Week Headers Row
            Row(modifier = Modifier.fillMaxWidth()) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Calendar Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("calendar_grid"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Preceding padding cells
                    items(emptyPrecedingCells) {
                        Box(modifier = Modifier.aspectRatio(1f))
                    }

                    // Active days of the month cells
                    items(totalDaysInMonth) { index ->
                        val day = index + 1
                        val spendAmount = state.dailyTotals[day] ?: 0.0

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (spendAmount > 0) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (spendAmount > 0) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.sendIntent(CalendarIntent.SelectDay(day))
                                }
                                .padding(4.dp)
                                .testTag("calendar_day_$day")
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = day.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (spendAmount > 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )

                                if (spendAmount > 0) {
                                    Text(
                                        text = formatCompactSpend(spendAmount),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Formats e.g. 150000.0 -> 150k
fun formatCompactSpend(amount: Double): String {
    return if (amount >= 1_000_000) {
        String.format(Locale.US, "%.1fM", amount / 1_000_000)
    } else if (amount >= 1_000) {
        String.format(Locale.US, "%.0fk", amount / 1_000)
    } else {
        amount.toInt().toString()
    }
}
