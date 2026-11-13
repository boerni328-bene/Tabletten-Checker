package com.tablettenchecker.app.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tablettenchecker.app.R
import com.tablettenchecker.app.data.Medication
import com.tablettenchecker.app.data.Shopping
import com.tablettenchecker.app.data.StockCalculator
import com.tablettenchecker.app.data.StockCalculator.Level
import java.time.LocalDate

/** Einkaufsliste: was bis wann zu kaufen ist, wie viele Packungen, abhaken, teilen, als gekauft eintragen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    medications: List<Medication>,
    nowMillis: Long,
    plan: StockCalculator.Plan,
    supplyDays: Int,
    onBack: () -> Unit,
    onPurchased: (Map<String, Double>) -> Unit,
) {
    val today = localDate(nowMillis)
    val rows = remember(medications, nowMillis, plan) {
        medications.map { it to StockCalculator.status(it, nowMillis, plan = plan) }
    }
    val trip = remember(rows, today, plan) { Shopping.nextTrip(rows, today, plan.leadDays) }
    val items = remember(rows, today, supplyDays) { Shopping.items(rows, supplyDays, today) }
    val tripIds = trip?.medications?.map { it.id }?.toSet().orEmpty()
    val (soon, later) = items.partition { it.medication.id in tripIds }
    // Was beim nächsten Einkauf fällig ist, ist schon abgehakt.
    var checked by remember(tripIds) { mutableStateOf(soon.map { it.medication.id }.toSet()) }
    var confirm by remember { mutableStateOf(false) }
    val chosen = items.filter { it.medication.id in checked }
    val context = LocalContext.current
    val toggle = { id: String -> checked = if (id in checked) checked - id else checked + id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shopping_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { share(context, chosen, today) }, enabled = chosen.isNotEmpty()) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share))
                    }
                },
            )
        },
        bottomBar = {
            if (items.isNotEmpty()) {
                Box(modifier = Modifier.navigationBarsPadding().padding(16.dp)) {
                    Button(
                        onClick = { confirm = true },
                        enabled = chosen.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.shopping_purchased)) }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "einleitung") {
                Text(
                    text = stringResource(R.string.shopping_intro, supplyDays),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (items.isEmpty()) {
                item(key = "leer") {
                    Text(stringResource(R.string.shopping_empty), modifier = Modifier.padding(vertical = 16.dp))
                }
            }
            if (soon.isNotEmpty() && trip != null) {
                item(key = "titel-bald") {
                    SectionTitle(
                        if (trip.date.isAfter(today)) {
                            stringResource(R.string.shopping_by, formatDate(trip.date))
                        } else {
                            stringResource(R.string.shopping_now)
                        }
                    )
                }
                items(soon, key = { it.medication.id }) { item ->
                    ShoppingRow(item, item.medication.id in checked) { toggle(item.medication.id) }
                }
            }
            if (later.isNotEmpty()) {
                item(key = "titel-spaeter") { SectionTitle(stringResource(R.string.shopping_later)) }
                items(later, key = { it.medication.id }) { item ->
                    ShoppingRow(item, item.medication.id in checked) { toggle(item.medication.id) }
                }
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.purchased_title)) },
            text = { Text(stringResource(R.string.purchased_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onPurchased(chosen.associate { it.medication.id to Shopping.purchaseAmount(it) })
                }) { Text(stringResource(R.string.add)) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun ShoppingRow(item: Shopping.Item, checked: Boolean, onToggle: () -> Unit) {
    val medication = item.medication
    Surface(
        onClick = onToggle,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (medication.note.isBlank()) medication.name else "${medication.name} · ${medication.note}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                val packs = item.packs
                Text(
                    text = if (packs != null) {
                        pluralStringResource(R.plurals.packs, packs, packs, medication.packSize)
                    } else {
                        stringResource(R.string.pieces, formatAmount(item.needed))
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                val reorderBy = item.status.reorderBy
                Text(
                    text = when {
                        item.status.level == Level.EMPTY || item.status.level == Level.REORDER ->
                            stringResource(R.string.reorder_now)
                        reorderBy != null -> stringResource(R.string.reorder_by, formatDate(reorderBy))
                        else -> stringResource(R.string.lasts_very_long)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** Abgehakte Tabletten als Text an eine andere App geben (z. B. Nachricht an die Apotheke). */
private fun share(context: Context, items: List<Shopping.Item>, today: LocalDate) {
    val res = context.resources
    val lines = items.joinToString("\n") { item ->
        val med = item.medication
        val name = listOf(med.name, med.note).filter { it.isNotBlank() }.joinToString(" ")
        val amount = item.packs?.let { res.getQuantityString(R.plurals.packs, it, it, med.packSize) }
            ?: res.getString(R.string.pieces, formatAmount(item.needed))
        "• $name: $amount"
    }
    val text = res.getString(R.string.share_title, formatDate(today)) + "\n" + lines
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, null))
}
