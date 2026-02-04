package com.domleondev.designsystem.presentation.state

import com.domleondev.designsystem.domain.model.ScreenDefinition

sealed class UiState {
    object Loading : UiState()
    data class Success(val screen: ScreenDefinition) : UiState()
    data class Error(val message: String) : UiState()
}