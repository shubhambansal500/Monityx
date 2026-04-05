package com.bansalcoders.monityx.ui.insights

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bansalcoders.monityx.domain.model.*
import com.bansalcoders.monityx.ui.components.EmptyScreen
import com.bansalcoders.monityx.ui.components.LoadingScreen
import com.bansalcoders.monityx.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onNavigateBack: () -> Unit,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title         = { Text("Financial Insights", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingScreen(modifier = Modifier.padding(innerPadding))

            uiState.error != null -> EmptyScreen(
                message     = "Could not load insights",
                actionLabel = "Retry",
                onAction    = { viewModel.load() },
                modifier    = Modifier.padding(innerPadding),
            )

            else -> InsightsContent(
                uiState    = uiState,
                modifier   = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun InsightsContent(
    uiState: InsightsUiState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier       = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        // ── Health Score hero ─────────────────────────────────────────────────
        uiState.healthScore?.let { score ->
            item { HealthScoreHero(score = score) }
        }

        // ── Total savings summary ─────────────────────────────────────────────
        if (uiState.totalYearlySavings > 0) {
            item {
                SavingsSummaryCard(
                    monthlyCancel = uiState.totalCancelMonthlySavings,
                    yearlyTotal   = uiState.totalYearlySavings,
                    currency      = uiState.baseCurrency,
                )
            }
        }

        // ── Budget status ─────────────────────────────────────────────────────
        if (uiState.monthlyBudget > 0) {
            item {
                BudgetStatusCard(
                    budget   = uiState.monthlyBudget,
                    currency = uiState.baseCurrency,
                )
            }
        }

        // ── Suggestions ───────────────────────────────────────────────────────
        if (uiState.suggestions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    ),
                ) {
                    Column(
                        modifier              = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment   = Alignment.CenterHorizontally,
                        verticalArrangement   = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("🎉", fontSize = 36.sp)
                        Text(
                            "Your portfolio looks optimised!",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "No savings suggestions at this time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    "${uiState.suggestions.size} Savings Opportunities",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            items(uiState.suggestions, key = { it.subscription.id }) { suggestion ->
                SuggestionCard(
                    suggestion = suggestion,
                    currency   = uiState.baseCurrency,
                )
            }
        }

        // ── Health breakdown ──────────────────────────────────────────────────
        uiState.healthScore?.let { score ->
            if (score.breakdown.isNotEmpty()) {
                item {
                    Text(
                        "Score Breakdown",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            score.breakdown.forEach { deduction ->
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        modifier          = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.RemoveCircleOutline,
                                            contentDescription = null,
                                            tint     = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            deduction.reason,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    Text(
                                        "−${deduction.points} pts",
                                        style      = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                            HorizontalDivider()
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Final Score", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${score.score}/100",
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color      = score.grade.scoreColor(),
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ── Health Score Hero ─────────────────────────────────────────────────────────

@Composable
private fun HealthScoreHero(score: HealthScore) {
    val gradeColor = score.grade.scoreColor()

    val animatedScore by animateIntAsState(
        targetValue   = score.score,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label         = "score_anim",
    )
    val sweepAngle by animateFloatAsState(
        targetValue   = score.score / 100f * 360f,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label         = "sweep_anim",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = gradeColor.copy(alpha = 0.1f)),
        shape    = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier            = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Subscription Health",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    val stroke = 14.dp.toPx()
                    val inset  = stroke / 2
                    drawArc(
                        color      = gradeColor.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter  = false,
                        style      = Stroke(width = stroke, cap = StrokeCap.Round),
                        topLeft    = Offset(inset, inset),
                        size       = Size(size.width - stroke, size.height - stroke),
                    )
                    drawArc(
                        color      = gradeColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter  = false,
                        style      = Stroke(width = stroke, cap = StrokeCap.Round),
                        topLeft    = Offset(inset, inset),
                        size       = Size(size.width - stroke, size.height - stroke),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = "$animatedScore",
                        style      = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color      = gradeColor,
                    )
                    Text(
                        text  = "/ 100",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            }

            Text(
                text       = "${score.grade.emoji}  ${score.grade.label}",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color      = gradeColor,
            )
        }
    }
}

// ── Savings Summary Card ──────────────────────────────────────────────────────

@Composable
private fun SavingsSummaryCard(
    monthlyCancel: Double,
    yearlyTotal: Double,
    currency: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "💰 Potential Savings",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SavingsStatColumn(
                    label  = "Monthly (cancel)",
                    amount = CurrencyUtils.formatAmount(monthlyCancel, currency),
                )
                VerticalDivider(modifier = Modifier.height(48.dp))
                SavingsStatColumn(
                    label  = "Yearly (all tips)",
                    amount = CurrencyUtils.formatAmount(yearlyTotal, currency),
                )
            }
        }
    }
}

@Composable
private fun SavingsStatColumn(label: String, amount: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = amount,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
        )
    }
}

// ── Budget Status Card ────────────────────────────────────────────────────────

@Composable
private fun BudgetStatusCard(budget: Double, currency: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        ListItem(
            headlineContent   = { Text("Monthly Budget", fontWeight = FontWeight.Medium) },
            supportingContent = { Text("Set in Settings") },
            trailingContent   = {
                Text(
                    CurrencyUtils.formatAmount(budget, currency),
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary,
                )
            },
            leadingContent = {
                Icon(Icons.Filled.AccountBalance, contentDescription = null)
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
            ),
        )
    }
}

// ── Suggestion Card ───────────────────────────────────────────────────────────

@Composable
private fun SuggestionCard(
    suggestion: SavingsSuggestion,
    currency: String,
) {
    val (bgColor, iconColor, icon) = when (suggestion.reason) {
        SuggestionReason.INACTIVE -> Triple(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.error,
            Icons.Filled.Cancel,
        )
        SuggestionReason.DUPLICATE_CATEGORY -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.tertiary,
            Icons.Filled.ContentCopy,
        )
        SuggestionReason.SWITCH_TO_YEARLY -> Triple(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.primary,
            Icons.Filled.CalendarMonth,
        )
        SuggestionReason.LIKELY_UNUSED -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            Icons.Filled.HelpOutline,
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = bgColor),
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = iconColor,
                modifier           = Modifier.size(22.dp).padding(top = 2.dp),
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text       = suggestion.subscription.name,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text  = suggestion.reason.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = iconColor,
                )
                Text(
                    text  = suggestion.reason.actionHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text       = "−${CurrencyUtils.formatAmount(suggestion.monthlySavings, currency)}/mo",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = iconColor,
                )
                Text(
                    text  = "−${CurrencyUtils.formatAmount(suggestion.yearlySavings, currency)}/yr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

// ── Grade color helpers ───────────────────────────────────────────────────────

@Composable
private fun HealthGrade.scoreColor(): Color = when (this) {
    HealthGrade.EXCELLENT -> Color(0xFF2E7D32)
    HealthGrade.GOOD      -> Color(0xFF1565C0)
    HealthGrade.FAIR      -> Color(0xFFF57F17)
    HealthGrade.POOR      -> Color(0xFFE65100)
    HealthGrade.CRITICAL  -> MaterialTheme.colorScheme.error
}
