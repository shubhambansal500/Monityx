package com.bansalcoders.monityx.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bansalcoders.monityx.domain.model.InsightType
import com.bansalcoders.monityx.domain.usecase.GetAnalyticsUseCase
import com.bansalcoders.monityx.ui.components.*
import com.bansalcoders.monityx.ui.theme.NeonGreen
import com.bansalcoders.monityx.ui.theme.Outline
import com.bansalcoders.monityx.ui.theme.SemanticAmber
import com.bansalcoders.monityx.ui.theme.SemanticBlue
import com.bansalcoders.monityx.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor      = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Analytics",
                        fontWeight = FontWeight.Black,
                        color      = MaterialTheme.colorScheme.onBackground,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingScreen(modifier = Modifier.padding(innerPadding))

            uiState.data == null || uiState.data!!.activeCount == 0 ->
                EmptyScreen(
                    message  = "Add subscriptions to see analytics",
                    modifier = Modifier.padding(innerPadding),
                )

            else -> {
                val data = uiState.data!!

                LazyColumn(
                    modifier       = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {

                    // ── Time filter ───────────────────────────────────────────
                    item {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, Outline, RoundedCornerShape(14.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            GetAnalyticsUseCase.TimeFilter.entries.forEach { filter ->
                                val selected = uiState.timeFilter == filter
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) NeonGreen.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(
                                            if (selected) 1.dp else 0.dp,
                                            if (selected) NeonGreen.copy(alpha = 0.4f) else Color.Transparent,
                                            RoundedCornerShape(10.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    TextButton(
                                        onClick = { viewModel.setTimeFilter(filter) },
                                        shape   = RoundedCornerShape(10.dp),
                                    ) {
                                        Text(
                                            filter.label(),
                                            color      = if (selected) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize   = 13.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Spending trend ────────────────────────────────────────
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, Outline, RoundedCornerShape(20.dp))
                                .padding(16.dp),
                        ) {
                            Column {
                                Text(
                                    "SPENDING TREND",
                                    style         = MaterialTheme.typography.labelSmall,
                                    color         = NeonGreen.copy(alpha = 0.8f),
                                    letterSpacing = 1.5.sp,
                                    fontWeight    = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                SpendingLineChart(
                                    data           = data.monthlySpendHistory,
                                    currencySymbol = CurrencyUtils.getSymbol(uiState.baseCurrency),
                                    modifier       = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    // ── Totals ────────────────────────────────────────────────
                    item {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StatCard(
                                title        = "Monthly",
                                value        = CurrencyUtils.formatAmount(data.totalMonthlySpend, uiState.baseCurrency),
                                icon         = Icons.Filled.CalendarViewMonth,
                                modifier     = Modifier.weight(1f),
                                accentColor  = NeonGreen,
                            )
                            StatCard(
                                title        = "Yearly",
                                value        = CurrencyUtils.formatAmount(data.totalYearlySpend, uiState.baseCurrency),
                                icon         = Icons.Filled.CalendarMonth,
                                modifier     = Modifier.weight(1f),
                                accentColor  = SemanticBlue,
                            )
                        }
                    }

                    // ── Category breakdown ────────────────────────────────────
                    if (data.categoryBreakdown.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, Outline, RoundedCornerShape(20.dp))
                                    .padding(16.dp),
                            ) {
                                Column {
                                    Text(
                                        "CATEGORIES",
                                        style         = MaterialTheme.typography.labelSmall,
                                        color         = NeonGreen.copy(alpha = 0.8f),
                                        letterSpacing = 1.5.sp,
                                        fontWeight    = FontWeight.Bold,
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    DonutChart(
                                        data     = data.categoryBreakdown,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }

                    // ── Most expensive ────────────────────────────────────────
                    data.mostExpensiveSubscription?.let { sub ->
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                            ) {
                                Row(
                                    modifier          = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Most Expensive", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${sub.name} · ${sub.billingCycle.label}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                        // If the subscription uses a different currency, show the original as a hint
                                        if (sub.currency != uiState.baseCurrency) {
                                            Text(
                                                "Originally ${CurrencyUtils.formatAmount(sub.cost, sub.currency)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            )
                                        }
                                    }
                                    // Always display the cost in the user's base currency
                                    Text(
                                        CurrencyUtils.formatAmount(data.mostExpensiveMonthlyCostInBase, uiState.baseCurrency),
                                        fontWeight = FontWeight.Black,
                                        color      = MaterialTheme.colorScheme.error,
                                        style      = MaterialTheme.typography.titleSmall,
                                    )
                                }
                            }
                        }
                    }

                    // ── AI Insights ───────────────────────────────────────────
                    if (data.aiInsights.isNotEmpty()) {
                        item {
                            Text(
                                "SMART INSIGHTS",
                                style         = MaterialTheme.typography.labelSmall,
                                color         = NeonGreen.copy(alpha = 0.8f),
                                letterSpacing = 1.5.sp,
                                fontWeight    = FontWeight.Bold,
                            )
                        }
                        items(data.aiInsights) { insight ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(insight.type.cardBg())
                                    .border(1.dp, insight.type.cardBorder(), RoundedCornerShape(16.dp))
                                    .padding(14.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(insight.type.emoji(), fontSize = 18.sp)
                                    Text(
                                        insight.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun GetAnalyticsUseCase.TimeFilter.label() = when (this) {
    GetAnalyticsUseCase.TimeFilter.ONE_MONTH  -> "1M"
    GetAnalyticsUseCase.TimeFilter.SIX_MONTHS -> "6M"
    GetAnalyticsUseCase.TimeFilter.ONE_YEAR   -> "1Y"
}

@Composable
private fun InsightType.cardBg() = when (this) {
    InsightType.BUDGET_ALERT       -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
    InsightType.DUPLICATE_CATEGORY -> SemanticAmber.copy(alpha = 0.08f)
    InsightType.SAVING_TIP         -> NeonGreen.copy(alpha = 0.07f)
    InsightType.UNUSED_LIKELY      -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun InsightType.cardBorder() = when (this) {
    InsightType.BUDGET_ALERT       -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
    InsightType.DUPLICATE_CATEGORY -> SemanticAmber.copy(alpha = 0.35f)
    InsightType.SAVING_TIP         -> NeonGreen.copy(alpha = 0.3f)
    InsightType.UNUSED_LIKELY      -> Outline
}

private fun InsightType.emoji() = when (this) {
    InsightType.BUDGET_ALERT       -> "⚠️"
    InsightType.DUPLICATE_CATEGORY -> "🔁"
    InsightType.SAVING_TIP         -> "💰"
    InsightType.UNUSED_LIKELY      -> "😴"
}
