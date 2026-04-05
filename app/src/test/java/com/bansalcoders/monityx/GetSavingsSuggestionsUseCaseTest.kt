package com.bansalcoders.monityx

import com.bansalcoders.monityx.domain.model.BillingCycle
import com.bansalcoders.monityx.domain.model.Category
import com.bansalcoders.monityx.domain.model.Subscription
import com.bansalcoders.monityx.domain.model.SuggestionReason
import com.bansalcoders.monityx.domain.repository.CurrencyRepository
import com.bansalcoders.monityx.domain.repository.SubscriptionRepository
import com.bansalcoders.monityx.domain.usecase.GetSavingsSuggestionsUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class GetSavingsSuggestionsUseCaseTest {

    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var currencyRepository: CurrencyRepository
    private lateinit var useCase: GetSavingsSuggestionsUseCase

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun sub(
        id: Long = 1L,
        category: Category = Category.VIDEO,
        billingCycle: BillingCycle = BillingCycle.MONTHLY,
        cost: Double = 100.0,
        isActive: Boolean = true,
        startDate: LocalDate = LocalDate.now().minusMonths(2),
        currency: String = "INR",
    ) = Subscription(
        id              = id,
        name            = "Sub$id",
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
        coEvery { currencyRepository.getExchangeRate(any(), any()) } returns 1.0
        useCase = GetSavingsSuggestionsUseCase(subscriptionRepository, currencyRepository)
    }

    // ── Empty portfolio ───────────────────────────────────────────────────────

    @Test
    fun `empty subscription list returns empty suggestions`() = runTest {
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(emptyList())

        val result = useCase("INR")

        assertTrue(result.isEmpty())
    }

    // ── INACTIVE suggestions ──────────────────────────────────────────────────

    @Test
    fun `inactive subscription produces INACTIVE suggestion`() = runTest {
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(
            listOf(sub(id = 1, isActive = false, cost = 200.0))
        )

        val result = useCase("INR")

        assertEquals(1, result.size)
        assertEquals(SuggestionReason.INACTIVE, result.first().reason)
    }

    @Test
    fun `inactive subscription savings equals its full monthly cost`() = runTest {
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(
            listOf(sub(id = 1, isActive = false, cost = 300.0, billingCycle = BillingCycle.MONTHLY))
        )

        val result = useCase("INR")

        assertEquals(300.0, result.first().monthlySavings, 0.001)
        assertEquals(3600.0, result.first().yearlySavings, 0.001)
    }

    // ── DUPLICATE_CATEGORY suggestions ────────────────────────────────────────

    @Test
    fun `two active subs in same category produces DUPLICATE suggestion for costlier one`() = runTest {
        val cheap     = sub(id = 1, category = Category.VIDEO, cost = 50.0)
        val expensive = sub(id = 2, category = Category.VIDEO, cost = 150.0)
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(
            listOf(cheap, expensive)
        )

        val result = useCase("INR")

        // The expensive sub gets DUPLICATE_CATEGORY; the cheap sub may also appear
        // as SWITCH_TO_YEARLY (separate suggestion), so we check by subscription id + reason.
        val duplicateSuggestion = result.find { it.reason == SuggestionReason.DUPLICATE_CATEGORY }
        assertNotNull(duplicateSuggestion)
        assertEquals(expensive.id, duplicateSuggestion!!.subscription.id)
    }

    @Test
    fun `cheapest duplicate is NOT flagged as DUPLICATE_CATEGORY`() = runTest {
        val cheap     = sub(id = 1, category = Category.MUSIC, cost = 30.0)
        val expensive = sub(id = 2, category = Category.MUSIC, cost = 100.0)
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(
            listOf(cheap, expensive)
        )

        val result = useCase("INR")

        // The cheap sub should never carry a DUPLICATE_CATEGORY reason.
        val cheapDuplicate = result.find {
            it.subscription.id == cheap.id && it.reason == SuggestionReason.DUPLICATE_CATEGORY
        }
        assertNull(cheapDuplicate)
        // The expensive sub must appear (either as DUPLICATE or another reason).
        assertTrue(result.any { it.subscription.id == expensive.id })
    }

    // ── SWITCH_TO_YEARLY suggestions ──────────────────────────────────────────

    @Test
    fun `monthly sub above 3 base units produces SWITCH_TO_YEARLY suggestion`() = runTest {
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(
            listOf(sub(id = 1, billingCycle = BillingCycle.MONTHLY, cost = 100.0))
        )

        val result = useCase("INR")

        assertEquals(1, result.size)
        assertEquals(SuggestionReason.SWITCH_TO_YEARLY, result.first().reason)
    }

    @Test
    fun `monthly sub below threshold does NOT produce switch suggestion`() = runTest {
        // Cost 2.0 < threshold 3.0 → no suggestion
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(
            listOf(sub(id = 1, billingCycle = BillingCycle.MONTHLY, cost = 2.0))
        )

        val result = useCase("INR")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `yearly saving is approximately 15 percent of annual cost`() = runTest {
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(
            listOf(sub(id = 1, billingCycle = BillingCycle.MONTHLY, cost = 100.0))
        )

        val result = useCase("INR")

        val expected = 100.0 * 12.0 * 0.15
        assertEquals(expected, result.first().yearlySavings, 0.01)
    }

    // ── LIKELY_UNUSED suggestions ─────────────────────────────────────────────

    @Test
    fun `yearly sub started more than 6 months ago produces LIKELY_UNUSED suggestion`() = runTest {
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(
            listOf(
                sub(
                    id          = 1,
                    billingCycle = BillingCycle.YEARLY,
                    startDate   = LocalDate.now().minusMonths(8),
                )
            )
        )

        val result = useCase("INR")

        assertEquals(1, result.size)
        assertEquals(SuggestionReason.LIKELY_UNUSED, result.first().reason)
    }

    @Test
    fun `yearly sub started less than 6 months ago does NOT produce LIKELY_UNUSED`() = runTest {
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(
            listOf(
                sub(
                    id          = 1,
                    billingCycle = BillingCycle.YEARLY,
                    startDate   = LocalDate.now().minusMonths(3),
                )
            )
        )

        val result = useCase("INR")

        assertTrue(result.none { it.reason == SuggestionReason.LIKELY_UNUSED })
    }

    // ── Each subscription appears at most once ────────────────────────────────

    @Test
    fun `inactive sub does not also appear as LIKELY_UNUSED`() = runTest {
        // inactive + yearly + started long ago → should only be INACTIVE
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(
            listOf(
                sub(
                    id          = 1,
                    isActive    = false,
                    billingCycle = BillingCycle.YEARLY,
                    startDate   = LocalDate.now().minusMonths(12),
                )
            )
        )

        val result = useCase("INR")

        val reasons = result.map { it.reason }
        // Only one entry; INACTIVE takes priority
        assertEquals(1, result.size)
        assertTrue(SuggestionReason.INACTIVE in reasons)
        assertFalse(SuggestionReason.LIKELY_UNUSED in reasons)
    }

    @Test
    fun `duplicate category sub does not also appear as SWITCH_TO_YEARLY`() = runTest {
        // Both active, same category, one is monthly expensive → duplicate takes it
        val expensive = sub(id = 1, category = Category.VIDEO, cost = 200.0, billingCycle = BillingCycle.MONTHLY)
        val cheap     = sub(id = 2, category = Category.VIDEO, cost = 50.0,  billingCycle = BillingCycle.MONTHLY)
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(
            listOf(expensive, cheap)
        )

        val result = useCase("INR")

        val expensiveEntry = result.filter { it.subscription.id == expensive.id }
        // Expensive sub appears exactly once
        assertEquals(1, expensiveEntry.size)
        assertEquals(SuggestionReason.DUPLICATE_CATEGORY, expensiveEntry.first().reason)
    }

    // ── Sort order ────────────────────────────────────────────────────────────

    @Test
    fun `suggestions are sorted by yearly savings descending`() = runTest {
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(
            listOf(
                sub(id = 1, isActive = false, cost = 10.0),
                sub(id = 2, isActive = false, cost = 500.0),
                sub(id = 3, isActive = false, cost = 100.0),
            )
        )

        val result = useCase("INR")

        val savings = result.map { it.yearlySavings }
        assertEquals(savings.sortedDescending(), savings)
    }

    // ── Currency exchange ─────────────────────────────────────────────────────

    @Test
    fun `exchange rate is applied to convert to base currency`() = runTest {
        // 1 USD = 83 INR
        coEvery { currencyRepository.getExchangeRate("USD", "INR") } returns 83.0
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(
            listOf(sub(id = 1, isActive = false, cost = 10.0, currency = "USD"))
        )

        val result = useCase("INR")

        // Monthly savings should be 10 * 83 = 830
        assertEquals(10.0 * 83.0, result.first().monthlySavings, 0.01)
    }

    @Test
    fun `exchange rate failure falls back to 1 and does not crash`() = runTest {
        coEvery { currencyRepository.getExchangeRate(any(), any()) } throws RuntimeException("timeout")
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(
            listOf(sub(id = 1, isActive = false, cost = 100.0))
        )

        val result = useCase("INR")

        // Uses rate = 1.0 fallback
        assertEquals(100.0, result.first().monthlySavings, 0.01)
    }
}
