package com.bansalcoders.monityx.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

/**
 * Manages user preferences with Jetpack DataStore.
 * All reads are exposed as Flows so the UI reacts to changes automatically.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.dataStore

    private object Keys {
        val BASE_CURRENCY          = stringPreferencesKey("base_currency")
        val THEME_MODE             = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED  = booleanPreferencesKey("notifications_enabled")
        val REMINDER_DAYS          = intPreferencesKey("reminder_days")
        val ONBOARDING_COMPLETE    = booleanPreferencesKey("onboarding_complete")
        /** Monthly budget cap stored as a plain amount. 0.0 = not set. */
        val MONTHLY_BUDGET         = doublePreferencesKey("monthly_budget")
        /**
         * The ISO-4217 currency code the monthly budget was entered in.
         * Defaults to "INR" so existing installs without this key are treated
         * as if they set their budget in the default base currency.
         */
        val BUDGET_CURRENCY        = stringPreferencesKey("budget_currency")
    }

    // ── Reads ─────────────────────────────────────────────────────────────────

    val baseCurrency: Flow<String> = dataStore.data
        .safeCatch()
        .map { it[Keys.BASE_CURRENCY] ?: "INR" }

    val themeMode: Flow<ThemeMode> = dataStore.data
        .safeCatch()
        .map { prefs ->
            ThemeMode.fromValue(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.value)
        }

    val notificationsEnabled: Flow<Boolean> = dataStore.data
        .safeCatch()
        .map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    val reminderDays: Flow<Int> = dataStore.data
        .safeCatch()
        .map { it[Keys.REMINDER_DAYS] ?: 3 }

    val onboardingComplete: Flow<Boolean> = dataStore.data
        .safeCatch()
        .map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    /** Monthly budget cap in base currency. 0.0 means no budget is set. */
    val monthlyBudget: Flow<Double> = dataStore.data
        .safeCatch()
        .map { it[Keys.MONTHLY_BUDGET] ?: 0.0 }

    /**
     * The currency the monthly budget was entered in.
     * When this differs from [baseCurrency] the raw budget must be converted
     * before display or comparison.
     */
    val budgetCurrency: Flow<String> = dataStore.data
        .safeCatch()
        .map { it[Keys.BUDGET_CURRENCY] ?: "INR" }

    // ── Writes ────────────────────────────────────────────────────────────────

    suspend fun setBaseCurrency(currency: String) = dataStore.edit {
        it[Keys.BASE_CURRENCY] = currency
    }

    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit {
        it[Keys.THEME_MODE] = mode.value
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) = dataStore.edit {
        it[Keys.NOTIFICATIONS_ENABLED] = enabled
    }

    suspend fun setReminderDays(days: Int) = dataStore.edit {
        it[Keys.REMINDER_DAYS] = days.coerceIn(1, 14)
    }

    suspend fun setOnboardingComplete() = dataStore.edit {
        it[Keys.ONBOARDING_COMPLETE] = true
    }

    suspend fun setMonthlyBudget(budget: Double) = dataStore.edit {
        it[Keys.MONTHLY_BUDGET] = budget.coerceAtLeast(0.0)
    }

    /**
     * Records which currency the budget was entered in.
     * Must be called alongside [setMonthlyBudget] whenever the budget value changes.
     */
    suspend fun setBudgetCurrency(currency: String) = dataStore.edit {
        it[Keys.BUDGET_CURRENCY] = currency
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun Flow<Preferences>.safeCatch(): Flow<Preferences> =
        catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
}

enum class ThemeMode(val value: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system");

    companion object {
        fun fromValue(value: String): ThemeMode =
            entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}
