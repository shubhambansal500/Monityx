package com.bansalcoders.monityx.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.bansalcoders.monityx.ui.analytics.AnalyticsScreen
import com.bansalcoders.monityx.ui.dashboard.DashboardScreen
import com.bansalcoders.monityx.ui.insights.InsightsScreen
import com.bansalcoders.monityx.ui.settings.PrivacyPolicyScreen
import com.bansalcoders.monityx.ui.settings.SettingsScreen
import com.bansalcoders.monityx.ui.subscriptions.AddEditSubscriptionScreen
import com.bansalcoders.monityx.ui.subscriptions.SubscriptionListScreen
import com.bansalcoders.monityx.ui.theme.NeonGreen
import com.bansalcoders.monityx.ui.theme.Outline
import com.bansalcoders.monityx.ui.theme.SurfaceVariant

/**
 * Root navigation host — premium dark bottom bar with neon-green active indicator.
 */
@Composable
fun SubscriptionManagerNavHost() {
    val navController = rememberNavController()

    Scaffold(
        containerColor    = MaterialTheme.colorScheme.background,
        // Hand inset responsibility to each child screen's own Scaffold so
        // the status-bar top padding is applied once, not doubled.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val showBottomBar = bottomNavItems.any { item ->
                currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
            }

            if (showBottomBar) {
                PremiumNavBar(
                    items              = bottomNavItems,
                    currentDestination = currentDestination,
                    onItemClick        = { item ->
                        val selected = currentDestination?.hierarchy
                            ?.any { it.route == item.screen.route } == true
                        if (!selected) {
                            if (item.screen.route == Screen.Dashboard.route) {
                                navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                            } else {
                                navController.navigate(item.screen.route) {
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            }
                        }
                    },
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Dashboard.route,
            modifier         = Modifier.padding(innerPadding),
            enterTransition  = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) },
            exitTransition   = { fadeOut(tween(180)) + slideOutHorizontally(tween(180)) },
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onAddSubscription   = { navController.navigate(Screen.AddSubscription.route) },
                    onViewSubscriptions = { navController.navigate(Screen.Subscriptions.route) },
                    onViewInsights      = { navController.navigate(Screen.Insights.route) },
                )
            }

            composable(Screen.Insights.route) {
                InsightsScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.Subscriptions.route) {
                SubscriptionListScreen(
                    onAddSubscription  = { navController.navigate(Screen.AddSubscription.route) },
                    onEditSubscription = { id ->
                        navController.navigate(Screen.EditSubscription.createRoute(id))
                    },
                )
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) },
                )
            }

            composable(Screen.AddSubscription.route) {
                AddEditSubscriptionScreen(
                    subscriptionId = null,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(
                route     = Screen.EditSubscription.route,
                arguments = listOf(navArgument("subscriptionId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("subscriptionId")
                AddEditSubscriptionScreen(
                    subscriptionId = id,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Screen.PrivacyPolicy.route) {
                PrivacyPolicyScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}

// ── Premium bottom navigation bar ─────────────────────────────────────────────

@Composable
private fun PremiumNavBar(
    items: List<BottomNavItem>,
    currentDestination: androidx.navigation.NavDestination?,
    onItemClick: (BottomNavItem) -> Unit,
) {
    // navigationBarsPadding() adds exactly the system navigation-bar height so the
    // pill floats above the gesture bar / 3-button bar on every device.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Outline, RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = currentDestination?.hierarchy
                    ?.any { it.route == item.screen.route } == true

                NavBarItem(
                    item     = item,
                    selected = selected,
                    onClick  = { onItemClick(item) },
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor   = if (selected) NeonGreen.copy(alpha = 0.12f) else Color.Transparent
    val iconTint  = if (selected) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant
    val textColor = if (selected) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant

    TextButton(
        onClick = onClick,
        shape   = RoundedCornerShape(16.dp),
        colors  = ButtonDefaults.textButtonColors(containerColor = bgColor),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector        = item.icon,
                contentDescription = item.contentDescription,
                tint               = iconTint,
                modifier           = Modifier.size(22.dp),
            )
            Text(
                text      = item.label,
                fontSize  = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color     = textColor,
                letterSpacing = 0.3.sp,
            )
            // Active dot
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(NeonGreen),
                )
            } else {
                Spacer(Modifier.size(4.dp))
            }
        }
    }
}
