package com.tablettenchecker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextOverflow
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
    compact: Boolean,
    onToggleCompact: () -> Unit,
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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    // Die Schaltfläche nennt die Ansicht, zu der sie wechselt.
                    TextButton(onClick = onToggleCompact) {
                        Text(stringResource(if (compact) R.string.view_cards else R.string.view_compact))
                    }
                },
            )
        },
        floatingActionButton = {
            if (compact) {
                // Klein, damit sie in der kompakten Ansicht möglichst wenig verdeckt.
                FloatingActionButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_medication))
                }
            } else {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.add_medication)) },
                )
            }
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
        } else if (compact) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 80.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item(key = "kopfzeile") { CompactHeader() }
                items(rows, key = { it.first.id }) { (medication, status) ->
                    CompactRow(medication = medication, status = status, onClick = { onEdit(medication.id) })
                }
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

private val STOCK_WIDTH = 60.dp
private val DAYS_WIDTH = 48.dp
private val REORDER_WIDTH = 76.dp

/** Spaltenüberschriften der kompakten Ansicht. */
@Composable
private fun CompactHeader() {
    val style = MaterialTheme.typography.labelMedium
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.col_name), style = style, color = color, modifier = Modifier.weight(1f))
        Text(
            stringResource(R.string.col_stock),
            style = style,
            color = color,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(STOCK_WIDTH),
        )
        Text(
            stringResource(R.string.col_days),
            style = style,
            color = color,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(DAYS_WIDTH),
        )
        Text(
            stringResource(R.string.col_reorder),
            style = style,
            color = color,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(REORDER_WIDTH),
        )
    }
}

/** Eine Tablette als Zeile: Name, Bestand, Tage Reichweite, Nachkauf-Datum. */
@Composable
private fun CompactRow(
    medication: Medication,
    status: StockCalculator.Status,
    onClick: () -> Unit,
) {
    val urgent = status.level == Level.EMPTY || status.level == Level.REORDER
    val container = when (status.level) {
        Level.EMPTY -> MaterialTheme.colorScheme.errorContainer
        Level.REORDER -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val content = when (status.level) {
        Level.EMPTY -> MaterialTheme.colorScheme.onErrorContainer
        Level.REORDER -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val doses = stringResource(
        R.string.dose_short,
        formatAmount(medication.morning),
        formatAmount(medication.evening),
    )
    val reorderBy = status.reorderBy
    val reorder = when {
        urgent -> stringResource(R.string.reorder_now_short)
        reorderBy != null -> formatDateShort(reorderBy)
        else -> "–"
    }
    Surface(
        onClick = onClick,
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medication.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (medication.note.isBlank()) doses else "$doses · ${medication.note}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = formatAmount(status.current),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(STOCK_WIDTH),
            )
            Text(
                text = when {
                    status.level == Level.EMPTY -> "0"
                    else -> status.daysLeft?.toString() ?: "–"
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(DAYS_WIDTH),
            )
            Text(
                text = reorder,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (urgent) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(REORDER_WIDTH),
            )
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
