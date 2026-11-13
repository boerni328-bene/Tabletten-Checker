package com.tablettenchecker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tablettenchecker.app.R
import com.tablettenchecker.app.data.Medication
import com.tablettenchecker.app.data.StockCalculator
import com.tablettenchecker.app.data.StockCalculator.Level

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    medications: List<Medication>,
    nowMillis: Long,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onAddPack: (String) -> Unit,
) {
    // Dringendes zuerst: Was am frühesten aufgebraucht ist, steht oben.
    val rows = remember(medications, nowMillis) {
        medications
            .map { it to StockCalculator.status(it, nowMillis) }
            .sortedWith(compareBy({ it.second.daysLeft ?: Int.MAX_VALUE }, { it.first.name.lowercase() }))
    }
    var packFor by remember { mutableStateOf<Medication?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_medication)) },
            )
        },
    ) { padding ->
        if (rows.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.empty_list),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    // Platz für die Schaltfläche „Tablette hinzufügen“.
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(rows, key = { it.first.id }) { (medication, status) ->
                    MedicationCard(
                        medication = medication,
                        status = status,
                        onClick = { onEdit(medication.id) },
                        onAddPack = { packFor = medication },
                    )
                }
            }
        }
    }

    packFor?.let { medication ->
        AlertDialog(
            onDismissRequest = { packFor = null },
            title = { Text(stringResource(R.string.add_pack_title)) },
            text = {
                Text(stringResource(R.string.add_pack_text, medication.name, medication.packSize.toString()))
            },
            confirmButton = {
                TextButton(onClick = {
                    onAddPack(medication.id)
                    packFor = null
                }) { Text(stringResource(R.string.add)) }
            },
            dismissButton = {
                TextButton(onClick = { packFor = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun MedicationCard(
    medication: Medication,
    status: StockCalculator.Status,
    onClick: () -> Unit,
    onAddPack: () -> Unit,
) {
    val colors = when (status.level) {
        Level.EMPTY -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
        Level.REORDER -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        else -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    }
    Card(onClick = onClick, colors = colors, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (medication.note.isBlank()) medication.name else "${medication.name} · ${medication.note}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(
                    R.string.dose_line,
                    formatAmount(medication.morning),
                    formatAmount(medication.evening),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.stock_line, formatAmount(status.current)),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(text = rangeText(status), style = MaterialTheme.typography.bodyLarge)
            reorderText(status)?.let {
                Text(text = it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            val packDays = status.packDays
            if (packDays != null) {
                Text(
                    text = pluralStringResource(
                        R.plurals.pack_line,
                        packDays,
                        medication.packSize.toString(),
                        packDays,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (medication.packSize > 0) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onAddPack) { Text(stringResource(R.string.add_pack)) }
            }
        }
    }
}

@Composable
private fun rangeText(status: StockCalculator.Status): String {
    val daysLeft = status.daysLeft
    val emptyOn = status.emptyOn
    return when {
        status.level == Level.NO_PLAN -> stringResource(R.string.no_plan)
        daysLeft == null || emptyOn == null -> stringResource(R.string.lasts_very_long)
        status.level == Level.EMPTY -> stringResource(R.string.not_enough_for_a_day)
        else -> pluralStringResource(R.plurals.days_left, daysLeft, daysLeft) + " · " +
            stringResource(R.string.empty_on, formatDate(emptyOn))
    }
}

@Composable
private fun reorderText(status: StockCalculator.Status): String? {
    val reorderBy = status.reorderBy
    return when {
        status.level == Level.EMPTY || status.level == Level.REORDER -> stringResource(R.string.reorder_now)
        status.level == Level.OK && reorderBy != null -> stringResource(R.string.reorder_by, formatDate(reorderBy))
        else -> null
    }
}
