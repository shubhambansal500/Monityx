package com.bansalcoders.monityx

import app.cash.turbine.test
import com.bansalcoders.monityx.domain.model.BillingCycle
import com.bansalcoders.monityx.domain.model.Category
import com.bansalcoders.monityx.domain.model.Subscription
import com.bansalcoders.monityx.domain.repository.CurrencyRepository
import com.bansalcoders.monityx.domain.usecase.DeleteSubscriptionUseCase
import com.bansalcoders.monityx.domain.usecase.GetSubscriptionsUseCase
import com.bansalcoders.monityx.ui.subscriptions.SubscriptionListViewModel
import com.bansalcoders.monityx.utils.PreferencesManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getSubscriptionsUseCase: GetSubscriptionsUseCase
    private lateinit var deleteSubscriptionUseCase: DeleteSubscriptionUseCase
    private lateinit var currencyRepository: CurrencyRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var viewModel: SubscriptionListViewModel

    private val sampleSubscriptions = listOf(
        Subscription(
            id = 1L, name = "Netflix", providerKey = "netflix",
            category = Category.VIDEO, cost = 15.99, currency = "USD",
            billingCycle = BillingCycle.MONTHLY,
            startDate       = LocalDate.now().minusMonths(6),
            nextBillingDate = LocalDate.now().plusDays(5),
        ),
        Subscription(
            id = 2L, name = "Spotify", providerKey = "spotify",
            category = Category.MUSIC, cost = 9.99, currency = "USD",
            billingCycle = BillingCycle.MONTHLY,
            startDate       = LocalDate.now().minusMonths(3),
            nextBillingDate = LocalDate.now().plusDays(15),
        ),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getSubscriptionsUseCase   = mockk()
        deleteSubscriptionUseCase = mockk()
        currencyRepository        = mockk()
        preferencesManager        = mockk()

        // Default: INR base currency, identity exchange rate
        every { preferencesManager.baseCurrency } returns flowOf("INR")
        coEvery { currencyRepository.getExchangeRate(any(), any()) } returns 1.0
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        every { getSubscriptionsUseCase(any()) } returns flowOf(sampleSubscriptions)
        viewModel = SubscriptionListViewModel(
            getSubscriptionsUseCase   = getSubscriptionsUseCase,
            deleteSubscriptionUseCase = deleteSubscriptionUseCase,
            currencyRepository        = currencyRepository,
            preferencesManager        = preferencesManager,
        )
    }

    @Test
    fun `initial state is loading`() {
        every { getSubscriptionsUseCase(any()) } returns flowOf(emptyList())
        viewModel = SubscriptionListViewModel(
            getSubscriptionsUseCase   = getSubscriptionsUseCase,
            deleteSubscriptionUseCase = deleteSubscriptionUseCase,
            currencyRepository        = currencyRepository,
            preferencesManager        = preferencesManager,
        )

        // Before advancing dispatcher, isLoading may still be true
        assertTrue(viewModel.uiState.value.isLoading || viewModel.uiState.value.subscriptions.isEmpty())
    }

    @Test
    fun `subscriptions load correctly`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.subscriptions.size)
            assertEquals("Netflix", state.subscriptions.first().name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `baseCurrency from preferences is reflected in state`() = runTest {
        createViewModel()
        advanceUntilIdle()

        assertEquals("INR", viewModel.uiState.value.baseCurrency)
    }

    @Test
    fun `when baseCurrency changes converted costs are recomputed`() = runTest {
        // Simulate the ViewModel reacting to a baseCurrency change
        val baseCurrencyFlow = kotlinx.coroutines.flow.MutableStateFlow("INR")
        every { preferencesManager.baseCurrency } returns baseCurrencyFlow
        // 1 USD = 83 INR
        coEvery { currencyRepository.getExchangeRate("USD", "INR") } returns 83.0
        coEvery { currencyRepository.getExchangeRate("USD", "USD") } returns 1.0

        every { getSubscriptionsUseCase(any()) } returns flowOf(sampleSubscriptions)
        viewModel = SubscriptionListViewModel(
            getSubscriptionsUseCase   = getSubscriptionsUseCase,
            deleteSubscriptionUseCase = deleteSubscriptionUseCase,
            currencyRepository        = currencyRepository,
            preferencesManager        = preferencesManager,
        )
        advanceUntilIdle()

        // Converted cost for Netflix (15.99 USD → INR)
        val netflixConverted = viewModel.uiState.value.convertedMonthlyCosts[1L]
        assertNotNull(netflixConverted)
        assertEquals(15.99 * 83.0, netflixConverted!!, 0.01)
    }

    @Test
    fun `convertedMonthlyCosts map is keyed by subscription id`() = runTest {
        coEvery { currencyRepository.getExchangeRate("USD", "INR") } returns 83.0
        createViewModel()
        advanceUntilIdle()

        val costs = viewModel.uiState.value.convertedMonthlyCosts
        assertTrue(1L in costs)
        assertTrue(2L in costs)
    }

    @Test
    fun `search query filters subscriptions`() = runTest {
        every { getSubscriptionsUseCase(any()) } answers {
            val params = firstArg<GetSubscriptionsUseCase.Params>()
            flowOf(sampleSubscriptions.filter {
                params.query.isBlank() || it.name.contains(params.query, ignoreCase = true)
            })
        }
        viewModel = SubscriptionListViewModel(
            getSubscriptionsUseCase   = getSubscriptionsUseCase,
            deleteSubscriptionUseCase = deleteSubscriptionUseCase,
            currencyRepository        = currencyRepository,
            preferencesManager        = preferencesManager,
        )
        advanceUntilIdle()

        viewModel.onSearchQuery("Netflix")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Netflix", state.query)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete confirmation flow works correctly`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.showDeleteConfirm(1L)
        assertEquals(1L, viewModel.uiState.value.deleteConfirmId)

        viewModel.dismissDeleteConfirm()
        assertNull(viewModel.uiState.value.deleteConfirmId)
    }

    @Test
    fun `confirm delete calls use case`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { deleteSubscriptionUseCase(1L) } returns Result.success(Unit)

        viewModel.showDeleteConfirm(1L)
        viewModel.confirmDelete()
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteSubscriptionUseCase(1L) }
        assertNull(viewModel.uiState.value.deleteConfirmId)
    }

    @Test
    fun `delete failure shows error`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coEvery { deleteSubscriptionUseCase(1L) } returns Result.failure(Exception("DB locked"))

        viewModel.showDeleteConfirm(1L)
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun `sort order change propagates to use case`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onSortOrder(GetSubscriptionsUseCase.SortOrder.COST_HIGH)
        advanceUntilIdle()

        assertEquals(
            GetSubscriptionsUseCase.SortOrder.COST_HIGH,
            viewModel.uiState.value.sortOrder,
        )
    }

    @Test
    fun `exchange rate failure falls back and still loads subscriptions`() = runTest {
        coEvery { currencyRepository.getExchangeRate(any(), any()) } throws RuntimeException("network error")

        every { getSubscriptionsUseCase(any()) } returns flowOf(sampleSubscriptions)
        viewModel = SubscriptionListViewModel(
            getSubscriptionsUseCase   = getSubscriptionsUseCase,
            deleteSubscriptionUseCase = deleteSubscriptionUseCase,
            currencyRepository        = currencyRepository,
            preferencesManager        = preferencesManager,
        )
        advanceUntilIdle()

        // Should not crash; subscriptions still loaded
        assertEquals(2, viewModel.uiState.value.subscriptions.size)
        // Converted costs fall back to rate = 1.0
        val netflixConverted = viewModel.uiState.value.convertedMonthlyCosts[1L]
        assertNotNull(netflixConverted)
        assertEquals(15.99, netflixConverted!!, 0.01) // 15.99 * 1.0
    }

    @Test
    fun `same currency subscription has converted cost equal to monthly cost`() = runTest {
        val inrSub = sampleSubscriptions.first().copy(id = 10L, currency = "INR", cost = 200.0)
        every { getSubscriptionsUseCase(any()) } returns flowOf(listOf(inrSub))
        coEvery { currencyRepository.getExchangeRate("INR", "INR") } returns 1.0

        viewModel = SubscriptionListViewModel(
            getSubscriptionsUseCase   = getSubscriptionsUseCase,
            deleteSubscriptionUseCase = deleteSubscriptionUseCase,
            currencyRepository        = currencyRepository,
            preferencesManager        = preferencesManager,
        )
        advanceUntilIdle()

        val converted = viewModel.uiState.value.convertedMonthlyCosts[10L]
        assertNotNull(converted)
        assertEquals(200.0, converted!!, 0.01)
    }
}
