package com.bansalcoders.monityx.ui.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bansalcoders.monityx.domain.model.Category
import com.bansalcoders.monityx.domain.model.Subscription
import com.bansalcoders.monityx.domain.repository.CurrencyRepository
import com.bansalcoders.monityx.domain.usecase.DeleteSubscriptionUseCase
import com.bansalcoders.monityx.domain.usecase.GetSubscriptionsUseCase
import com.bansalcoders.monityx.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionListUiState(
    val isLoading: Boolean = true,
    val subscriptions: List<Subscription> = emptyList(),
    val query: String = "",
    val sortOrder: GetSubscriptionsUseCase.SortOrder = GetSubscriptionsUseCase.SortOrder.NEXT_BILLING,
    val filterCategory: String? = null,
    val error: String? = null,
    val deleteConfirmId: Long? = null,  // subscription awaiting delete confirmation
    /** User's selected base currency (e.g. "INR"). */
    val baseCurrency: String = "INR",
    /**
     * Per-subscription monthly cost converted to [baseCurrency].
     * Key = Subscription.id, Value = monthly cost in base currency.
     */
    val convertedMonthlyCosts: Map<Long, Double> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SubscriptionListViewModel @Inject constructor(
    private val getSubscriptionsUseCase: GetSubscriptionsUseCase,
    private val deleteSubscriptionUseCase: DeleteSubscriptionUseCase,
    private val currencyRepository: CurrencyRepository,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _filterState = MutableStateFlow(
        GetSubscriptionsUseCase.Params()
    )
    private val _uiState = MutableStateFlow(SubscriptionListUiState())
    val uiState: StateFlow<SubscriptionListUiState> = _uiState.asStateFlow()

    val availableCategories: List<String> =
        Category.entries.map { it.label }.sorted()

    init {
        viewModelScope.launch {
            combine(
                _filterState.flatMapLatest { params -> getSubscriptionsUseCase(params) },
                preferencesManager.baseCurrency,
            ) { subscriptions, baseCurrency ->
                Pair(subscriptions, baseCurrency)
            }.collect { (subscriptions, baseCurrency) ->

                // Pre-fetch exchange rates once per currency change so that
                // the pure lookups in associate{} below are non-suspend.
                val distinctCurrencies = subscriptions.map { it.currency }.distinct()
                val rateCache = mutableMapOf<String, Double>()
                for (currency in distinctCurrencies) {
                    rateCache[currency] = try {
                        currencyRepository.getExchangeRate(currency, baseCurrency)
                    } catch (_: Exception) { 1.0 }
                }

                val convertedMonthlyCosts = subscriptions.associate { sub ->
                    sub.id to sub.monthlyCost * (rateCache[sub.currency] ?: 1.0)
                }

                _uiState.update {
                    it.copy(
                        isLoading             = false,
                        subscriptions         = subscriptions,
                        baseCurrency          = baseCurrency,
                        convertedMonthlyCosts = convertedMonthlyCosts,
                        error                 = null,
                    )
                }
            }
        }
    }

    fun onSearchQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        _filterState.update { it.copy(query = query) }
    }

    fun onSortOrder(order: GetSubscriptionsUseCase.SortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
        _filterState.update { it.copy(sortOrder = order) }
    }

    fun onFilterCategory(category: String?) {
        _uiState.update { it.copy(filterCategory = category) }
        _filterState.update { it.copy(filterCategory = category) }
    }

    fun showDeleteConfirm(id: Long) {
        _uiState.update { it.copy(deleteConfirmId = id) }
    }

    fun dismissDeleteConfirm() {
        _uiState.update { it.copy(deleteConfirmId = null) }
    }

    fun confirmDelete() {
        val id = _uiState.value.deleteConfirmId ?: return
        viewModelScope.launch {
            deleteSubscriptionUseCase(id).onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
            _uiState.update { it.copy(deleteConfirmId = null) }
        }
    }
}
