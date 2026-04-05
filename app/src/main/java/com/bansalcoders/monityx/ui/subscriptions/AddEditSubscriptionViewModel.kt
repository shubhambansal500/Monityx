package com.bansalcoders.monityx.ui.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bansalcoders.monityx.domain.model.*
import com.bansalcoders.monityx.domain.repository.CurrencyRepository
import com.bansalcoders.monityx.domain.repository.SubscriptionRepository
import com.bansalcoders.monityx.domain.usecase.AddSubscriptionUseCase
import com.bansalcoders.monityx.domain.usecase.UpdateSubscriptionUseCase
import com.bansalcoders.monityx.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AddEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,

    // Form fields
    val name: String = "",
    val selectedProvider: ProviderInfo? = null,
    val cost: String = "",
    val currency: String = "INR",
    val billingCycle: BillingCycle = BillingCycle.MONTHLY,
    val startDate: LocalDate = LocalDate.now(),
    val category: Category = Category.OTHER,
    val notes: String = "",
    val isActive: Boolean = true,
    val sharedWith: Int = 1,         // number of people sharing; 1 = just you

    // Validation errors
    val nameError: String? = null,
    val costError: String? = null,

    // Data
    val availableCurrencies: List<String> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class AddEditSubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val currencyRepository: CurrencyRepository,
    private val addSubscriptionUseCase: AddSubscriptionUseCase,
    private val updateSubscriptionUseCase: UpdateSubscriptionUseCase,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    private var editingId: Long? = null

    fun init(subscriptionId: Long?) {
        viewModelScope.launch {
            val baseCurrency = preferencesManager.baseCurrency.first()
            val currencies   = runCatching { currencyRepository.getSupportedCurrencies() }
                .getOrDefault(com.bansalcoders.monityx.utils.CurrencyUtils.POPULAR_CURRENCIES.map { it.first })

            _uiState.update {
                it.copy(
                    currency             = baseCurrency,
                    availableCurrencies  = currencies,
                )
            }

            if (subscriptionId != null) {
                editingId = subscriptionId
                _uiState.update { it.copy(isLoading = true) }
                val sub = subscriptionRepository.getSubscriptionById(subscriptionId)
                if (sub != null) {
                    _uiState.update {
                        it.copy(
                            isLoading     = false,
                            name          = sub.name,
                            cost          = sub.cost.toString(),
                            currency      = sub.currency,
                            billingCycle  = sub.billingCycle,
                            startDate     = sub.startDate,
                            category      = sub.category,
                            notes         = sub.notes,
                            isActive      = sub.isActive,
                            sharedWith    = sub.sharedWith,
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Subscription not found") }
                }
            }
        }
    }

    // ── Field setters ─────────────────────────────────────────────────────────

    fun onNameChange(value: String)         = _uiState.update { it.copy(name = value, nameError = null) }
    fun onCostChange(value: String)         = _uiState.update { it.copy(cost = value, costError = null) }
    fun onCurrencyChange(value: String)     = _uiState.update { it.copy(currency = value) }
    fun onBillingCycleChange(value: BillingCycle) = _uiState.update { it.copy(billingCycle = value) }
    fun onStartDateChange(value: LocalDate) = _uiState.update { it.copy(startDate = value) }
    fun onCategoryChange(value: Category)   = _uiState.update { it.copy(category = value) }
    fun onNotesChange(value: String)        = _uiState.update { it.copy(notes = value) }
    fun onActiveChange(value: Boolean)      = _uiState.update { it.copy(isActive = value) }
    fun onSharedWithChange(value: Int)      = _uiState.update { it.copy(sharedWith = value.coerceIn(1, 10)) }

    fun onProviderSelected(provider: ProviderInfo) {
        _uiState.update {
            it.copy(
                selectedProvider = provider,
                name             = if (provider.key == "custom") "" else provider.displayName,
                category         = provider.category,
            )
        }
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    fun save() {
        val state = _uiState.value

        // Client-side validation
        val nameError = if (state.name.isBlank()) "Name is required" else null
        val costError = if (state.cost.toDoubleOrNull()?.let { it > 0 } != true) "Enter a valid amount" else null

        if (nameError != null || costError != null) {
            _uiState.update { it.copy(nameError = nameError, costError = costError) }
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val subscription = Subscription(
                id              = editingId ?: 0L,
                name            = state.name.trim(),
                providerKey     = state.selectedProvider?.key ?: "custom",
                category        = state.category,
                cost            = state.cost.toDouble(),
                currency        = state.currency,
                billingCycle    = state.billingCycle,
                startDate       = state.startDate,
                nextBillingDate = AddSubscriptionUseCase.calculateNextBillingDate(state.startDate, state.billingCycle),
                isActive        = state.isActive,
                notes           = state.notes,
                sharedWith      = state.sharedWith,
            )

            val result = if (editingId == null) {
                val addResult = addSubscriptionUseCase(AddSubscriptionUseCase.Params(subscription))
                if (addResult is AddSubscriptionUseCase.Result.Error)
                    Result.failure(Exception(addResult.message))
                else Result.success(Unit)
            } else {
                updateSubscriptionUseCase(subscription)
            }

            result.fold(
                onSuccess = { _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) } },
                onFailure = { e -> _uiState.update { it.copy(isSaving = false, error = e.message) } },
            )
        }
    }
}
