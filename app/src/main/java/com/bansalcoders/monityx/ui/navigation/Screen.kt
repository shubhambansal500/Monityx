package com.bansalcoders.monityx.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/** Type-safe navigation destinations for the app. */
sealed class Screen(val route: String) {

    // Bottom navigation destinations
    object Dashboard     : Screen("dashboard")
    object Subscriptions : Screen("subscriptions")
    object Analytics     : Screen("analytics")
    object Settings      : Screen("settings")

    // Intelligence screen
    object Insights : Screen("insights")

    // Detail screens
    object AddSubscription  : Screen("add_subscription")
    object EditSubscription : Screen("edit_subscription/{subscriptionId}") {
        fun createRoute(id: Long) = "edit_subscription/$id"
    }
    object PrivacyPolicy : Screen("privacy_policy")
}

/** Bottom navigation item definition. */
data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
)

val bottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.Dashboard,
        label  = "Dashboard",
        icon   = Icons.Filled.Dashboard,
        contentDescription = "Dashboard",
    ),
    BottomNavItem(
        screen = Screen.Subscriptions,
        label  = "Subscriptions",
        icon   = Icons.Filled.List,
        contentDescription = "Subscriptions",
    ),
    BottomNavItem(
        screen = Screen.Analytics,
        label  = "Analytics",
        icon   = Icons.Filled.Analytics,
        contentDescription = "Analytics",
    ),
    BottomNavItem(
        screen = Screen.Settings,
        label  = "Settings",
        icon   = Icons.Filled.Settings,
        contentDescription = "Settings",
    ),
)
