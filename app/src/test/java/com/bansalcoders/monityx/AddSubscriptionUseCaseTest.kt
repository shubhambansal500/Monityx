package com.bansalcoders.monityx

import com.bansalcoders.monityx.domain.model.BillingCycle
import com.bansalcoders.monityx.domain.model.Category
import com.bansalcoders.monityx.domain.model.Subscription
import com.bansalcoders.monityx.domain.repository.SubscriptionRepository
import com.bansalcoders.monityx.domain.usecase.AddSubscriptionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class AddSubscriptionUseCaseTest {

    private lateinit var repository: SubscriptionRepository
    private lateinit var useCase: AddSubscriptionUseCase

    private val validSubscription = Subscription(
        name         = "Netflix",
        providerKey  = "netflix",
        category     = Category.VIDEO,
        cost         = 15.99,
        currency     = "USD",
        billingCycle = BillingCycle.MONTHLY,
        startDate    = LocalDate.now().minusMonths(3),
        nextBillingDate = LocalDate.now().plusDays(10),
    )

    @Before
    fun setup() {
        repository = mockk()
        useCase    = AddSubscriptionUseCase(repository)
    }

    @Test
    fun `invoke with valid subscription returns Success`() = runTest {
        coEvery { repository.addSubscription(any()) } returns 1L

        val result = useCase(AddSubscriptionUseCase.Params(validSubscription))

        assertTrue(result is AddSubscriptionUseCase.Result.Success)
        assertEquals(1L, (result as AddSubscriptionUseCase.Result.Success).id)
        coVerify(exactly = 1) { repository.addSubscription(any()) }
    }

    @Test
    fun `invoke with blank name returns Error`() = runTest {
        val sub = validSubscription.copy(name = "  ")

        val result = useCase(AddSubscriptionUseCase.Params(sub))

        assertTrue(result is AddSubscriptionUseCase.Result.Error)
        coVerify(exactly = 0) { repository.addSubscription(any()) }
    }

    @Test
    fun `invoke with zero cost returns Error`() = runTest {
        val sub = validSubscription.copy(cost = 0.0)

        val result = useCase(AddSubscriptionUseCase.Params(sub))

        assertTrue(result is AddSubscriptionUseCase.Result.Error)
    }

    @Test
    fun `invoke with negative cost returns Error`() = runTest {
        val sub = validSubscription.copy(cost = -5.0)

        val result = useCase(AddSubscriptionUseCase.Params(sub))

        assertTrue(result is AddSubscriptionUseCase.Result.Error)
    }

    @Test
    fun `invoke with invalid currency code returns Error`() = runTest {
        val sub = validSubscription.copy(currency = "US")  // too short

        val result = useCase(AddSubscriptionUseCase.Params(sub))

        assertTrue(result is AddSubscriptionUseCase.Result.Error)
    }

    @Test
    fun `invoke trims whitespace from name`() = runTest {
        coEvery { repository.addSubscription(any()) } returns 1L
        val sub = validSubscription.copy(name = "  Netflix  ")

        useCase(AddSubscriptionUseCase.Params(sub))

        coVerify { repository.addSubscription(match { it.name == "Netflix" }) }
    }

    @Test
    fun `calculateNextBillingDate always returns future date`() {
        val past = LocalDate.now().minusYears(2)
        val result = AddSubscriptionUseCase.calculateNextBillingDate(past, BillingCycle.MONTHLY)

        assertTrue(result.isAfter(LocalDate.now()))
    }

    @Test
    fun `calculateNextBillingDate for yearly cycle advances by year`() {
        val past = LocalDate.now().minusMonths(6)
        val result = AddSubscriptionUseCase.calculateNextBillingDate(past, BillingCycle.YEARLY)

        assertTrue(result.isAfter(LocalDate.now()))
        assertTrue(result.isBefore(LocalDate.now().plusYears(2)))
    }

    @Test
    fun `repository exception propagates as Error`() = runTest {
        coEvery { repository.addSubscription(any()) } throws RuntimeException("DB error")

        val result = useCase(AddSubscriptionUseCase.Params(validSubscription))

        assertTrue(result is AddSubscriptionUseCase.Result.Error)
        assertEquals("DB error", (result as AddSubscriptionUseCase.Result.Error).message)
    }
}
