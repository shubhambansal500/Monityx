package com.bansalcoders.monityx.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bansalcoders.monityx.domain.repository.CurrencyRepository
import com.bansalcoders.monityx.domain.usecase.ExportDataUseCase
import com.bansalcoders.monityx.domain.usecase.ScheduleNotificationsUseCase
import com.bansalcoders.monityx.utils.PreferencesManager
import com.bansalcoders.monityx.utils.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val baseCurrency: String = "INR",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val reminderDays: Int = 3,
    val isExporting: Boolean = false,
    val exportedFile: File? = null,
    val exportError: String? = null,
    /** Raw budget amount as stored, in [budgetCurrency]. 0.0 = not set. */
    val monthlyBudget: Double = 0.0,
    /** The ISO-4217 code the budget was entered in. */
    val budgetCurrency: String = "INR",
    /**
     * Budget converted to [baseCurrency].
     * Always use this value for display and comparison — it is equal to
     * [monthlyBudget] when [budgetCurrency] == [baseCurrency].
     */
    val monthlyBudgetInBaseCurrency: Double = 0.0,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val currencyRepository: CurrencyRepository,
    private val exportDataUseCase: ExportDataUseCase,
    private val scheduleNotificationsUseCase: ScheduleNotificationsUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Build base state from the 5 simple preferences (no conversion needed here).
            combine(
                preferencesManager.baseCurrency,
                preferencesManager.themeMode,
                preferencesManager.notificationsEnabled,
                preferencesManager.reminderDays,
                preferencesManager.monthlyBudget,
            ) { args ->
                @Suppress("UNCHECKED_CAST")
                SettingsUiState(
                    baseCurrency         = args[0] as String,
                    themeMode            = args[1] as ThemeMode,
                    notificationsEnabled = args[2] as Boolean,
                    reminderDays         = args[3] as Int,
                    monthlyBudget        = args[4] as Double,
                )
            }
            // Chain with budgetCurrency so that any change to either the stored
            // budget, the stored budget-currency, or the selected base currency
            // re-triggers the conversion.
            .combine(preferencesManager.budgetCurrency) { state, budgetCurrency ->
                val converted = convertBudgetToBase(
                    rawBudget      = state.monthlyBudget,
                    budgetCurrency = budgetCurrency,
                    baseCurrency   = state.baseCurrency,
                )
                state.copy(
                    budgetCurrency              = budgetCurrency,
                    monthlyBudgetInBaseCurrency = converted,
                )
            }
            .collect { state -> _uiState.value = state }
        }
    }

    /** Converts [rawBudget] from [budgetCurrency] to [baseCurrency]. Returns [rawBudget] on error. */
    private suspend fun convertBudgetToBase(
        rawBudget: Double,
        budgetCurrency: String,
        baseCurrency: String,
    ): Double {
        if (rawBudget <= 0.0) return 0.0
        if (budgetCurrency == baseCurrency) return rawBudget
        return runCatching {
            currencyRepository.getExchangeRate(budgetCurrency, baseCurrency) * rawBudget
        }.getOrDefault(rawBudget)
    }

    fun setBaseCurrency(currency: String) {
        viewModelScope.launch { preferencesManager.setBaseCurrency(currency) }
    }

    fun setMonthlyBudget(budget: Double) {
        viewModelScope.launch {
            preferencesManager.setMonthlyBudget(budget)
            // Record which currency this budget was entered in so it can be
            // converted correctly when the user later changes base currency.
            preferencesManager.setBudgetCurrency(_uiState.value.baseCurrency)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesManager.setThemeMode(mode) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNotificationsEnabled(enabled)
            scheduleNotificationsUseCase.schedule(
                reminderDays          = _uiState.value.reminderDays,
                notificationsEnabled  = enabled,
            )
        }
    }

    fun setReminderDays(days: Int) {
        viewModelScope.launch {
            preferencesManager.setReminderDays(days)
            if (_uiState.value.notificationsEnabled) {
                scheduleNotificationsUseCase.schedule(
                    reminderDays         = days,
                    notificationsEnabled = true,
                )
            }
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportedFile = null, exportError = null) }
            when (val result = exportDataUseCase(ExportDataUseCase.Format.CSV)) {
                is ExportDataUseCase.Result.Success ->
                    _uiState.update { it.copy(isExporting = false, exportedFile = result.file) }
                is ExportDataUseCase.Result.Error ->
                    _uiState.update { it.copy(isExporting = false, exportError = result.message) }
            }
        }
    }

    fun exportPdf() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportedFile = null, exportError = null) }
            when (val result = exportDataUseCase(ExportDataUseCase.Format.PDF)) {
                is ExportDataUseCase.Result.Success ->
                    _uiState.update { it.copy(isExporting = false, exportedFile = result.file) }
                is ExportDataUseCase.Result.Error ->
                    _uiState.update { it.copy(isExporting = false, exportError = result.message) }
            }
        }
    }

    fun clearExportResult() {
        _uiState.update { it.copy(exportedFile = null, exportError = null) }
    }
}
