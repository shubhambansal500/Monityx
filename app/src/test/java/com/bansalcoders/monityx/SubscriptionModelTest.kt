package com.bansalcoders.monityx

import com.bansalcoders.monityx.domain.model.BillingCycle
import com.bansalcoders.monityx.domain.model.Category
import com.bansalcoders.monityx.domain.model.HealthGrade
import com.bansalcoders.monityx.domain.model.Subscription
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for the [Subscription] domain model and [HealthGrade] enum.
 */
class SubscriptionModelTest {

    private fun subscription(
        cost: Double = 10.0,
        billingCycle: BillingCycle = BillingCycle.MONTHLY,
        sharedWith: Int = 1,
        isActive: Boolean = true,
    ) = Subscription(
        id              = 1L,
        name            = "Test Sub",
        providerKey     = "test",
        category        = Category.VIDEO,
        cost            = cost,
        currency        = "INR",
        billingCycle    = billingCycle,
        startDate       = LocalDate.now().minusMonths(3),
        nextBillingDate = LocalDate.now().plusDays(10),
        isActive        = isActive,
        sharedWith      = sharedWith,
    )

    // ── monthlyCost ───────────────────────────────────────────────────────────

    @Test
    fun `monthlyCost for MONTHLY billing equals cost`() {
        val sub = subscription(cost = 199.0, billingCycle = BillingCycle.MONTHLY)
        assertEquals(199.0, sub.monthlyCost, 0.001)
    }

    @Test
    fun `monthlyCost for YEARLY billing divides by 12`() {
        val sub = subscription(cost = 1200.0, billingCycle = BillingCycle.YEARLY)
        assertEquals(100.0, sub.monthlyCost, 0.001)
    }

    @Test
    fun `yearlyCost equals monthlyCost times 12`() {
        val sub = subscription(cost = 200.0, billingCycle = BillingCycle.MONTHLY)
        assertEquals(sub.monthlyCost * 12.0, sub.yearlyCost, 0.001)
    }

    // ── yourMonthlyCost ───────────────────────────────────────────────────────

    @Test
    fun `yourMonthlyCost with sharedWith=1 equals monthlyCost`() {
        val sub = subscription(cost = 300.0, sharedWith = 1)
        assertEquals(sub.monthlyCost, sub.yourMonthlyCost, 0.001)
    }

    @Test
    fun `yourMonthlyCost with sharedWith=2 is half monthlyCost`() {
        val sub = subscription(cost = 300.0, sharedWith = 2)
        assertEquals(sub.monthlyCost / 2.0, sub.yourMonthlyCost, 0.001)
    }

    @Test
    fun `yourMonthlyCost with sharedWith=3 is one-third monthlyCost`() {
        val sub = subscription(cost = 300.0, sharedWith = 3)
        assertEquals(sub.monthlyCost / 3.0, sub.yourMonthlyCost, 0.001)
    }

    @Test
    fun `yourMonthlyCost guards against sharedWith=0 (treats as 1)`() {
        val sub = subscription(cost = 300.0, sharedWith = 0)
        assertEquals(sub.monthlyCost, sub.yourMonthlyCost, 0.001)
    }

    @Test
    fun `yourMonthlyCost guards against negative sharedWith`() {
        val sub = subscription(cost = 300.0, sharedWith = -5)
        // coerceAtLeast(1) must prevent division by negative / zero
        assertTrue(sub.yourMonthlyCost >= 0.0)
    }

    // ── daysUntilNextBilling ──────────────────────────────────────────────────

    @Test
    fun `daysUntilNextBilling is positive for future date`() {
        val sub = subscription().copy(nextBillingDate = LocalDate.now().plusDays(7))
        assertTrue(sub.daysUntilNextBilling() > 0)
    }

    @Test
    fun `daysUntilNextBilling is negative for past date`() {
        val sub = subscription().copy(nextBillingDate = LocalDate.now().minusDays(3))
        assertTrue(sub.daysUntilNextBilling() < 0)
    }

    @Test
    fun `daysUntilNextBilling is zero for today`() {
        val sub = subscription().copy(nextBillingDate = LocalDate.now())
        assertEquals(0L, sub.daysUntilNextBilling())
    }

    // ── HealthGrade.fromScore() ───────────────────────────────────────────────

    @Test
    fun `HealthGrade EXCELLENT for score 100`() =
        assertEquals(HealthGrade.EXCELLENT, HealthGrade.fromScore(100))

    @Test
    fun `HealthGrade EXCELLENT for score 85`() =
        assertEquals(HealthGrade.EXCELLENT, HealthGrade.fromScore(85))

    @Test
    fun `HealthGrade GOOD for score 84`() =
        assertEquals(HealthGrade.GOOD, HealthGrade.fromScore(84))

    @Test
    fun `HealthGrade GOOD for score 70`() =
        assertEquals(HealthGrade.GOOD, HealthGrade.fromScore(70))

    @Test
    fun `HealthGrade FAIR for score 69`() =
        assertEquals(HealthGrade.FAIR, HealthGrade.fromScore(69))

    @Test
    fun `HealthGrade FAIR for score 50`() =
        assertEquals(HealthGrade.FAIR, HealthGrade.fromScore(50))

    @Test
    fun `HealthGrade POOR for score 49`() =
        assertEquals(HealthGrade.POOR, HealthGrade.fromScore(49))

    @Test
    fun `HealthGrade POOR for score 30`() =
        assertEquals(HealthGrade.POOR, HealthGrade.fromScore(30))

    @Test
    fun `HealthGrade CRITICAL for score 29`() =
        assertEquals(HealthGrade.CRITICAL, HealthGrade.fromScore(29))

    @Test
    fun `HealthGrade CRITICAL for score 0`() =
        assertEquals(HealthGrade.CRITICAL, HealthGrade.fromScore(0))
}
