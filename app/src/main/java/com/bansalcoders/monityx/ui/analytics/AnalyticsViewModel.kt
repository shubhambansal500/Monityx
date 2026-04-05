package com.bansalcoders.monityx.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bansalcoders.monityx.domain.model.AnalyticsData
import com.bansalcoders.monityx.domain.usecase.GetAnalyticsUseCase
import com.bansalcoders.monityx.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val data: AnalyticsData? = null,
    val timeFilter: GetAnalyticsUseCase.TimeFilter = GetAnalyticsUseCase.TimeFilter.ONE_YEAR,
    val baseCurrency: String = "INR",
    val error: String? = null,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getAnalyticsUseCase: GetAnalyticsUseCase,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _timeFilter = MutableStateFlow(GetAnalyticsUseCase.TimeFilter.ONE_YEAR)
    private val _uiState    = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                _timeFilter,
                preferencesManager.baseCurrency,
            ) { filter, currency -> Pair(filter, currency) }
                .collect { (filter, currency) ->
                    _uiState.update { it.copy(isLoading = true, timeFilter = filter, baseCurrency = currency) }
                    try {
                        val data = getAnalyticsUseCase(baseCurrency = currency, timeFilter = filter)
                        _uiState.update { it.copy(isLoading = false, data = data, error = null) }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                }
        }
    }

    fun setTimeFilter(filter: GetAnalyticsUseCase.TimeFilter) {
        _timeFilter.value = filter
    }
}
