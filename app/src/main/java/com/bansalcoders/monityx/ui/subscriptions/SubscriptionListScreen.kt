package com.bansalcoders.monityx.ui.subscriptions

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bansalcoders.monityx.ui.theme.NeonGreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bansalcoders.monityx.domain.usecase.GetSubscriptionsUseCase
import com.bansalcoders.monityx.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionListScreen(
    onAddSubscription: () -> Unit,
    onEditSubscription: (Long) -> Unit,
    viewModel: SubscriptionListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    // Delete confirmation dialog
    if (uiState.deleteConfirmId != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirm,
            title = { Text("Delete Subscription?") },
            text  = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDelete,
                    colors  = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteConfirm) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        containerColor      = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Subscriptions", fontWeight = androidx.compose.ui.text.font.FontWeight.Black, color = MaterialTheme.colorScheme.onBackground) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    actions = {
                        // Sort
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                        ) {
                            GetSubscriptionsUseCase.SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.displayName()) },
                                    onClick = {
                                        viewModel.onSortOrder(order)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (uiState.sortOrder == order) {
                                            Icon(Icons.Filled.Check, contentDescription = null)
                                        }
                                    },
                                )
                            }
                        }
                        // Filter
                        IconButton(onClick = { showFilterSheet = true }) {
                            Badge(
                                containerColor = if (uiState.filterCategory != null)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                            ) {
                                Icon(Icons.Filled.FilterList, contentDescription = "Filter")
                            }
                        }
                    },
                )

                // Search bar
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query          = uiState.query,
                            onQueryChange  = viewModel::onSearchQuery,
                            onSearch       = {},
                            expanded       = false,
                            onExpandedChange = {},
                            placeholder    = { Text("Search subscriptions…") },
                            leadingIcon    = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon   = {
                                if (uiState.query.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQuery("") }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                        )
                    },
                    expanded       = false,
                    onExpandedChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {}

                // Category filter chips
                if (uiState.filterCategory != null) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = true,
                                onClick  = { viewModel.onFilterCategory(null) },
                                label    = { Text(uiState.filterCategory!!) },
                                trailingIcon = {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove filter", modifier = Modifier.size(16.dp))
                                },
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = onAddSubscription,
                containerColor = NeonGreen,
                contentColor   = androidx.compose.ui.graphics.Color(0xFF00210D),
                shape          = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add subscription")
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingScreen(modifier = Modifier.padding(innerPadding))

            uiState.subscriptions.isEmpty() -> EmptyScreen(
                message     = if (uiState.query.isNotEmpty()) "No results for \"${uiState.query}\"" else "No subscriptions yet",
                actionLabel = if (uiState.query.isEmpty()) "Add subscription" else null,
                onAction    = if (uiState.query.isEmpty()) onAddSubscription else null,
                modifier    = Modifier.padding(innerPadding),
            )

            else -> LazyColumn(
                modifier       = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        "${uiState.subscriptions.size} subscription${if (uiState.subscriptions.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }

                items(
                    items = uiState.subscriptions,
                    key   = { it.id },
                ) { subscription ->
                    AnimatedVisibility(
                        visible = true,
                        enter   = fadeIn() + slideInHorizontally(),
                    ) {
                        SubscriptionCard(
                            subscription         = subscription,
                            onEdit               = { onEditSubscription(subscription.id) },
                            onDelete             = { viewModel.showDeleteConfirm(subscription.id) },
                            baseCurrency         = uiState.baseCurrency,
                            convertedMonthlyCost = uiState.convertedMonthlyCosts[subscription.id],
                        )
                    }
                }
            }
        }
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Filter by Category", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))

                FilterChip(
                    selected = uiState.filterCategory == null,
                    onClick  = {
                        viewModel.onFilterCategory(null)
                        showFilterSheet = false
                    },
                    label = { Text("All") },
                )
                viewModel.availableCategories.forEach { category ->
                    FilterChip(
                        selected = uiState.filterCategory == category,
                        onClick  = {
                            viewModel.onFilterCategory(category)
                            showFilterSheet = false
                        },
                        label = { Text(category) },
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private fun GetSubscriptionsUseCase.SortOrder.displayName() = when (this) {
    GetSubscriptionsUseCase.SortOrder.NAME         -> "Name (A–Z)"
    GetSubscriptionsUseCase.SortOrder.COST_HIGH    -> "Cost (High–Low)"
    GetSubscriptionsUseCase.SortOrder.COST_LOW     -> "Cost (Low–High)"
    GetSubscriptionsUseCase.SortOrder.NEXT_BILLING -> "Next Billing"
    GetSubscriptionsUseCase.SortOrder.CATEGORY     -> "Category"
}
