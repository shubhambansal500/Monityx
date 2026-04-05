package com.bansalcoders.monityx.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bansalcoders.monityx.domain.model.*
import com.bansalcoders.monityx.ui.components.*
import com.bansalcoders.monityx.ui.theme.*
import com.bansalcoders.monityx.utils.CurrencyUtils
import com.bansalcoders.monityx.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddSubscription: () -> Unit,
    onViewSubscriptions: () -> Unit,
    onViewInsights: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor      = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonGreen.copy(alpha = 0.15f))
                                .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("M", fontWeight = FontWeight.Black, color = NeonGreen, fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Monityx",
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color      = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onViewInsights) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonGreen.copy(alpha = 0.1f))
                                .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Insights,
                                contentDescription = "Insights",
                                tint     = NeonGreen,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick          = onAddSubscription,
                containerColor   = NeonGreen,
                contentColor     = Color(0xFF00210D),
                shape            = RoundedCornerShape(18.dp),
                elevation        = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(20.dp))
                    Text("Add", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingScreen(modifier = Modifier.padding(innerPadding))
        } else {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh    = { viewModel.refreshCurrencyRates() },
                modifier     = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {

                    // ── Hero balance card ─────────────────────────────────────
                    item {
                        HeroBalanceCard(
                            monthlyCost  = uiState.totalMonthlyCost,
                            yearlyCost   = uiState.totalYearlyCost,
                            activeCount  = uiState.activeCount,
                            baseCurrency = uiState.baseCurrency,
                        )
                    }

                    // ── Health Score ──────────────────────────────────────────
                    uiState.healthScore?.let { score ->
                        item {
                            HealthScoreCard(score = score, onViewMore = onViewInsights)
                        }
                    }

                    // ── Budget Progress ───────────────────────────────────────
                    if (uiState.monthlyBudget > 0.0) {
                        item {
                            BudgetProgressCard(
                                spent      = uiState.totalMonthlyCost,
                                budget     = uiState.monthlyBudget,
                                percentage = uiState.budgetPercentageUsed,
                                currency   = uiState.baseCurrency,
                            )
                        }
                    }

                    // ── Smart Savings ─────────────────────────────────────────
                    if (uiState.totalPotentialMonthlySavings > 0) {
                        item {
                            SmartSavingsCard(
                                monthlySavings = uiState.totalPotentialMonthlySavings,
                                currency       = uiState.baseCurrency,
                                topSuggestions = uiState.topSavings,
                                onViewAll      = onViewInsights,
                            )
                        }
                    }

                    // ── Upcoming payments ─────────────────────────────────────
                    if (uiState.upcomingPayments.isNotEmpty()) {
                        item {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "UPCOMING",
                                    style         = MaterialTheme.typography.labelMedium,
                                    fontWeight    = FontWeight.Bold,
                                    color         = NeonGreen,
                                    letterSpacing = 1.5.sp,
                                )
                                TextButton(onClick = onViewSubscriptions) {
                                    Text(
                                        "View all",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        items(uiState.upcomingPayments, key = { it.id }) { sub ->
                            val displayAmount   = uiState.convertedPaymentCosts[sub.id] ?: sub.monthlyCost
                            val displayCurrency = uiState.baseCurrency
                            UpcomingPaymentRow(
                                sub             = sub,
                                displayAmount   = displayAmount,
                                displayCurrency = displayCurrency,
                            )
                        }
                    } else if (uiState.activeCount > 0) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, Outline, RoundedCornerShape(16.dp))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "No renewals in the next 7 days 🎉",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        item {
                            EmptyScreen(
                                message     = "No subscriptions yet",
                                actionLabel = "Add your first",
                                onAction    = onAddSubscription,
                                modifier    = Modifier.height(240.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Hero Balance Card ─────────────────────────────────────────────────────────

@Composable
private fun HeroBalanceCard(
    monthlyCost: Double,
    yearlyCost: Double,
    activeCount: Int,
    baseCurrency: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0D2137), Color(0xFF091A10)),
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(NeonGreen.copy(alpha = 0.5f), Outline)),
                RoundedCornerShape(24.dp),
            )
            .padding(24.dp),
    ) {
        // Glow circle backdrop
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            drawCircle(
                color  = NeonGreen.copy(alpha = 0.06f),
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.8f, size.height * 0.2f),
            )
        }

        Column {
            Text(
                "MONTHLY SPEND",
                style         = MaterialTheme.typography.labelSmall,
                color         = NeonGreen.copy(alpha = 0.8f),
                letterSpacing = 1.5.sp,
                fontWeight    = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text       = CurrencyUtils.formatAmount(monthlyCost, baseCurrency),
                style      = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color      = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "${CurrencyUtils.formatAmount(yearlyCost, baseCurrency)} / year",
                style = MaterialTheme.typography.bodySmall,
                color = NeonGreen.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = NeonGreen.copy(alpha = 0.12f))
            Spacer(Modifier.height(16.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HeroStat(label = "Active", value = activeCount.toString())
                HeroStat(label = "Avg / day", value = CurrencyUtils.formatAmount(monthlyCost / 30.0, baseCurrency))
                HeroStat(label = "Avg / week", value = CurrencyUtils.formatAmount(monthlyCost / 4.33, baseCurrency))
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = NeonGreen.copy(alpha = 0.6f),
        )
    }
}

// ── Health Score Card ─────────────────────────────────────────────────────────

@Composable
private fun HealthScoreCard(score: HealthScore, onViewMore: () -> Unit) {
    val animatedScore by animateIntAsState(
        targetValue   = score.score,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label         = "health_score",
    )
    val sweepAngle by animateFloatAsState(
        targetValue   = score.score / 100f * 360f,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label         = "sweep",
    )
    val gradeColor = score.grade.gradeColor()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Outline, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Animated ring
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                Canvas(modifier = Modifier.size(80.dp)) {
                    val stroke = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                    val inset  = Offset(5.dp.toPx(), 5.dp.toPx())
                    val s      = Size(size.width - 10.dp.toPx(), size.height - 10.dp.toPx())
                    drawArc(gradeColor.copy(alpha = 0.15f), -90f, 360f, false, inset, s, style = stroke)
                    drawArc(gradeColor, -90f, sweepAngle, false, inset, s, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$animatedScore", fontWeight = FontWeight.Black, fontSize = 20.sp, color = gradeColor)
                    Text("/100", fontSize = 9.sp, color = gradeColor.copy(alpha = 0.6f))
                }
            }

            Spacer(Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${score.grade.emoji}  ${score.grade.label}",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = gradeColor,
                )
                Text(
                    "Portfolio Health Score",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (score.breakdown.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "▸ ${score.breakdown.first().reason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            TextButton(onClick = onViewMore) {
                Text("Details", style = MaterialTheme.typography.labelMedium, color = NeonGreen)
            }
        }
    }
}

