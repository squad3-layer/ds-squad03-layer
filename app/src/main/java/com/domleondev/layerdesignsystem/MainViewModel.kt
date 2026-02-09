package com.domleondev.layerdesignsystem

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.domleondev.designsystem.domain.repository.RemoteConfigRepository
import com.domleondev.designsystem.domain.usecase.RenderScreenUseCase
import com.domleondev.designsystem.presentation.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val renderScreenUseCase: RenderScreenUseCase,
    private val repository: RemoteConfigRepository
) : ViewModel() {
    
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState


    fun loadScreen(key: String) {
        _uiState.value = UiState.Loading
        repository.fetchScreenConfig(key) { json ->
            val screenDefinition = renderScreenUseCase(json)
            if (screenDefinition != null) {
                _uiState.value = UiState.Success(screenDefinition)
            } else {
                _uiState.value = UiState.Error("Erro de processamento")
            }
        }
    }
}