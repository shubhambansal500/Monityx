package com.bansalcoders.monityx

import android.content.Context
import com.bansalcoders.monityx.domain.repository.CurrencyRepository
import com.bansalcoders.monityx.domain.usecase.ExportDataUseCase
import com.bansalcoders.monityx.domain.usecase.ScheduleNotificationsUseCase
import com.bansalcoders.monityx.ui.settings.SettingsViewModel
import com.bansalcoders.monityx.utils.PreferencesManager
import com.bansalcoders.monityx.utils.ThemeMode
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var currencyRepository: CurrencyRepository
    private lateinit var exportDataUseCase: ExportDataUseCase
    private lateinit var scheduleNotificationsUseCase: ScheduleNotificationsUseCase
    private lateinit var context: Context
    private lateinit var viewModel: SettingsViewModel

    // Mutable flows so tests can push new values
    private val baseCurrencyFlow   = MutableStateFlow("INR")
    private val themeModeFlow      = MutableStateFlow(ThemeMode.SYSTEM)
    private val notificationsFlow  = MutableStateFlow(true)
    private val reminderDaysFlow   = MutableStateFlow(3)
    private val monthlyBudgetFlow  = MutableStateFlow(0.0)
    private val budgetCurrencyFlow = MutableStateFlow("INR")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        preferencesManager        = mockk(relaxed = true)
        currencyRepository        = mockk()
        exportDataUseCase         = mockk()
        scheduleNotificationsUseCase = mockk(relaxed = true)
        context                   = mockk(relaxed = true)

        every { preferencesManager.baseCurrency }          returns baseCurrencyFlow
        every { preferencesManager.themeMode }             returns themeModeFlow
        every { preferencesManager.notificationsEnabled }  returns notificationsFlow
        every { preferencesManager.reminderDays }          returns reminderDaysFlow
        every { preferencesManager.monthlyBudget }         returns monthlyBudgetFlow
        every { preferencesManager.budgetCurrency }        returns budgetCurrencyFlow

        // Default: identity exchange rate
        coEvery { currencyRepository.getExchangeRate(any(), any()) } returns 1.0
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = SettingsViewModel(
            preferencesManager           = preferencesManager,
            currencyRepository           = currencyRepository,
            exportDataUseCase            = exportDataUseCase,
            scheduleNotificationsUseCase = scheduleNotificationsUseCase,
            context                      = context,
        )
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state reflects preferences`() = runTest {
        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("INR", state.baseCurrency)
        assertEquals(0.0, state.monthlyBudget, 0.001)
        assertEquals(0.0, state.monthlyBudgetInBaseCurrency, 0.001)
    }

    // ── Budget display with same currency ─────────────────────────────────────

    @Test
    fun `budget in same currency as base shows unchanged value`() = runTest {
        monthlyBudgetFlow.value  = 5000.0
        budgetCurrencyFlow.value = "INR"
        baseCurrencyFlow.value   = "INR"

        createViewModel()
        advanceUntilIdle()

        assertEquals(5000.0, viewModel.uiState.value.monthlyBudgetInBaseCurrency, 0.001)
    }

    // ── Budget conversion when currencies differ ──────────────────────────────

    @Test
    fun `budget set in USD converts to INR when base currency is INR`() = runTest {
        // Budget was $100, 1 USD = 83 INR → should display ₹8300
        coEvery { currencyRepository.getExchangeRate("USD", "INR") } returns 83.0
        monthlyBudgetFlow.value  = 100.0
        budgetCurrencyFlow.value = "USD"
        baseCurrencyFlow.value   = "INR"

        createViewModel()
        advanceUntilIdle()

        assertEquals(100.0 * 83.0, viewModel.uiState.value.monthlyBudgetInBaseCurrency, 0.01)
    }

    @Test
    fun `budget set in INR converts to USD when base currency changes to USD`() = runTest {
        // Budget was ₹10000, 1 INR = 0.012 USD → should display $120
        coEvery { currencyRepository.getExchangeRate("INR", "USD") } returns 0.012
        monthlyBudgetFlow.value  = 10000.0
        budgetCurrencyFlow.value = "INR"
        baseCurrencyFlow.value   = "USD"

        createViewModel()
        advanceUntilIdle()

        assertEquals(10000.0 * 0.012, viewModel.uiState.value.monthlyBudgetInBaseCurrency, 0.01)
    }

    @Test
    fun `budgetCurrency is reflected in state`() = runTest {
        budgetCurrencyFlow.value = "USD"
        createViewModel()
        advanceUntilIdle()

        assertEquals("USD", viewModel.uiState.value.budgetCurrency)
    }

    // ── Budget reacts to live currency change ─────────────────────────────────

    @Test
    fun `changing base currency live reconverts the displayed budget`() = runTest {
        coEvery { currencyRepository.getExchangeRate("USD", "INR") } returns 83.0
        coEvery { currencyRepository.getExchangeRate("USD", "EUR") } returns 0.92

        monthlyBudgetFlow.value  = 100.0
        budgetCurrencyFlow.value = "USD"
        baseCurrencyFlow.value   = "INR"

        createViewModel()
        advanceUntilIdle()

        // Initially in INR
        assertEquals(100.0 * 83.0, viewModel.uiState.value.monthlyBudgetInBaseCurrency, 0.01)

        // User switches to EUR
        baseCurrencyFlow.value = "EUR"
        advanceUntilIdle()

        assertEquals(100.0 * 0.92, viewModel.uiState.value.monthlyBudgetInBaseCurrency, 0.01)
    }

    // ── Zero budget ───────────────────────────────────────────────────────────

    @Test
    fun `zero budget gives zero converted budget regardless of currencies`() = runTest {
        coEvery { currencyRepository.getExchangeRate(any(), any()) } returns 83.0
        monthlyBudgetFlow.value  = 0.0
        budgetCurrencyFlow.value = "USD"
        baseCurrencyFlow.value   = "INR"

        createViewModel()
        advanceUntilIdle()

        assertEquals(0.0, viewModel.uiState.value.monthlyBudgetInBaseCurrency, 0.001)
    }

    // ── Exchange rate failure ─────────────────────────────────────────────────

    @Test
    fun `exchange rate failure falls back to raw budget amount`() = runTest {
        coEvery { currencyRepository.getExchangeRate(any(), any()) } throws RuntimeException("network")
        monthlyBudgetFlow.value  = 200.0
        budgetCurrencyFlow.value = "USD"
        baseCurrencyFlow.value   = "INR"

        createViewModel()
        advanceUntilIdle()

        // Falls back to raw value (rate = 1.0)
        assertEquals(200.0, viewModel.uiState.value.monthlyBudgetInBaseCurrency, 0.001)
    }

    // ── setMonthlyBudget saves currency ──────────────────────────────────────

    @Test
    fun `setMonthlyBudget also stores current baseCurrency as budgetCurrency`() = runTest {
        baseCurrencyFlow.value = "EUR"
        createViewModel()
        advanceUntilIdle()

        viewModel.setMonthlyBudget(500.0)
        advanceUntilIdle()

        coVerify { preferencesManager.setMonthlyBudget(500.0) }
        coVerify { preferencesManager.setBudgetCurrency("EUR") }
    }

    @Test
    fun `clearing budget stores zero and preserves current currency`() = runTest {
        baseCurrencyFlow.value = "INR"
        createViewModel()
        advanceUntilIdle()

        viewModel.setMonthlyBudget(0.0)
        advanceUntilIdle()

        coVerify { preferencesManager.setMonthlyBudget(0.0) }
        coVerify { preferencesManager.setBudgetCurrency("INR") }
    }
}
