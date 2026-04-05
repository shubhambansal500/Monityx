package com.bansalcoders.monityx

import com.bansalcoders.monityx.domain.model.BillingCycle
import com.bansalcoders.monityx.domain.model.Category
import com.bansalcoders.monityx.domain.model.InsightType
import com.bansalcoders.monityx.domain.model.Subscription
import com.bansalcoders.monityx.domain.repository.CurrencyRepository
import com.bansalcoders.monityx.domain.repository.SubscriptionRepository
import com.bansalcoders.monityx.domain.usecase.GetAnalyticsUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class GetAnalyticsUseCaseTest {

    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var currencyRepository: CurrencyRepository
    private lateinit var useCase: GetAnalyticsUseCase

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun sub(
        id: Long = 1L,
        name: String = "Sub$id",
        category: Category = Category.VIDEO,
        billingCycle: BillingCycle = BillingCycle.MONTHLY,
        cost: Double = 100.0,
        currency: String = "INR",
    ) = Subscription(
        id              = id,
        name            = name,
        providerKey     = "key$id",
        category        = category,
        cost            = cost,
        currency        = currency,
        billingCycle    = billingCycle,
        startDate       = LocalDate.now().minusMonths(3),
        nextBillingDate = LocalDate.now().plusDays(10),
    )

    @Before
    fun setup() {
        subscriptionRepository = mockk()
        currencyRepository     = mockk()
        coEvery { currencyRepository.getExchangeRate(any(), any()) } returns 1.0
        useCase = GetAnalyticsUseCase(subscriptionRepository, currencyRepository)
    }

    // ── Empty portfolio ───────────────────────────────────────────────────────

    @Test
    fun `empty subscriptions return zeroed analytics`() = runTest {
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(emptyList())

        val result = useCase()

        assertEquals(0.0, result.totalMonthlySpend, 0.001)
        assertEquals(0.0, result.totalYearlySpend, 0.001)
        assertEquals(0, result.activeCount)
        assertNull(result.mostExpensiveSubscription)
        assertNull(result.spendingSpike)
    }

    // ── Totals ────────────────────────────────────────────────────────────────

    @Test
    fun `totalMonthlySpend sums all active subscriptions`() = runTest {
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(
            listOf(
                sub(id = 1, cost = 100.0),
                sub(id = 2, cost = 200.0),
            )
        )

        val result = useCase()

        assertEquals(300.0, result.totalMonthlySpend, 0.001)
    }

    @Test
    fun `totalYearlySpend is 12 times totalMonthlySpend`() = runTest {
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(
            listOf(sub(id = 1, cost = 150.0))
        )

        val result = useCase()

        assertEquals(result.totalMonthlySpend * 12.0, result.totalYearlySpend, 0.001)
    }

    @Test
    fun `activeCount matches number of subscriptions`() = runTest {
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(
            listOf(sub(id = 1), sub(id = 2), sub(id = 3))
        )

        val result = useCase()

        assertEquals(3, result.activeCount)
    }

    // ── mostExpensiveSubscription ─────────────────────────────────────────────

    @Test
    fun `mostExpensiveSubscription identifies the costliest sub`() = runTest {
        val expensive = sub(id = 2, cost = 999.0)
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(
            listOf(sub(id = 1, cost = 100.0), expensive)
        )

        val result = useCase()

        assertEquals(expensive.id, result.mostExpensiveSubscription?.id)
    }

    // ── Monthly spend history ─────────────────────────────────────────────────

    @Test
    fun `ONE_YEAR filter produces 12 history entries`() = runTest {
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(
            listOf(sub(id = 1, cost = 50.0))
        )

        val result = useCase(timeFilter = GetAnalyticsUseCase.TimeFilter.ONE_YEAR)

        assertEquals(12, result.monthlySpendHistory.size)
    }

    @Test
    fun `SIX_MONTHS filter produces 6 history entries`() = runTest {
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(
            listOf(sub(id = 1, cost = 50.0))
        )

        val result = useCase(timeFilter = GetAnalyticsUseCase.TimeFilter.SIX_MONTHS)

        assertEquals(6, result.monthlySpendHistory.size)
    }

    @Test
    fun `ONE_MONTH filter produces 1 history entry`() = runTest {
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(
            listOf(sub(id = 1, cost = 50.0))
        )

        val result = useCase(timeFilter = GetAnalyticsUseCase.TimeFilter.ONE_MONTH)

        assertEquals(1, result.monthlySpendHistory.size)
    }

    // ── Category breakdown ────────────────────────────────────────────────────

    @Test
    fun `category breakdown includes each category present`() = runTest {
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(
            listOf(
                sub(id = 1, category = Category.VIDEO, cost = 100.0),
                sub(id = 2, category = Category.MUSIC, cost = 50.0),
            )
        )

        val result = useCase()

        val categories = result.categoryBreakdown.map { it.category }
        assertTrue(Category.VIDEO in categories)
        assertTrue(Category.MUSIC in categories)
    }

    @Test
    fun `category percentages sum to approximately 100`() = runTest {
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(
            listOf(
                sub(id = 1, category = Category.VIDEO, cost = 60.0),
                sub(id = 2, category = Category.MUSIC, cost = 40.0),
            )
        )

        val result = useCase()

        val total = result.categoryBreakdown.sumOf { it.percentage.toDouble() }
        assertEquals(100.0, total, 0.1)
    }

    @Test
    fun `category breakdown is sorted by amount descending`() = runTest {
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(
            listOf(
                sub(id = 1, category = Category.MUSIC, cost = 30.0),
                sub(id = 2, category = Category.VIDEO, cost = 200.0),
                sub(id = 3, category = Category.GAMING, cost = 80.0),
            )
        )

        val result = useCase()

        val amounts = result.categoryBreakdown.map { it.amount }
        assertEquals(amounts.sortedDescending(), amounts)
    }

    // ── Spending spike detection ──────────────────────────────────────────────

    @Test
    fun `no spending spike when only one data point`() = runTest {
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(
            listOf(sub(id = 1, cost = 100.0))
        )

        val result = useCase(timeFilter = GetAnalyticsUseCase.TimeFilter.ONE_MONTH)

        assertNull(result.spendingSpike)
    }

    @Test
    fun `no spending spike when spend is stable`() = runTest {
        // Same cost every month → no spike
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(
            listOf(sub(id = 1, cost = 100.0))
        )

        val result = useCase(timeFilter = GetAnalyticsUseCase.TimeFilter.ONE_YEAR)

        // All months have same spend → std dev = 0 → no spike
        assertNull(result.spendingSpike)
    }

    // ── AI insights ───────────────────────────────────────────────────────────

    @Test
    fun `DUPLICATE_CATEGORY insight generated for two subs in same category`() = runTest {
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(
            listOf(
                sub(id = 1, category = Category.VIDEO, cost = 100.0),
                sub(id = 2, category = Category.VIDEO, cost = 50.0),
            )
        )

        val result = useCase()

        val duplicateInsight = result.aiInsights.find { it.type == InsightType.DUPLICATE_CATEGORY }
        assertNotNull(duplicateInsight)
    }

    @Test
    fun `SAVING_TIP insight generated for monthly billing with meaningful saving`() = runTest {
        // 100/mo * 12 * 15% = 180 saving → > 5.0 threshold
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(
            listOf(sub(id = 1, billingCycle = BillingCycle.MONTHLY, cost = 100.0))
        )

        val result = useCase()

        val savingInsight = result.aiInsights.find { it.type == InsightType.SAVING_TIP }
        assertNotNull(savingInsight)
    }

    @Test
    fun `BUDGET_ALERT insight generated when total monthly spend exceeds 100`() = runTest {
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(
            listOf(
                sub(id = 1, cost = 60.0),
                sub(id = 2, cost = 60.0),
            )
        )

        val result = useCase()

        val budgetAlert = result.aiInsights.find { it.type == InsightType.BUDGET_ALERT }
        assertNotNull(budgetAlert)
    }

    @Test
    fun `no BUDGET_ALERT when spend is low`() = runTest {
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(
            listOf(sub(id = 1, cost = 10.0))
        )

        val result = useCase()

        val budgetAlert = result.aiInsights.find { it.type == InsightType.BUDGET_ALERT }
        assertNull(budgetAlert)
    }

    @Test
    fun `AI insights capped at 5`() = runTest {
        // Many conditions to trigger many insights
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(
            listOf(
                sub(id = 1, category = Category.VIDEO, cost = 200.0, billingCycle = BillingCycle.MONTHLY),
                sub(id = 2, category = Category.VIDEO, cost = 150.0, billingCycle = BillingCycle.MONTHLY),
                sub(id = 3, category = Category.MUSIC, cost = 100.0, billingCycle = BillingCycle.MONTHLY),
                sub(id = 4, category = Category.MUSIC, cost = 80.0,  billingCycle = BillingCycle.MONTHLY),
                sub(id = 5, category = Category.GAMING, cost = 60.0, billingCycle = BillingCycle.MONTHLY),
                sub(id = 6, category = Category.GAMING, cost = 50.0, billingCycle = BillingCycle.MONTHLY),
            )
        )

        val result = useCase()

        assertTrue(result.aiInsights.size <= 5)
    }

    // ── Currency conversion ───────────────────────────────────────────────────

    @Test
    fun `exchange rates are applied to totals`() = runTest {
        coEvery { currencyRepository.getExchangeRate("USD", "INR") } returns 83.0
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(
            listOf(sub(id = 1, cost = 10.0, currency = "USD"))
        )

        val result = useCase(baseCurrency = "INR")

        assertEquals(10.0 * 83.0, result.totalMonthlySpend, 0.01)
    }

    @Test
    fun `exchange rate failure falls back to rate 1 and does not crash`() = runTest {
        coEvery { currencyRepository.getExchangeRate(any(), any()) } throws RuntimeException("network")
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(
            listOf(sub(id = 1, cost = 50.0, currency = "USD"))
        )

        val result = useCase(baseCurrency = "INR")

        // Fallback rate = 1.0
        assertEquals(50.0, result.totalMonthlySpend, 0.01)
    }
}