// ── Budget Progress Card ──────────────────────────────────────────────────────

@Composable
private fun BudgetProgressCard(
    spent: Double,
    budget: Double,
    percentage: Float,
    currency: String,
) {
    val animatedPct by animateFloatAsState(
        targetValue   = (percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(900),
        label         = "budget_pct",
    )
    val isExceeded = percentage >= 100f
    val isNear     = percentage >= 80f && !isExceeded
    val barColor   = when {
        isExceeded -> MaterialTheme.colorScheme.error
        isNear     -> MaterialTheme.colorScheme.tertiary
        else       -> NeonGreen
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, if (isExceeded) barColor.copy(alpha = 0.4f) else Outline, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        if (isExceeded) Icons.Filled.Warning else Icons.Filled.AccountBalance,
                        contentDescription = null,
                        tint     = barColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        if (isExceeded) "Budget Exceeded!" else "Monthly Budget",
                        style      = MaterialTheme.typography.titleSmall,
                        color      = barColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(barColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text("${percentage.toInt()}%", style = MaterialTheme.typography.labelMedium, color = barColor, fontWeight = FontWeight.Bold)
                }
            }

            // Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor.copy(alpha = 0.12f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedPct)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(listOf(barColor.copy(alpha = 0.6f), barColor))
                        ),
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Spent  ${CurrencyUtils.formatAmount(spent, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Budget  ${CurrencyUtils.formatAmount(budget, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Smart Savings Card ────────────────────────────────────────────────────────

@Composable
private fun SmartSavingsCard(
    monthlySavings: Double,
    currency: String,
    topSuggestions: List<SavingsSuggestion>,
    onViewAll: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF091A10), Color(0xFF0D2137)))
            )
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(NeonGreen.copy(alpha = 0.4f), Outline)),
                RoundedCornerShape(20.dp),
            )
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "SMART SAVINGS",
                        style         = MaterialTheme.typography.labelSmall,
                        color         = NeonGreen.copy(alpha = 0.7f),
                        letterSpacing = 1.5.sp,
                        fontWeight    = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Save ${CurrencyUtils.formatAmount(monthlySavings, currency)}/mo",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color      = NeonGreen,
                    )
                }
                TextButton(onClick = onViewAll) {
                    Text("View all →", style = MaterialTheme.typography.labelMedium, color = NeonGreen.copy(alpha = 0.7f))
                }
            }

            if (topSuggestions.isNotEmpty()) {
                HorizontalDivider(color = NeonGreen.copy(alpha = 0.1f))
                topSuggestions.take(2).forEach { s ->
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.subscription.category.emoji, fontSize = 16.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.subscription.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                            Text(s.reason.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            "−${CurrencyUtils.formatAmount(s.monthlySavings, currency)}/mo",
                            style      = MaterialTheme.typography.labelMedium,
                            color      = NeonGreen,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

// ── Upcoming Payment Row ──────────────────────────────────────────────────────

@Composable
private fun UpcomingPaymentRow(
    sub: Subscription,
    displayAmount: Double,
    displayCurrency: String,
) {
    val daysLeft  = sub.daysUntilNextBilling()
    val urgency   = when {
        daysLeft <= 0 -> MaterialTheme.colorScheme.error
        daysLeft <= 3 -> MaterialTheme.colorScheme.tertiary
        else          -> NeonGreen
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Emoji avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(urgency.copy(alpha = 0.1f))
                    .border(1.dp, urgency.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(sub.category.emoji, fontSize = 18.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(sub.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(2.dp))
                Text(DateUtils.daysUntilText(sub.nextBillingDate), style = MaterialTheme.typography.labelSmall, color = urgency)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    CurrencyUtils.formatAmount(displayAmount, displayCurrency),
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = NeonGreen,
                )
                if (sub.currency != displayCurrency) {
                    Text(
                        CurrencyUtils.formatAmount(sub.cost, sub.currency),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── Grade colour helper ───────────────────────────────────────────────────────

@Composable
private fun HealthGrade.gradeColor(): Color = when (this) {
    HealthGrade.EXCELLENT -> NeonGreen
    HealthGrade.GOOD      -> SemanticBlue
    HealthGrade.FAIR      -> SemanticAmber
    HealthGrade.POOR      -> Color(0xFFFF7043)
    HealthGrade.CRITICAL  -> MaterialTheme.colorScheme.error
}
