package com.tablettenchecker.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tablettenchecker.app.R
import com.tablettenchecker.app.data.Medication
import com.tablettenchecker.app.data.Shopping
import com.tablettenchecker.app.data.StockCalculator
import com.tablettenchecker.app.data.StockCalculator.Level
import java.time.LocalDate

private typealias Row2 = Pair<Medication, StockCalculator.Status>

/** Abschnitt der kompakten Ansicht; [title] null = ohne Überschrift. */
private class Group(@StringRes val title: Int?, val rows: List<Row2>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    medications: List<Medication>,
    nowMillis: Long,
    plan: StockCalculator.Plan,
    showNoon: Boolean,
    compact: Boolean,
    filter: DoseFilter,
    onToggleCompact: () -> Unit,
    onFilter: (DoseFilter) -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onAddPack: (String) -> Unit,
    onShopping: () -> Unit,
    onSettings: () -> Unit,
) {
    val noonVisible = showNoon || medications.any { it.noon > 0.0 }
    val activeFilter = if (filter == DoseFilter.NOON && !noonVisible) DoseFilter.ALL else filter
    val all = remember(medications, nowMillis, plan) {
        medications.map { it to StockCalculator.status(it, nowMillis, plan = plan) }
    }
    val today = localDate(nowMillis)
    val trip = remember(all, today, plan) { Shopping.nextTrip(all, today, plan.leadDays) }
    val shown = all.filter { activeFilter.includes(it.first) }
    var packFor by remember { mutableStateOf<Medication?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(onClick = onShopping) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = stringResource(R.string.shopping_title))
                        }
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                        }
                    },
                )
                if (medications.isNotEmpty()) {
                    FilterRow(activeFilter, noonVisible, compact, onFilter, onToggleCompact)
                }
            }
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
        if (medications.isEmpty()) {
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
            return@Scaffold
        }
        val side = if (compact) 8.dp else 16.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = side,
                end = side,
                top = padding.calculateTopPadding() + 4.dp,
                // Platz für die Schaltfläche „Tablette hinzufügen“.
                bottom = padding.calculateBottomPadding() + if (compact) 80.dp else 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 12.dp),
        ) {
            if (trip != null) {
                item(key = "einkauf") { TripBanner(trip, today, onShopping) }
            }
            if (shown.isEmpty()) {
                item(key = "leer") {
                    Text(stringResource(R.string.empty_filter), modifier = Modifier.padding(16.dp))
                }
            } else if (compact) {
                item(key = "kopfzeile") { CompactHeader() }
                compactGroups(shown, activeFilter).forEach { group ->
                    group.title?.let { title -> item(key = "gruppe-$title") { GroupTitle(title) } }
                    items(group.rows, key = { it.first.id }) { (medication, status) ->
                        CompactRow(medication, status, activeFilter, today) { onEdit(medication.id) }
                    }
                }
            } else {
                // Karten: Dringendes zuerst, was am frühesten aufgebraucht ist, steht oben.
                val sorted = shown.sortedWith(
                    compareBy({ it.second.daysLeft ?: Int.MAX_VALUE }, { it.first.name.lowercase() })
                )
                items(sorted, key = { it.first.id }) { (medication, status) ->
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

/** Alle: Abschnitte Morgens, Mittags, Abends (jede Tablette in ihrem ersten); sonst eine Liste. Je nach Name. */
private fun compactGroups(rows: List<Row2>, filter: DoseFilter): List<Group> {
    val byName = rows.sortedBy { it.first.name.lowercase() }
    if (filter != DoseFilter.ALL) return listOf(Group(null, byName))
    val titles = listOf(R.string.filter_morning, R.string.filter_noon, R.string.filter_evening, R.string.group_none)
    return byName
        .groupBy { (med, _) ->
            when {
                med.morning > 0.0 -> 0
                med.noon > 0.0 -> 1
                med.evening > 0.0 -> 2
                else -> 3
            }
        }
        .toSortedMap()
        .map { (slot, list) -> Group(titles[slot], list) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(
    active: DoseFilter,
    noonVisible: Boolean,
    compact: Boolean,
    onFilter: (DoseFilter) -> Unit,
    onToggleCompact: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DoseFilter.entries.filter { it != DoseFilter.NOON || noonVisible }.forEach { option ->
                FilterChip(
                    selected = option == active,
                    onClick = { onFilter(option) },
                    label = { Text(stringResource(option.label)) },
                )
            }
        }
        // Die Schaltfläche nennt die Ansicht, zu der sie wechselt.
        TextButton(onClick = onToggleCompact) {
            Text(stringResource(if (compact) R.string.view_cards else R.string.view_compact))
        }
    }
}

/** Hinweis oben: nächster Einkauf und was dabei gleich mitkommt. */
@Composable
private fun TripBanner(trip: Shopping.Trip, today: LocalDate, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ShoppingCart, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = if (trip.date.isAfter(today)) {
                        stringResource(R.string.trip_by, formatDate(trip.date))
                    } else {
                        stringResource(R.string.trip_now)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = trip.medications.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun GroupTitle(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 2.dp),
    )
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
            DoseChips(medication, DoseFilter.ALL, modifier = Modifier.padding(top = 4.dp))
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.col_name), style = style, color = color, modifier = Modifier.weight(1f))
        listOf(
            R.string.col_stock to STOCK_WIDTH,
            R.string.col_days to DAYS_WIDTH,
            R.string.col_reorder to REORDER_WIDTH,
        ).forEach { (text, width) ->
            Text(
                stringResource(text),
                style = style,
                color = color,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(width),
            )
        }
    }
}

/** Eine Tablette als Zeile: Name und Stärke, Einnahme-Marken, Bestand, Tage Reichweite, Nachkauf-Datum. */
@Composable
private fun CompactRow(
    medication: Medication,
    status: StockCalculator.Status,
    filter: DoseFilter,
    today: LocalDate,
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
    val reorderBy = status.reorderBy
    val reorder = when {
        urgent -> stringResource(R.string.reorder_now_short)
        reorderBy != null -> formatDateShort(reorderBy, today)
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = medication.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (medication.note.isNotBlank()) {
                        Text(
                            text = "  " + medication.note,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                DoseChips(medication, filter, modifier = Modifier.padding(top = 2.dp))
            }
            Text(
                text = formatAmount(status.current),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(STOCK_WIDTH),
            )
            Text(
                text = if (status.level == Level.EMPTY) "0" else status.daysLeft?.toString() ?: "–",
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
