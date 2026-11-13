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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import java.time.LocalDate
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
    showNoon: Boolean,
    defaultLead: Int,
    onSave: (MedicationInput) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(medication?.name ?: "") }
    var note by rememberSaveable { mutableStateOf(medication?.note ?: "") }
    var morning by rememberSaveable { mutableStateOf(medication?.let { formatAmount(it.morning) } ?: "") }
    var noon by rememberSaveable { mutableStateOf(medication?.let { formatAmount(it.noon) } ?: "") }
    var evening by rememberSaveable { mutableStateOf(medication?.let { formatAmount(it.evening) } ?: "") }
    var stock by rememberSaveable { mutableStateOf(medication?.let { formatAmount(currentStock) } ?: "") }
    var packSize by rememberSaveable {
        mutableStateOf(medication?.takeIf { it.packSize > 0 }?.packSize?.toString() ?: "")
    }
    var everyDays by rememberSaveable { mutableStateOf((medication?.everyDays ?: 1).toString()) }
    var nextDose by rememberSaveable {
        mutableStateOf(
            medication?.takeIf { it.everyDays > 1 }
                ?.let { Math.floorMod(it.anchorDay - LocalDate.now().toEpochDay(), it.everyDays.toLong()).toString() }
                ?: "0"
        )
    }
    var lead by rememberSaveable { mutableStateOf(medication?.leadDays?.toString() ?: "") }
    var more by rememberSaveable {
        mutableStateOf(medication != null && (medication.everyDays > 1 || medication.leadDays != null))
    }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val noonVisible = showNoon || (medication?.noon ?: 0.0) > 0.0
    val morningValue = parseAmount(morning)
    val noonValue = if (noonVisible) parseAmount(noon) else 0.0
    val eveningValue = parseAmount(evening)
    val stockValue = parseAmount(stock)
    val packValue = parseAmount(packSize)
    val everyValue = parseWhole(everyDays, 1, 365)
    val nextValue = if ((everyValue ?: 1) > 1) parseWhole(nextDose, 0, (everyValue ?: 1) - 1) else 0
    val leadValue = parseWhole(lead, 0, 365)
    val leadOk = lead.isBlank() || leadValue != null
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
            Text(stringResource(R.string.label_per_dose), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    value = morning,
                    onValueChange = { morning = it },
                    label = stringResource(R.string.field_morning),
                    isError = showErrors && morningValue == null,
                    modifier = Modifier.weight(1f),
                )
                if (noonVisible) {
                    NumberField(
                        value = noon,
                        onValueChange = { noon = it },
                        label = stringResource(R.string.field_noon),
                        isError = showErrors && noonValue == null,
                        modifier = Modifier.weight(1f),
                    )
                }
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

            TextButton(onClick = { more = !more }) {
                Text(stringResource(R.string.more_options))
                Icon(
                    if (more) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                )
            }
            if (more) {
                NumberField(
                    value = everyDays,
                    onValueChange = { everyDays = it },
                    label = stringResource(R.string.field_every_days),
                    isError = showErrors && everyValue == null,
                    whole = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if ((everyValue ?: 1) > 1) {
                    NumberField(
                        value = nextDose,
                        onValueChange = { nextDose = it },
                        label = stringResource(R.string.field_next_dose),
                        isError = showErrors && nextValue == null,
                        whole = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                NumberField(
                    value = lead,
                    onValueChange = { lead = it },
                    label = stringResource(R.string.field_lead),
                    isError = showErrors && !leadOk,
                    whole = true,
                    placeholder = stringResource(R.string.lead_default, defaultLead),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = {
                    if (nameOk && morningValue != null && noonValue != null && eveningValue != null &&
                        stockValue != null && packValue != null && everyValue != null && nextValue != null && leadOk
                    ) {
                        onSave(
                            MedicationInput(
                                name = name,
                                note = note,
                                morning = morningValue,
                                noon = noonValue,
                                evening = eveningValue,
                                everyDays = everyValue,
                                nextDoseInDays = nextValue,
                                stock = stockValue,
                                packSize = packValue.roundToInt(),
                                leadDays = if (lead.isBlank()) null else leadValue,
                            )
                        )
                    } else {
                        showErrors = true
                        // Fehler in den weiteren Angaben sichtbar machen.
                        if (everyValue == null || nextValue == null || !leadOk) more = true
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
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    whole: Boolean = false,
    placeholder: String? = null,
    errorText: String = stringResource(R.string.error_number),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = true,
        isError = isError,
        supportingText = if (isError) {
            { Text(errorText) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = if (whole) KeyboardType.Number else KeyboardType.Decimal),
        modifier = modifier,
    )
}
