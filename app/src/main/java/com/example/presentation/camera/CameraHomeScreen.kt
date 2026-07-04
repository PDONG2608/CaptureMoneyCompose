package com.example.presentation.camera

import android.Manifest
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraHomeScreen(
    onNavigateToReview: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
    viewModel: CameraHomeViewModel = viewModel(factory = viewModelFactory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var cameraViewInstance by remember { mutableStateOf<Camera2PreviewView?>(null) }

    // Sync permission state with ViewModel
    LaunchedEffect(cameraPermissionState.status.isGranted) {
        viewModel.sendIntent(CameraHomeIntent.UpdatePermission(cameraPermissionState.status.isGranted))
    }

    // Refresh today's spending whenever we enter the home screen
    LaunchedEffect(Unit) {
        viewModel.sendIntent(CameraHomeIntent.LoadTodaySpend)
    }

    // Handle ViewModel effects (navigation, error)
    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CameraHomeEffect.NavigateToReview -> {
                    onNavigateToReview(effect.path)
                }
                is CameraHomeEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (state.hasCameraPermission) {
            // Render Camera2 preview
            AndroidView(
                factory = { ctx ->
                    Camera2PreviewView(ctx).also {
                        cameraViewInstance = it
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top overlay bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Today spend box (polished glassmorphism style)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = "Today's Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                        Text(
                            text = formatVnd(state.todaySpend),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.testTag("today_spend_text")
                        )
                    }
                }

                // Quick actions
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Flash action
                    IconButton(
                        onClick = {
                            cameraViewInstance?.toggleFlash { isNowOn ->
                                viewModel.sendIntent(CameraHomeIntent.ToggleFlash(isNowOn))
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .testTag("flash_toggle")
                    ) {
                        Icon(
                            imageVector = if (state.isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Toggle Flash",
                            tint = if (state.isFlashOn) Color.Yellow else Color.White
                        )
                    }

                    // Settings action
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            }

            // Bottom controls section (with capture button)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // History Button
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .testTag("history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Expense History",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Camera capture trigger
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(6.dp)
                            .clickable {
                                cameraViewInstance?.capturePhoto(
                                    onCaptured = { path ->
                                        viewModel.sendIntent(CameraHomeIntent.PhotoCaptured(path))
                                    },
                                    onError = { msg ->
                                        viewModel.sendIntent(CameraHomeIntent.CaptureError(msg))
                                    }
                                )
                            }
                            .testTag("capture_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }

                    // Calendar Button
                    IconButton(
                        onClick = onNavigateToCalendar,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .testTag("calendar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Monthly Calendar",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        } else {
            // Permission request state fallback UI
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "SnapSpend uses your camera to scan receipt photos. Please enable camera access in permissions settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("grant_permission_button")
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}

fun formatVnd(amount: Double): String {
    val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN"))
    return "${formatter.format(amount)} ₫"
}
