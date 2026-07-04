package com.example.presentation.history

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.domain.model.Expense
import com.example.domain.model.ExpenseCategory
import com.example.presentation.camera.formatVnd
import com.example.presentation.daydetail.ExpenseDetailItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
    viewModel: ExpenseHistoryViewModel = viewModel(factory = viewModelFactory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedImagePathForDialog by remember { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    val categories = remember {
        listOf("All") + ExpenseCategory.values().map { it.displayName }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("history_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(imageVector = Icons.Default.SwapVert, contentDescription = "Sort Options")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    viewModel.sendIntent(HistoryIntent.ChangeSort(option))
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.sendIntent(HistoryIntent.UpdateSearch(it)) },
                placeholder = { Text("Search transactions...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            )

            // Category Filter Scroll Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { categoryName ->
                    val isSelected = state.selectedCategory == categoryName
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.sendIntent(HistoryIntent.FilterByCategory(categoryName)) },
                        label = { Text(categoryName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Text(
                text = "TRANSACTIONS (${state.filteredExpenses.size})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (state.filteredExpenses.isEmpty() && !state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No matching transactions found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.filteredExpenses, key = { it.id }) { expense ->
                        // Show transaction item with date header
                        val dateString = remember(expense.dateTimeMillis) {
                            val sdf = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
                            sdf.format(Date(expense.dateTimeMillis))
                        }
                        
                        Column {
                            Text(
                                text = dateString,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            ExpenseDetailItem(
                                expense = expense,
                                onItemClick = { onNavigateToEdit(expense.id) },
                                onDeleteClick = { viewModel.sendIntent(HistoryIntent.DeleteExpense(expense.id)) },
                                onImageClick = { path -> selectedImagePathForDialog = path }
                            )
                        }
                    }
                }
            }
        }
    }

    // Receipt Image Zoom Dialog
    selectedImagePathForDialog?.let { path ->
        Dialog(onDismissRequest = { selectedImagePathForDialog = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = File(path),
                        contentDescription = "Scanned Receipt Full View",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
