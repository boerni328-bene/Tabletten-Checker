package com.tablettenchecker.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tablettenchecker.app.R
import com.tablettenchecker.app.data.AppSettings
import com.tablettenchecker.app.data.MedicationFile
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onBack: () -> Unit,
    onExport: (Uri) -> Boolean,
    onReadBackup: (Uri) -> MedicationFile?,
    onRestore: (MedicationFile) -> Unit,
) {
    var morning by rememberSaveable { mutableStateOf(formatTime(settings.morningMinutes)) }
    var noon by rememberSaveable { mutableStateOf(formatTime(settings.noonMinutes)) }
    var evening by rememberSaveable { mutableStateOf(formatTime(settings.eveningMinutes)) }
    var showNoon by rememberSaveable { mutableStateOf(settings.showNoon) }
    var lead by rememberSaveable { mutableStateOf(settings.leadDays.toString()) }
    var supply by rememberSaveable { mutableStateOf(settings.supplyDays.toString()) }
    var notify by rememberSaveable { mutableStateOf(settings.notify) }
    var notifyTime by rememberSaveable { mutableStateOf(formatTime(settings.notifyMinutes)) }
    val notifyValue = parseTime(notifyTime)
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var pending by remember { mutableStateOf<MedicationFile?>(null) }

    val morningValue = parseTime(morning)
    val noonValue = parseTime(noon)
    val eveningValue = parseTime(evening)
    val leadValue = parseWhole(lead, 0, 365)
    val supplyValue = parseWhole(supply, 1, 730)

    val context = LocalContext.current
    // Ab Android 13 müssen Benachrichtigungen erlaubt werden; der Stand wird nach jeder Antwort neu gelesen.
    var allowed by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val msgSaved = stringResource(R.string.backup_done)
    val msgFailed = stringResource(R.string.backup_failed)
    val msgUnreadable = stringResource(R.string.backup_unreadable)
    val msgRestored = stringResource(R.string.restore_done)
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            Toast.makeText(context, if (onExport(uri)) msgSaved else msgFailed, Toast.LENGTH_LONG).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val file = onReadBackup(uri)
            if (file == null) Toast.makeText(context, msgUnreadable, Toast.LENGTH_LONG).show() else pending = file
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.settings_times), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeField(morning, { morning = it }, stringResource(R.string.field_morning), showErrors && morningValue == null, Modifier.weight(1f))
                if (showNoon) {
                    TimeField(noon, { noon = it }, stringResource(R.string.field_noon), showErrors && noonValue == null, Modifier.weight(1f))
                }
                TimeField(evening, { evening = it }, stringResource(R.string.field_evening), showErrors && eveningValue == null, Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.show_noon), modifier = Modifier.weight(1f))
                Switch(checked = showNoon, onCheckedChange = { showNoon = it })
            }
            Text(
                text = stringResource(R.string.settings_times_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(stringResource(R.string.settings_reorder), style = MaterialTheme.typography.titleMedium)
            NumberField(
                value = lead,
                onValueChange = { lead = it },
                label = stringResource(R.string.field_default_lead),
                isError = showErrors && leadValue == null,
                whole = true,
                modifier = Modifier.fillMaxWidth(),
            )
            NumberField(
                value = supply,
                onValueChange = { supply = it },
                label = stringResource(R.string.field_supply_days),
                isError = showErrors && supplyValue == null,
                whole = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.notify_switch), modifier = Modifier.weight(1f))
                Switch(
                    checked = notify,
                    onCheckedChange = {
                        notify = it
                        if (it && !allowed && Build.VERSION.SDK_INT >= 33) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
            if (notify) {
                TimeField(
                    notifyTime,
                    { notifyTime = it },
                    stringResource(R.string.field_notify_time),
                    showErrors && notifyValue == null,
                    Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.notify_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!allowed) {
                    Text(
                        text = stringResource(R.string.notify_blocked),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    OutlinedButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    }) { Text(stringResource(R.string.notify_open_settings)) }
                }
            }
            Button(
                onClick = {
                    if (morningValue != null && noonValue != null && eveningValue != null &&
                        leadValue != null && supplyValue != null && (!notify || notifyValue != null)
                    ) {
                        onSave(
                            settings.copy(
                                morningMinutes = morningValue,
                                noonMinutes = noonValue,
                                eveningMinutes = eveningValue,
                                showNoon = showNoon,
                                leadDays = leadValue,
                                supplyDays = supplyValue,
                                notify = notify,
                                notifyMinutes = notifyValue ?: settings.notifyMinutes,
                            )
                        )
                    } else {
                        showErrors = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save)) }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.backup_text), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { exportLauncher.launch("tabletten-checker-${LocalDate.now()}.json") },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.backup_save)) }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.backup_restore)) }
            }
        }
    }

    pending?.let { file ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.restore_title)) },
            text = {
                Text(pluralStringResource(R.plurals.restore_text, file.medications.size, file.medications.size))
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    onRestore(file)
                    Toast.makeText(context, msgRestored, Toast.LENGTH_LONG).show()
                }) { Text(stringResource(R.string.backup_restore)) }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun TimeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = if (isError) {
            { Text(stringResource(R.string.error_time)) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}
