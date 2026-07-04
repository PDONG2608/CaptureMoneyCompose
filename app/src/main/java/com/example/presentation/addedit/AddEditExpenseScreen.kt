package com.example.presentation.addedit

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Payments
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.ExpenseCategory
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseScreen(
    photoPath: String?,
    expenseId: Long,
    onNavigateBack: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
    viewModel: AddEditExpenseViewModel = viewModel(factory = viewModelFactory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val scrollState = rememberScrollState()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(photoPath, expenseId) {
        val path = if (photoPath == "null" || photoPath.isNullOrEmpty()) null else photoPath
        viewModel.sendIntent(AddEditExpenseIntent.Init(path, expenseId))
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is AddEditExpenseEffect.NavigateBack -> {
                    onNavigateBack()
                }
                is AddEditExpenseEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is AddEditExpenseEffect.ShowSuccess -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (state.isEditing) "Edit Expense" else "Add Expense", 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("add_edit_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
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
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Scanned Receipt Thumbnail Section
                state.receiptImagePath?.let { path ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = path,
                                contentDescription = "Scanned Receipt Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("add_edit_receipt_preview")
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Scanned Receipt Attached",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { viewModel.sendIntent(AddEditExpenseIntent.UpdateTitle(it)) },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. Family Dinner, Gas station") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Description, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors()
                )

                // Amount (VND) Input
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = { viewModel.sendIntent(AddEditExpenseIntent.UpdateAmount(it)) },
                    label = { Text("Amount (VND)") },
                    placeholder = { Text("0") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Payments, contentDescription = null) },
                    suffix = { Text("₫", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amount_input"),
                    singleLine = true
                )

                // Category Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExpenseCategory.entries.forEach { cat ->
                            val isSelected = cat.displayName == state.category
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.sendIntent(AddEditExpenseIntent.UpdateCategory(cat.displayName)) },
                                label = { Text(cat.displayName) },
                                modifier = Modifier.testTag("category_chip_${cat.name.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Date Time Picker section
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .testTag("date_picker_trigger"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Transaction Date",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDate(state.dateTimeMillis),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Text(
                            text = "Change",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save Expense Action Button
                Button(
                    onClick = { viewModel.sendIntent(AddEditExpenseIntent.SaveExpense) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("save_expense_button"),
                    enabled = !state.isSaving,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = if (state.isEditing) "Update Expense" else "Save Expense",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }

    // DatePicker Dialog UI
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.dateTimeMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.sendIntent(AddEditExpenseIntent.UpdateDateTime(it))
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("date_dialog_confirm")
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    modifier = Modifier.testTag("date_dialog_cancel")
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

fun formatDate(millis: Long): String {
    val date = Date(millis)
    val format = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    return format.format(date)
}
