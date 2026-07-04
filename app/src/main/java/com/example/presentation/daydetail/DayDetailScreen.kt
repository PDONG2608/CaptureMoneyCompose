package com.example.presentation.daydetail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
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
import com.example.presentation.camera.formatVnd
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    dateTimeMillis: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
    viewModel: DayDetailViewModel = viewModel(factory = viewModelFactory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedImagePathForDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(dateTimeMillis) {
        viewModel.sendIntent(DayDetailIntent.Init(dateTimeMillis))
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is DayDetailEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val dateStr = remember(dateTimeMillis) {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        sdf.format(Date(dateTimeMillis))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dateStr, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("day_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
            // Total Spending Summary card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL SPENT TODAY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatVnd(state.totalSpend),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("day_total_spend")
                    )
                }
            }

            Text(
                text = "EXPENSES",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (state.expenses.isEmpty() && !state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses recorded on this day.",
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
                    items(state.expenses, key = { it.id }) { expense ->
                        ExpenseDetailItem(
                            expense = expense,
                            onItemClick = { onNavigateToEdit(expense.id) },
                            onDeleteClick = { viewModel.sendIntent(DayDetailIntent.DeleteExpense(expense.id)) },
                            onImageClick = { path -> selectedImagePathForDialog = path }
                        )
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

@Composable
fun ExpenseDetailItem(
    expense: Expense,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onImageClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .testTag("expense_item_${expense.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Scanned Receipt Thumbnail if exists
                if (!expense.receiptImagePath.isNullOrEmpty()) {
                    val file = File(expense.receiptImagePath)
                    if (file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = "Scanned Receipt",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onImageClick(expense.receiptImagePath) }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Gray.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = "Missing Image")
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = expense.category.take(2).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatVnd(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.testTag("delete_button_${expense.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Expense",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
