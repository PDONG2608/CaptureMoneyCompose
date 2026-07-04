package com.example.presentation.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptReviewScreen(
    photoPath: String,
    onNavigateToCamera: () -> Unit,
    onNavigateToAddExpense: (String) -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
    viewModel: ReceiptReviewViewModel = viewModel(factory = viewModelFactory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(photoPath) {
        viewModel.sendIntent(ReceiptReviewIntent.Init(photoPath))
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ReceiptReviewEffect.NavigateToCamera -> {
                    onNavigateToCamera()
                }
                is ReceiptReviewEffect.NavigateToAddExpense -> {
                    onNavigateToAddExpense(effect.path)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Receipt", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.sendIntent(ReceiptReviewIntent.Retake) },
                        modifier = Modifier.testTag("review_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to camera"
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
        ) {
            // Render receipt image
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.Black, shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (state.photoPath.isNotEmpty()) {
                    AsyncImage(
                        model = state.photoPath,
                        contentDescription = "Captured receipt image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("receipt_image_preview")
                    )
                } else {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            // Bottom Actions (Use photo / Retake photo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.sendIntent(ReceiptReviewIntent.Retake) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("retake_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retake", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.sendIntent(ReceiptReviewIntent.UsePhoto) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("use_photo_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Photo", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
