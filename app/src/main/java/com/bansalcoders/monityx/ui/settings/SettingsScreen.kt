package com.bansalcoders.monityx.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bansalcoders.monityx.BuildConfig
import com.bansalcoders.monityx.utils.CurrencyUtils
import com.bansalcoders.monityx.utils.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToPrivacyPolicy: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context  = LocalContext.current
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showBudgetDialog   by remember { mutableStateOf(false) }

    // Track POST_NOTIFICATIONS permission; re-check on every ON_RESUME so the
    // banner disappears automatically after the user grants it in system settings.
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            else true,
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) {
                hasNotificationPermission = context.checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Share exported file when ready
    LaunchedEffect(uiState.exportedFile) {
        uiState.exportedFile?.let { file ->
            val uri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file,
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (file.name.endsWith(".csv")) "text/csv" else "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share export"))
            viewModel.clearExportResult()
        }
    }

    Scaffold(
        containerColor      = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {

            // ── Budget ────────────────────────────────────────────────────────
            SettingsSection(title = "Budget") {
                ListItem(
                    headlineContent   = { Text("Monthly Budget") },
                    supportingContent = {
                        if (uiState.monthlyBudgetInBaseCurrency > 0.0) {
                            Column {
                                Text(CurrencyUtils.formatAmount(uiState.monthlyBudgetInBaseCurrency, uiState.baseCurrency))
                                // Show the original amount as a hint when the budget was
                                // set in a different currency than the current base.
                                if (uiState.budgetCurrency != uiState.baseCurrency) {
                                    Text(
                                        "Originally: ${CurrencyUtils.formatAmount(uiState.monthlyBudget, uiState.budgetCurrency)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    )
                                }
                            }
                        } else {
                            Text("Not set — tap to configure")
                        }
                    },
                    trailingContent   = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.monthlyBudgetInBaseCurrency > 0.0) {
                                TextButton(onClick = { viewModel.setMonthlyBudget(0.0) }) {
                                    Text("Clear", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null)
                        }
                    },
                    leadingContent    = { Icon(Icons.Filled.AccountBalance, contentDescription = null) },
                    modifier          = Modifier.clickableListItem { showBudgetDialog = true },
                )
            }

            // ── Currency ──────────────────────────────────────────────────────
            SettingsSection(title = "Currency") {
                ListItem(
                    headlineContent   = { Text("Base Currency") },
                    supportingContent = { Text(CurrencyUtils.getDisplayName(uiState.baseCurrency)) },
                    trailingContent   = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(uiState.baseCurrency, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Icon(Icons.Filled.ChevronRight, contentDescription = null)
                        }
                    },
                    leadingContent    = { Icon(Icons.Filled.CurrencyExchange, contentDescription = null) },
                    modifier          = Modifier.clickableListItem { showCurrencyPicker = true },
                )
            }

            // ── Theme ─────────────────────────────────────────────────────────
            SettingsSection(title = "Appearance") {
                ThemeMode.entries.forEach { mode ->
                    ListItem(
                        headlineContent = { Text(mode.displayName()) },
                        trailingContent = {
                            RadioButton(
                                selected = uiState.themeMode == mode,
                                onClick  = { viewModel.setThemeMode(mode) },
                            )
                        },
                        leadingContent = {
                            Icon(mode.icon(), contentDescription = null)
                        },
                        modifier = Modifier.clickableListItem { viewModel.setThemeMode(mode) },
                    )
                }
            }

            // ── Notifications ─────────────────────────────────────────────────
            SettingsSection(title = "Notifications") {
                ListItem(
                    headlineContent   = { Text("Billing Reminders") },
                    supportingContent = { Text("Notify before subscriptions renew") },
                    trailingContent   = {
                        Switch(
                            checked         = uiState.notificationsEnabled,
                            onCheckedChange = viewModel::setNotificationsEnabled,
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                )

                if (uiState.notificationsEnabled) {
                    ListItem(
                        headlineContent   = { Text("Remind me") },
                        supportingContent = { Text("${uiState.reminderDays} day(s) before renewal") },
                        leadingContent    = { Icon(Icons.Filled.AccessTime, contentDescription = null) },
                    )
                    Slider(
                        value         = uiState.reminderDays.toFloat(),
                        onValueChange = { viewModel.setReminderDays(it.toInt()) },
                        valueRange    = 1f..14f,
                        steps         = 12,
                        modifier      = Modifier.padding(horizontal = 16.dp),
                    )
                }

                // ── Permission denied banner ──────────────────────────────────
                if (uiState.notificationsEnabled && !hasNotificationPermission) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.NotificationsOff,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "Notification permission denied. Grant it in system settings to receive reminders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    },
                                )
                            },
                        ) { Text("Open Settings", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }

            // ── Export ────────────────────────────────────────────────────────
            SettingsSection(title = "Data") {
                ListItem(
                    headlineContent   = { Text("Export as CSV") },
                    supportingContent = { Text("Comma-separated values file") },
                    trailingContent   = {
                        if (uiState.isExporting)
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        else
                            Icon(Icons.Filled.Download, contentDescription = null)
                    },
                    leadingContent = { Icon(Icons.Filled.TableChart, contentDescription = null) },
                    modifier       = Modifier.clickableListItem { viewModel.exportCsv() },
                )

                ListItem(
                    headlineContent   = { Text("Export as PDF") },
                    supportingContent = { Text("Printable PDF report") },
                    trailingContent   = {
                        if (uiState.isExporting)
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        else
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                    },
                    leadingContent = { Icon(Icons.Filled.Description, contentDescription = null) },
                    modifier       = Modifier.clickableListItem { viewModel.exportPdf() },
                )
            }

            // ── Export error ──────────────────────────────────────────────────
            uiState.exportError?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        error,
                        modifier = Modifier.padding(12.dp),
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            // ── About ─────────────────────────────────────────────────────────
            SettingsSection(title = "About") {
                ListItem(
                    headlineContent   = { Text("Privacy Policy") },
                    trailingContent   = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                    leadingContent    = { Icon(Icons.Filled.PrivacyTip, contentDescription = null) },
                    modifier          = Modifier.clickableListItem(onNavigateToPrivacyPolicy),
                )
                ListItem(
                    headlineContent   = { Text("App Version") },
                    supportingContent = { Text(BuildConfig.VERSION_NAME) },
                    leadingContent    = { Icon(Icons.Filled.Info, contentDescription = null) },
                )
                ListItem(
                    headlineContent   = { Text("Licenses") },
                    supportingContent = { Text("Third-party libraries and their licenses") },
                    leadingContent    = { Icon(Icons.Filled.Code, contentDescription = null) },
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Budget input dialog — pre-populate with the value already converted to
    // the current base currency so the user edits in their preferred currency.
    if (showBudgetDialog) {
        BudgetInputDialog(
            currentBudget = uiState.monthlyBudgetInBaseCurrency,
            currency      = uiState.baseCurrency,
            onConfirm     = { budget ->
                viewModel.setMonthlyBudget(budget)
                showBudgetDialog = false
            },
            onDismiss     = { showBudgetDialog = false },
        )
    }

    // Currency picker dialog
    if (showCurrencyPicker) {
        AlertDialog(
            onDismissRequest = { showCurrencyPicker = false },
            title   = { Text("Select Base Currency") },
            text    = {
                Column(
                    modifier = Modifier
                        .height(320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    CurrencyUtils.POPULAR_CURRENCIES.forEach { (code, name) ->
                        ListItem(
                            headlineContent   = { Text(code, fontWeight = FontWeight.Medium) },
                            supportingContent = { Text(name) },
                            trailingContent   = {
                                if (uiState.baseCurrency == code)
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            modifier = Modifier.clickableListItem {
                                viewModel.setBaseCurrency(code)
                                showCurrencyPicker = false
                            },
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showCurrencyPicker = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun BudgetInputDialog(
    currentBudget: Double,
    currency: String,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember {
        mutableStateOf(if (currentBudget > 0.0) "%.0f".format(currentBudget) else "")
    }
    val isValid = text.toDoubleOrNull()?.let { it > 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Set Monthly Budget") },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Enter your maximum monthly subscription spend in $currency.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                OutlinedTextField(
                    value         = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' } },
                    label         = { Text("Budget ($currency)") },
                    leadingIcon   = { Text(currency, style = MaterialTheme.typography.bodyMedium) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { text.toDoubleOrNull()?.let { onConfirm(it) } },
                enabled = isValid,
            ) { Text("Set Budget") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun PrivacyPolicyScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Privacy Policy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Last updated: April 2025", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

            PrivacySection("Data Collection") {
                "Subscription Manager does not collect, transmit, or share any personal data with third parties. All subscription data is stored exclusively on your device."
            }
            PrivacySection("Data Storage") {
                "All data is stored locally using an encrypted SQLite database (Room). No data is sent to any server."
            }
            PrivacySection("Network Access") {
                "The app optionally accesses the internet only to refresh currency exchange rates from the open.er-api.com free tier. No personal or subscription data is included in these requests."
            }
            PrivacySection("Permissions") {
                "The app requests only: POST_NOTIFICATIONS (for billing reminders, Android 13+), INTERNET (optional currency refresh), ACCESS_NETWORK_STATE (connectivity check). Billing reminders use WorkManager — no exact alarm permissions are required. No contacts, location, camera, microphone, or storage permissions are requested."
            }
            PrivacySection("Contact") {
                "If you have privacy questions, please open an issue on our GitHub repository."
            }
        }
    }
}

@Composable
private fun PrivacySection(title: String, content: () -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(content(), style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text          = title.uppercase(),
            style         = MaterialTheme.typography.labelSmall,
            color         = com.bansalcoders.monityx.ui.theme.NeonGreen.copy(alpha = 0.8f),
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier      = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape    = RoundedCornerShape(18.dp),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border   = androidx.compose.foundation.BorderStroke(1.dp, com.bansalcoders.monityx.ui.theme.Outline),
        ) {
            Column(content = content)
        }
    }
}

private fun Modifier.clickableListItem(onClick: () -> Unit) =
    this.then(androidx.compose.ui.Modifier.clickable(onClick = onClick))

private fun ThemeMode.displayName() = when (this) {
    ThemeMode.LIGHT  -> "Light"
    ThemeMode.DARK   -> "Dark"
    ThemeMode.SYSTEM -> "System Default"
}

@Composable
private fun ThemeMode.icon() = when (this) {
    ThemeMode.LIGHT  -> Icons.Filled.LightMode
    ThemeMode.DARK   -> Icons.Filled.DarkMode
    ThemeMode.SYSTEM -> Icons.Filled.SettingsBrightness
}

