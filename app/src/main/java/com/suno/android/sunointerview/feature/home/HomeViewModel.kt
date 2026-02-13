package com.suno.android.sunointerview.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.suno.android.sunointerview.domain.model.Song
import com.suno.android.sunointerview.domain.usecase.GetSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed class HomeState {
    data object Loading : HomeState()
    data class Success(val songs: Flow<PagingData<Song>>) : HomeState()
    data class Error(val message: String) : HomeState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getSongsUseCase: GetSongsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeState>(HomeState.Loading)
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    init {
        loadSongs()
    }

    private fun loadSongs() {
        try {
            val songFlow = getSongsUseCase()
                .cachedIn(viewModelScope)
            _uiState.value = HomeState.Success(songFlow)
        } catch (e: Exception) {
            _uiState.value = HomeState.Error(e.message ?: "Unknown error")
        }
    }
}
