package com.tablettenchecker.app.ui

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tablettenchecker.app.R
import com.tablettenchecker.app.data.Medication
import com.tablettenchecker.app.data.MedicationInput
import kotlin.math.roundToInt

/**
 * Tablette hinzufügen ([medication] = null) oder bearbeiten.
 * [currentStock] ist der heute berechnete Bestand; er steht als Vorschlag im Feld „Bestand heute“.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    medication: Medication?,
    currentStock: Double,
    onSave: (MedicationInput) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(medication?.name ?: "") }
    var note by rememberSaveable { mutableStateOf(medication?.note ?: "") }
    var morning by rememberSaveable { mutableStateOf(medication?.let { formatAmount(it.morning) } ?: "") }
    var evening by rememberSaveable { mutableStateOf(medication?.let { formatAmount(it.evening) } ?: "") }
    var stock by rememberSaveable { mutableStateOf(medication?.let { formatAmount(currentStock) } ?: "") }
    var packSize by rememberSaveable {
        mutableStateOf(medication?.takeIf { it.packSize > 0 }?.packSize?.toString() ?: "")
    }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val morningValue = parseAmount(morning)
    val eveningValue = parseAmount(evening)
    val stockValue = parseAmount(stock)
    val packValue = parseAmount(packSize)
    val nameOk = name.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (medication == null) R.string.title_new else R.string.title_edit))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    if (medication != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.field_name)) },
                singleLine = true,
                isError = showErrors && !nameOk,
                supportingText = if (showErrors && !nameOk) {
                    { Text(stringResource(R.string.error_name)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.field_note)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(
                    value = morning,
                    onValueChange = { morning = it },
                    label = stringResource(R.string.field_morning),
                    isError = showErrors && morningValue == null,
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    value = evening,
                    onValueChange = { evening = it },
                    label = stringResource(R.string.field_evening),
                    isError = showErrors && eveningValue == null,
                    modifier = Modifier.weight(1f),
                )
            }
            NumberField(
                value = stock,
                onValueChange = { stock = it },
                label = stringResource(R.string.field_stock),
                isError = showErrors && stockValue == null,
                modifier = Modifier.fillMaxWidth(),
            )
            NumberField(
                value = packSize,
                onValueChange = { packSize = it },
                label = stringResource(R.string.field_pack_size),
                isError = showErrors && packValue == null,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.hint_doses),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    if (nameOk && morningValue != null && eveningValue != null && stockValue != null && packValue != null) {
                        onSave(
                            MedicationInput(
                                name = name,
                                note = note,
                                morning = morningValue,
                                evening = eveningValue,
                                stock = stockValue,
                                packSize = packValue.roundToInt(),
                            )
                        )
                    } else {
                        showErrors = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }

    if (confirmDelete && medication != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_text, medication.name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = if (isError) {
            { Text(stringResource(R.string.error_number)) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}
