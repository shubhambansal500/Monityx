package com.bansalcoders.monityx

import com.bansalcoders.monityx.domain.model.BillingCycle
import com.bansalcoders.monityx.domain.model.Category
import com.bansalcoders.monityx.domain.model.HealthGrade
import com.bansalcoders.monityx.domain.model.Subscription
import com.bansalcoders.monityx.domain.repository.CurrencyRepository
import com.bansalcoders.monityx.domain.repository.SubscriptionRepository
import com.bansalcoders.monityx.domain.usecase.GetHealthScoreUseCase
import com.bansalcoders.monityx.utils.PreferencesManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class GetHealthScoreUseCaseTest {

    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var currencyRepository: CurrencyRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var useCase: GetHealthScoreUseCase

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun sub(
        id: Long = 1L,
        name: String = "Sub$id",
        category: Category = Category.VIDEO,
        billingCycle: BillingCycle = BillingCycle.MONTHLY,
        cost: Double = 100.0,
        currency: String = "INR",
        isActive: Boolean = true,
        startDate: LocalDate = LocalDate.now().minusMonths(1),
    ) = Subscription(
        id              = id,
        name            = name,
        providerKey     = "key$id",
        category        = category,
        cost            = cost,
        currency        = currency,
        billingCycle    = billingCycle,
        startDate       = startDate,
        nextBillingDate = LocalDate.now().plusDays(10),
        isActive        = isActive,
    )

    @Before
    fun setup() {
        subscriptionRepository = mockk()
        currencyRepository     = mockk()
        preferencesManager     = mockk(relaxed = true)

        // Default: no budget set, same budget currency as base, identity exchange rate
        every { preferencesManager.monthlyBudget }  returns flowOf(0.0)
        every { preferencesManager.budgetCurrency } returns flowOf("INR")
        coEvery { currencyRepository.getExchangeRate(any(), any()) } returns 1.0
    }

    private fun givenSubs(vararg subs: Subscription) {
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(subs.toList())
    }

    // ── Perfect score ─────────────────────────────────────────────────────────

    @Test
    fun `perfect portfolio gives score 100 and EXCELLENT grade`() = runTest {
        givenSubs(sub(id = 1, category = Category.VIDEO))
        useCase = GetHealthScoreUseCase(subscriptionRepository, currencyRepository, preferencesManager)

        val result = useCase("INR")

        assertEquals(100, result.score)
        assertEquals(HealthGrade.EXCELLENT, result.grade)
        assertTrue(result.breakdown.isEmpty())
    }

    // ── Inactive subscriptions ────────────────────────────────────────────────

    @Test
    fun `one inactive subscription deducts 8 points`() = runTest {
        givenSubs(sub(id = 1, isActive = false))
        useCase = GetHealthScoreUseCase(subscriptionRepository, currencyRepository, preferencesManager)

        val result = useCase("INR")

        assertEquals(92, result.score)
        assertEquals(1, result.breakdown.size)
        assertEquals(8, result.breakdown.first().points)
    }

    @Test
    fun `three inactive subscriptions deducts 24 points (cap)`() = runTest {
        givenSubs(
            sub(id = 1, isActive = false),
            sub(id = 2, isActive = false),
            sub(id = 3, isActive = false),
        )
        useCase = GetHealthScoreUseCase(subscriptionRepository, currencyRepository, preferencesManager)

        val result = useCase("INR")

        assertEquals(76, result.score)
        assertEquals(24, result.breakdown.first().points)
    }

    @Test
    fun `four inactive subscriptions still capped at 24 deduction`() = runTest {
        givenSubs(
            sub(id = 1, isActive = false),
            sub(id = 2, isActive = false),
            sub(id = 3, isActive = false),
            sub(id = 4, isActive = false),
        )
        useCase = GetHealthScoreUseCase(subscriptionRepository, currencyRepository, preferencesManager)

        val result = useCase("INR")

        assertEquals(76, result.score)
    }

    // ── Duplicate categories ──────────────────────────────────────────────────

    @Test
    fun `two subscriptions in same category deducts 5 points`() = runTest {
        givenSubs(
            sub(id = 1, category = Category.VIDEO),
            sub(id = 2, category = Category.VIDEO),
        )
        useCase = GetHealthScoreUseCase(subscriptionRepository, currencyRepository, preferencesManager)

        val result = useCase("INR")

        assertEquals(95, result.score)
    }

    @Test
    fun `duplicates in different categories deducts 5 each`() = runTest {
        givenSubs(
            sub(id = 1, category = Category.VIDEO),
            sub(id = 2, category = Category.VIDEO),
            sub(id = 3, category = Category.MUSIC),
            sub(id = 4, category = Category.MUSIC),
        )
        useCase = GetHealthScoreUseCase(subscriptionRepository, currencyRepository, preferencesManager)

        val result = useCase("INR")

        // 2 extra slots → 2 × 5 = 10 deducted
        assertEquals(90, result.score)
    }

    @Test
    fun `duplicate deduction is capped at 20 points`() = runTest {
        // 5 active subs in same category → 4 extra → 4×5=20, but cap is 20
        val subs = (1..5).map { sub(id = it.toLong(), category = Category.VIDEO) }
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(subs)
        useCase = GetHealthScoreUseCase(subscriptionRepository, currencyRepository, preferencesManager)

        val result = useCase("INR")

        val dupDeduction = result.breakdown.find { "Overlapping" in it.reason }
        assertNotNull(dupDeduction)
        assertEquals(20, dupDeduction!!.points)
    }

    // ── Budget checks ─────────────────────────────────────────────────────────

    @Test
    fun `budget exceeded deducts 20 points`() = runTest {
        // Budget = 100, monthly spend = 200
        every { preferencesManager.monthlyBudget } returns flowOf(100.0)
        givenSubs(sub(id = 1, cost = 200.0))
        useCase = GetHealthScoreUseCase(subscriptionRepository, currencyRepository, preferencesManager)

        val result = useCase("INR")

        val budgetDeduction = result.breakdown.find { "exceeded" in it.reason }
        assertNotNull(budgetDeduction)
        assertEquals(20, budgetDeduction!!.points)
    }

    @Test
    fun `budget at 80 percent deducts 10 points`() = runTest {
        // Budget = 100, monthly spend = 85 (85%)
        every { preferencesManager.monthlyBudget } returns flowOf(100.0)
        givenSubs(sub(id = 1, cost = 85.0))
        useCase = GetHealthScoreUseCase(subscriptionRepository, currencyRepository, preferencesManager)

        val result = useCase("INR")

        val budgetDeduction = result.breakdown.find { "%" in it.reason }
        assertNotNull(budgetDeduction)
        assertEquals(10, budgetDeduction!!.points)
    }

    @Test
    fun `no budget set means no budget deduction`() = runTest {
        every { preferencesManager.monthlyBudget } returns flowOf(0.0)
        givenSubs(sub(id = 1, cost = 9999.0))
        useCase = GetHealthScoreUseCase(subscriptionRepository, currencyRepository, preferencesManager)

        val result = useCase("INR")

        assertTrue(result.breakdown.none { "budget" in it.reason.lowercase() })
    }

    // ── Unused yearly subscriptions ───────────────────────────────────────────

    @Test
    fun `yearly sub started over 6 months ago deducts 6 points`() = runTest {
        givenSubs(
            sub(
                id          = 1,
                billingCycle = BillingCycle.YEARLY,
                startDate   = LocalDate.now().minusMonths(8),
            )
        )
        useCase = GetHealthScoreUseCase(subscriptionRepository, currencyRepository, preferencesManager)

        val result = useCase("INR")

        val unusedDeduction = result.breakdown.find { "yearly" in it.reason.lowercase() }
        assertNotNull(unusedDeduction)
        assertEquals(6, unusedDeduction!!.points)
    }

    @Test
    fun `yearly sub started 3 months ago does NOT deduct points`() = runTest {
        givenSubs(
            sub(
                id          = 1,
                billingCycle = BillingCycle.YEARLY,
                startDate   = LocalDate.now().minusMonths(3),
            )
        )
        useCase = GetHealthScoreUseCase(subscriptionRepository, currencyRepository, preferencesManager)

        val result = useCase("INR")

        assertEquals(100, result.score)
    }

    @Test
    fun `unused yearly deduction capped at 18 points`() = runTest {
        val subs = (1..4).map {
            sub(
                id          = it.toLong(),
                billingCycle = BillingCycle.YEARLY,
                startDate   = LocalDate.now().minusMonths(12),
            )
        }
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(subs)
        useCase = GetHealthScoreUseCase(subscriptionRepository, currencyRepository, preferencesManager)

        val result = useCase("INR")

        val unusedDeduction = result.breakdown.find { "yearly" in it.reason.lowercase() }
        assertNotNull(unusedDeduction)
        assertEquals(18, unusedDeduction!!.points)
    }

    // ── Score always in 0–100 ─────────────────────────────────────────────────

    @Test
    fun `score never goes below 0 even with many deductions`() = runTest {
        // 3 inactive (-24) + 4 duplicates (-20) + exceeded budget (-20) + 3 unused yearly (-18)
        every { preferencesManager.monthlyBudget } returns flowOf(10.0)
        val subs = listOf(
            sub(id = 1, isActive = false),
            sub(id = 2, isActive = false),
            sub(id = 3, isActive = false),
            sub(id = 4, category = Category.VIDEO),
            sub(id = 5, category = Category.VIDEO),
            sub(id = 6, category = Category.VIDEO),
            sub(id = 7, category = Category.VIDEO),
            sub(id = 8, billingCycle = BillingCycle.YEARLY, startDate = LocalDate.now().minusMonths(12)),
            sub(id = 9, billingCycle = BillingCycle.YEARLY, startDate = LocalDate.now().minusMonths(12)),
            sub(id = 10, billingCycle = BillingCycle.YEARLY, startDate = LocalDate.now().minusMonths(12)),
        )
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(subs)
        useCase = GetHealthScoreUseCase(subscriptionRepository, currencyRepository, preferencesManager)

        val result = useCase("INR")

        assertTrue(result.score >= 0)
    }

    // ── Budget currency conversion ────────────────────────────────────────────

    @Test
    fun `budget set in USD is converted to INR before comparing with INR totals`() = runTest {
        // Budget: $100 USD, rate 1 USD = 83 INR → budgetInBase = ₹8300
        // Subscription: ₹9000/mo → ratio = 9000/8300 > 1.0 → EXCEEDED
        every { preferencesManager.monthlyBudget }  returns flowOf(100.0)
        every { preferencesManager.budgetCurrency } returns flowOf("USD")
        coEvery { currencyRepository.getExchangeRate("USD", "INR") } returns 83.0
        givenSubs(sub(id = 1, cost = 9000.0, currency = "INR"))
        useCase = GetHealthScoreUseCase(subscriptionRepository, currencyRepository, preferencesManager)

        val result = useCase("INR")

        val budgetDeduction = result.breakdown.find { "exceeded" in it.reason }
        assertNotNull("Expected budget-exceeded deduction", budgetDeduction)
        assertEquals(20, budgetDeduction!!.points)
    }

    @Test
    fun `budget set in USD is NOT exceeded when converted total is within limit`() = runTest {
        // Budget: $200 USD @ 83 = ₹16600; spend = ₹1000 → well under
        every { preferencesManager.monthlyBudget }  returns flowOf(200.0)
        every { preferencesManager.budgetCurrency } returns flowOf("USD")
        coEvery { currencyRepository.getExchangeRate("USD", "INR") } returns 83.0
        givenSubs(sub(id = 1, cost = 1000.0, currency = "INR"))
        useCase = GetHealthScoreUseCase(subscriptionRepository, currencyRepository, preferencesManager)

        val result = useCase("INR")

        assertTrue(result.breakdown.none { "budget" in it.reason.lowercase() })
    }

    // ── Currency exchange fallback ─────────────────────────────────────────────

    @Test
    fun `exchange rate exception falls back to 1 and does not crash`() = runTest {
        every { preferencesManager.monthlyBudget } returns flowOf(100.0)
        coEvery { currencyRepository.getExchangeRate(any(), any()) } throws RuntimeException("Network error")
        givenSubs(sub(id = 1, cost = 150.0))
        useCase = GetHealthScoreUseCase(subscriptionRepository, currencyRepository, preferencesManager)

        // Should not throw
        val result = useCase("INR")
        assertNotNull(result)
    }
}
