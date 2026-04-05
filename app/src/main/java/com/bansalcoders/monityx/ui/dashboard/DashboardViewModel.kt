package com.bansalcoders.monityx.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bansalcoders.monityx.domain.model.*
import com.bansalcoders.monityx.domain.repository.CurrencyRepository
import com.bansalcoders.monityx.domain.repository.SubscriptionRepository
import com.bansalcoders.monityx.domain.usecase.GetHealthScoreUseCase
import com.bansalcoders.monityx.domain.usecase.GetSavingsSuggestionsUseCase
import com.bansalcoders.monityx.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val totalMonthlyCost: Double = 0.0,
    val totalYearlyCost: Double = 0.0,
    val activeCount: Int = 0,
    val upcomingPayments: List<Subscription> = emptyList(),
    val baseCurrency: String = "INR",
    val error: String? = null,

    // ── New intelligence fields ───────────────────────────────────────────────
    val healthScore: HealthScore? = null,
    /** Monthly budget cap in base currency. 0.0 = no budget set. */
    val monthlyBudget: Double = 0.0,
    /** % of budget used (0–100+). Meaningful only when monthlyBudget > 0. */
    val budgetPercentageUsed: Float = 0f,
    /** Top savings suggestions (preview; full list lives in InsightsScreen). */
    val topSavings: List<SavingsSuggestion> = emptyList(),
    val totalPotentialMonthlySavings: Double = 0.0,
    /**
     * Monthly cost of each upcoming payment, already converted to [baseCurrency].
     * Key = Subscription.id, Value = converted monthly cost.
     */
    val convertedPaymentCosts: Map<Long, Double> = emptyMap(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val currencyRepository: CurrencyRepository,
    private val preferencesManager: PreferencesManager,
    private val getHealthScoreUseCase: GetHealthScoreUseCase,
    private val getSavingsSuggestionsUseCase: GetSavingsSuggestionsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeDashboard()
    }

    private fun observeDashboard() {
        viewModelScope.launch {
            var lastFetchedCurrency = ""

            // Combine subscriptions + baseCurrency with a nested combine of the
            // two budget preferences so all four react to changes together.
            combine(
                subscriptionRepository.getActiveSubscriptions(),
                preferencesManager.baseCurrency,
                combine(
                    preferencesManager.monthlyBudget,
                    preferencesManager.budgetCurrency,
                    ::Pair,
                ),
            ) { subscriptions, baseCurrency, budgetPair ->
                Triple(subscriptions, baseCurrency, budgetPair)
            }.collect { (subscriptions, baseCurrency, budgetPair) ->
                val (rawBudget, budgetCurrency) = budgetPair

                // Refresh exchange rates when base currency changes
                if (baseCurrency != lastFetchedCurrency) {
                    lastFetchedCurrency = baseCurrency
                    runCatching { currencyRepository.refreshRates(baseCurrency) }
                }

                // Pre-fetch all needed rates once (subscription currencies + budget currency)
                val currenciesToFetch = (subscriptions.map { it.currency } + budgetCurrency).distinct()
                val rateCache = mutableMapOf<String, Double>()
                for (currency in currenciesToFetch) {
                    rateCache[currency] = runCatching {
                        currencyRepository.getExchangeRate(currency, baseCurrency)
                    }.getOrDefault(1.0)
                }

                val total = subscriptions.sumOf { sub ->
                    sub.monthlyCost * (rateCache[sub.currency] ?: 1.0)
                }

                val upcoming = subscriptions
                    .filter { it.daysUntilNextBilling() in 0..7 }
                    .sortedBy { it.nextBillingDate }
                    .take(5)

                val convertedPaymentCosts = upcoming.associate { sub ->
                    sub.id to sub.monthlyCost * (rateCache[sub.currency] ?: 1.0)
                }

                // Convert the stored budget to the current base currency.
                val budgetInBase = when {
                    rawBudget <= 0.0              -> 0.0
                    budgetCurrency == baseCurrency -> rawBudget
                    else                          -> rawBudget * (rateCache[budgetCurrency] ?: 1.0)
                }

                // Compute intelligence features
                val healthScore = runCatching {
                    getHealthScoreUseCase(baseCurrency)
                }.getOrNull()

                val allSuggestions = runCatching {
                    getSavingsSuggestionsUseCase(baseCurrency)
                }.getOrDefault(emptyList())

                val totalMonthlySavings = allSuggestions
                    .filter { it.reason != SuggestionReason.SWITCH_TO_YEARLY }
                    .sumOf { it.monthlySavings }

                val budgetPct = if (budgetInBase > 0.0) (total / budgetInBase * 100f).toFloat() else 0f

                _uiState.update {
                    it.copy(
                        isLoading                    = false,
                        totalMonthlyCost             = total,
                        totalYearlyCost              = total * 12.0,
                        activeCount                  = subscriptions.size,
                        upcomingPayments             = upcoming,
                        baseCurrency                 = baseCurrency,
                        healthScore                  = healthScore,
                        monthlyBudget                = budgetInBase,
                        budgetPercentageUsed         = budgetPct,
                        topSavings                   = allSuggestions.take(3),
                        totalPotentialMonthlySavings = totalMonthlySavings,
                        convertedPaymentCosts        = convertedPaymentCosts,
                    )
                }
            }
        }
    }

    fun refreshCurrencyRates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val baseCurrency = preferencesManager.baseCurrency.first()
            runCatching { currencyRepository.refreshRates(baseCurrency) }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
}
