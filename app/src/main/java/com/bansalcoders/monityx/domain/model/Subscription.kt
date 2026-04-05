package com.bansalcoders.monityx.domain.model

import java.time.LocalDate

/**
 * Pure domain model for a subscription.
 * No Android or persistence imports – this class belongs to the domain layer only.
 */
data class Subscription(
    val id: Long = 0L,
    val name: String,
    val providerKey: String,          // e.g. "netflix", "spotify" – used for icon lookup
    val category: Category,
    val cost: Double,
    val currency: String,             // ISO-4217 code, e.g. "USD", "EUR"
    val billingCycle: BillingCycle,
    val startDate: LocalDate,
    val nextBillingDate: LocalDate,
    val isActive: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    /** Number of people splitting this subscription (1 = just you). */
    val sharedWith: Int = 1,
) {
    /** Monthly-equivalent cost in the subscription's own currency. */
    val monthlyCost: Double get() = cost * billingCycle.perMonth

    /** Yearly-equivalent cost in the subscription's own currency. */
    val yearlyCost: Double get() = monthlyCost * 12.0

    /** Your personal share of the monthly cost when splitting with others. */
    val yourMonthlyCost: Double get() = monthlyCost / sharedWith.coerceAtLeast(1)

    /** Days until next billing date from today. */
    fun daysUntilNextBilling(): Long =
        java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), nextBillingDate)
}

/** Known provider display data used to populate the "quick-add" dropdown. */
data class ProviderInfo(
    val key: String,
    val displayName: String,
    val category: Category,
    val defaultCurrency: String = "USD",
)

/** Hardcoded list of popular subscription providers. */
val KNOWN_PROVIDERS: List<ProviderInfo> = listOf(
    ProviderInfo("netflix",        "Netflix",           Category.VIDEO),
    ProviderInfo("amazonprime",    "Amazon Prime",      Category.SHOPPING),
    ProviderInfo("spotify",        "Spotify",           Category.MUSIC),
    ProviderInfo("youtube_premium","YouTube Premium",   Category.VIDEO),
    ProviderInfo("disney_plus",    "Disney+",           Category.VIDEO),
    ProviderInfo("apple_tv",       "Apple TV+",         Category.VIDEO),
    ProviderInfo("hulu",           "Hulu",              Category.VIDEO),
    ProviderInfo("hbo_max",        "Max (HBO)",         Category.VIDEO),
    ProviderInfo("apple_music",    "Apple Music",       Category.MUSIC),
    ProviderInfo("tidal",          "Tidal",             Category.MUSIC),
    ProviderInfo("deezer",         "Deezer",            Category.MUSIC),
    ProviderInfo("xbox_gamepass",  "Xbox Game Pass",    Category.GAMING),
    ProviderInfo("playstation_now","PlayStation Plus",  Category.GAMING),
    ProviderInfo("ea_play",        "EA Play",           Category.GAMING),
    ProviderInfo("dropbox",        "Dropbox",           Category.CLOUD_STORAGE),
    ProviderInfo("google_one",     "Google One",        Category.CLOUD_STORAGE),
    ProviderInfo("icloud",         "iCloud+",           Category.CLOUD_STORAGE),
    ProviderInfo("microsoft365",   "Microsoft 365",     Category.PRODUCTIVITY),
    ProviderInfo("adobe_cc",       "Adobe CC",          Category.PRODUCTIVITY),
    ProviderInfo("notion",         "Notion",            Category.PRODUCTIVITY),
    ProviderInfo("nytimes",        "NY Times",          Category.NEWS),
    ProviderInfo("wsj",            "Wall St Journal",   Category.NEWS),
    ProviderInfo("duolingo",       "Duolingo Plus",     Category.EDUCATION),
    ProviderInfo("masterclass",    "MasterClass",       Category.EDUCATION),
    ProviderInfo("calm",           "Calm",              Category.FITNESS),
    ProviderInfo("peloton",        "Peloton",           Category.FITNESS),
    ProviderInfo("custom",         "Custom…",           Category.OTHER),
)
