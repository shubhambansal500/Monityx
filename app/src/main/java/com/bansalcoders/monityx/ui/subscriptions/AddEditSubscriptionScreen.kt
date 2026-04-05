package com.bansalcoders.monityx.ui.subscriptions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bansalcoders.monityx.domain.model.*
import com.bansalcoders.monityx.utils.CurrencyUtils
import com.bansalcoders.monityx.utils.DateUtils
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubscriptionScreen(
    subscriptionId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditSubscriptionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Initialise viewModel with the subscription ID
    LaunchedEffect(subscriptionId) {
        viewModel.init(subscriptionId)
    }

    // Navigate back on successful save
    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onNavigateBack()
    }

    var showProviderPicker   by remember { mutableStateOf(false) }
    var showCurrencyPicker   by remember { mutableStateOf(false) }
    var showDatePicker       by remember { mutableStateOf(false) }
    var showCategoryPicker   by remember { mutableStateOf(false) }

    // Date picker state
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.startDate.toEpochDay() * 86_400_000L
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val epochDay = millis / 86_400_000L
                        viewModel.onStartDateChange(LocalDate.ofEpochDay(epochDay))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (subscriptionId == null) "Add Subscription" else "Edit Subscription",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Provider quick-select ──────────────────────────────────────────
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick  = { showProviderPicker = true },
            ) {
                Row(
                    modifier             = Modifier.padding(16.dp),
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Provider", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text(
                            uiState.selectedProvider?.displayName ?: "Select or choose custom…",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            }

            // Provider picker dialog
            if (showProviderPicker) {
                AlertDialog(
                    onDismissRequest = { showProviderPicker = false },
                    title   = { Text("Select Provider") },
                    text    = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            KNOWN_PROVIDERS.forEach { provider ->
                                TextButton(
                                    onClick = {
                                        viewModel.onProviderSelected(provider)
                                        showProviderPicker = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier             = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment    = Alignment.CenterVertically,
                                    ) {
                                        Text("${provider.category.emoji}  ${provider.displayName}")
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showProviderPicker = false }) { Text("Cancel") }
                    },
                )
            }

            // ── Name ──────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = uiState.name,
                onValueChange = viewModel::onNameChange,
                label         = { Text("Subscription Name *") },
                leadingIcon   = { Icon(Icons.Filled.Label, contentDescription = null) },
                isError       = uiState.nameError != null,
                supportingText = { if (uiState.nameError != null) Text(uiState.nameError!!) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
            )

            // ── Cost & Currency ───────────────────────────────────────────────
            Row(
                modifier             = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value         = uiState.cost,
                    onValueChange = viewModel::onCostChange,
                    label         = { Text("Cost *") },
                    leadingIcon   = {
                        Text(
                            text  = CurrencyUtils.getSymbol(uiState.currency),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    isError       = uiState.costError != null,
                    supportingText = { if (uiState.costError != null) Text(uiState.costError!!) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine    = true,
                    modifier      = Modifier.weight(1f),
                )

                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    onClick = { showCurrencyPicker = true },
                ) {
                    Row(
                        modifier             = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Currency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            Text(uiState.currency, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Currency picker
            if (showCurrencyPicker) {
                AlertDialog(
                    onDismissRequest = { showCurrencyPicker = false },
                    title   = { Text("Select Currency") },
                    text    = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            CurrencyUtils.POPULAR_CURRENCIES.forEach { (code, name) ->
                                TextButton(
                                    onClick = {
                                        viewModel.onCurrencyChange(code)
                                        showCurrencyPicker = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier             = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(code, fontWeight = FontWeight.Medium)
                                        Text(name, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showCurrencyPicker = false }) { Text("Cancel") }
                    },
                )
            }

            // ── Billing cycle ─────────────────────────────────────────────────
            Column {
                Text("Billing Cycle", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BillingCycle.entries.forEach { cycle ->
                        FilterChip(
                            selected = uiState.billingCycle == cycle,
                            onClick  = { viewModel.onBillingCycleChange(cycle) },
                            label    = { Text(cycle.label) },
                        )
                    }
                }
            }

            // ── Start date ────────────────────────────────────────────────────
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick  = { showDatePicker = true },
            ) {
                Row(
                    modifier             = Modifier.padding(16.dp),
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Start Date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text(DateUtils.format(uiState.startDate), style = MaterialTheme.typography.bodyLarge)
                    }
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                }
            }

            // ── Category ──────────────────────────────────────────────────────
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick  = { showCategoryPicker = true },
            ) {
                Row(
                    modifier             = Modifier.padding(16.dp),
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text("${uiState.category.emoji} ${uiState.category.label}", style = MaterialTheme.typography.bodyLarge)
                    }
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            }

            if (showCategoryPicker) {
                AlertDialog(
                    onDismissRequest = { showCategoryPicker = false },
                    title = { Text("Select Category") },
                    text  = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Category.entries.forEach { cat ->
                                TextButton(
                                    onClick = {
                                        viewModel.onCategoryChange(cat)
                                        showCategoryPicker = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text("${cat.emoji}  ${cat.label}")
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = { TextButton(onClick = { showCategoryPicker = false }) { Text("Cancel") } },
                )
            }

            // ── Shared With ───────────────────────────────────────────────────
            SharedWithRow(
                sharedWith = uiState.sharedWith,
                cost       = uiState.cost.toDoubleOrNull() ?: 0.0,
                currency   = uiState.currency,
                billingCycle = uiState.billingCycle,
                onDecrement = { viewModel.onSharedWithChange(uiState.sharedWith - 1) },
                onIncrement = { viewModel.onSharedWithChange(uiState.sharedWith + 1) },
            )

            // ── Notes ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label         = { Text("Notes (optional)") },
                maxLines      = 3,
                modifier      = Modifier.fillMaxWidth(),
            )

            // ── Active toggle ─────────────────────────────────────────────────
            Row(
                modifier             = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment    = Alignment.CenterVertically,
            ) {
                Text("Active", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked         = uiState.isActive,
                    onCheckedChange = viewModel::onActiveChange,
                )
            }

            // ── Monthly cost preview ──────────────────────────────────────────
            val cost = uiState.cost.toDoubleOrNull() ?: 0.0
            if (cost > 0) {
                val monthlyCost = cost * uiState.billingCycle.perMonth
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier             = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Monthly equivalent", style = MaterialTheme.typography.labelMedium)
                        Text(
                            CurrencyUtils.formatAmount(monthlyCost, uiState.currency),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // ── Error snackbar ────────────────────────────────────────────────
            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            // ── Save button ───────────────────────────────────────────────────
            Button(
                onClick  = viewModel::save,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = !uiState.isSaving,
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (subscriptionId == null) "Add Subscription" else "Save Changes")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Shared With Row ───────────────────────────────────────────────────────────

@Composable
private fun SharedWithRow(
    sharedWith: Int,
    cost: Double,
    currency: String,
    billingCycle: BillingCycle,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "Split with",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Text(
                    if (sharedWith == 1) "Only me" else "$sharedWith people",
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                if (sharedWith > 1 && cost > 0) {
                    val yourShare = cost * billingCycle.perMonth / sharedWith
                    Text(
                        "Your share: ${CurrencyUtils.formatAmount(yourShare, currency)}/mo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalIconButton(
                    onClick  = onDecrement,
                    enabled  = sharedWith > 1,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                }
                Text(
                    text       = "$sharedWith",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.widthIn(min = 24.dp),
                )
                FilledTonalIconButton(
                    onClick  = onIncrement,
                    enabled  = sharedWith < 10,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
