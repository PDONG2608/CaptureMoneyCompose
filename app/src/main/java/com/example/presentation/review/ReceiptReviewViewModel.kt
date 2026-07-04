package com.example.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class ReceiptReviewUiState(
    val photoPath: String = ""
)

sealed interface ReceiptReviewIntent {
    data class Init(val path: String) : ReceiptReviewIntent
    object Retake : ReceiptReviewIntent
    object UsePhoto : ReceiptReviewIntent
}

sealed interface ReceiptReviewEffect {
    object NavigateToCamera : ReceiptReviewEffect
    data class NavigateToAddExpense(val path: String) : ReceiptReviewEffect
}

class ReceiptReviewViewModel : ViewModel() {

    private val _state = MutableStateFlow(ReceiptReviewUiState())
    val state: StateFlow<ReceiptReviewUiState> = _state.asStateFlow()

    private val _effect = Channel<ReceiptReviewEffect>(Channel.BUFFERED)
    val effect: Flow<ReceiptReviewEffect> = _effect.receiveAsFlow()

    fun sendIntent(intent: ReceiptReviewIntent) {
        when (intent) {
            is ReceiptReviewIntent.Init -> {
                _state.update { it.copy(photoPath = intent.path) }
            }
            is ReceiptReviewIntent.Retake -> {
                // Safely delete temp image before returning
                viewModelScope.launch {
                    val path = _state.value.photoPath
                    if (path.isNotEmpty()) {
                        val file = File(path)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                    _effect.send(ReceiptReviewEffect.NavigateToCamera)
                }
            }
            is ReceiptReviewIntent.UsePhoto -> {
                viewModelScope.launch {
                    _effect.send(ReceiptReviewEffect.NavigateToAddExpense(_state.value.photoPath))
                }
            }
        }
    }
}
