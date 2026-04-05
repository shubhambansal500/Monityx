package com.bansalcoders.monityx.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bansalcoders.monityx.domain.model.HealthScore
import com.bansalcoders.monityx.domain.model.SavingsSuggestion
import com.bansalcoders.monityx.domain.usecase.GetHealthScoreUseCase
import com.bansalcoders.monityx.domain.usecase.GetSavingsSuggestionsUseCase
import com.bansalcoders.monityx.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightsUiState(
    val isLoading: Boolean = true,
    val healthScore: HealthScore? = null,
    val suggestions: List<SavingsSuggestion> = emptyList(),
    /** Sum of monthly savings from cancellation-type suggestions (not billing-switch). */
    val totalCancelMonthlySavings: Double = 0.0,
    /** Sum of yearly savings across ALL suggestion types. */
    val totalYearlySavings: Double = 0.0,
    val baseCurrency: String = "INR",
    val monthlyBudget: Double = 0.0,
    val currentMonthlySpend: Double = 0.0,
    val error: String? = null,
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val getSavingsSuggestionsUseCase: GetSavingsSuggestionsUseCase,
    private val getHealthScoreUseCase: GetHealthScoreUseCase,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val baseCurrency  = preferencesManager.baseCurrency.first()
                val monthlyBudget = preferencesManager.monthlyBudget.first()

                val healthScore = getHealthScoreUseCase(baseCurrency)
                val suggestions = getSavingsSuggestionsUseCase(baseCurrency)

                val cancelSavings = suggestions
                    .filter {
                        it.reason == com.bansalcoders.monityx.domain.model.SuggestionReason.INACTIVE ||
                        it.reason == com.bansalcoders.monityx.domain.model.SuggestionReason.DUPLICATE_CATEGORY ||
                        it.reason == com.bansalcoders.monityx.domain.model.SuggestionReason.LIKELY_UNUSED
                    }
                    .sumOf { it.monthlySavings }

                _uiState.update {
                    it.copy(
                        isLoading                 = false,
                        healthScore               = healthScore,
                        suggestions               = suggestions,
                        totalCancelMonthlySavings = cancelSavings,
                        totalYearlySavings        = suggestions.sumOf { s -> s.yearlySavings },
                        baseCurrency              = baseCurrency,
                        monthlyBudget             = monthlyBudget,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
