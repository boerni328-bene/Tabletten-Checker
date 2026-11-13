package com.tablettenchecker.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tablettenchecker.app.R
import com.tablettenchecker.app.data.Medication

/** Auswahl oben in der Übersicht: alle Tabletten oder nur die einer Tageszeit. */
enum class DoseFilter(@StringRes val label: Int, @StringRes val chip: Int) {
    ALL(R.string.filter_all, R.string.filter_all),
    MORNING(R.string.filter_morning, R.string.chip_morning),
    NOON(R.string.filter_noon, R.string.chip_noon),
    EVENING(R.string.filter_evening, R.string.chip_evening);

    fun includes(med: Medication): Boolean = when (this) {
        ALL -> true
        MORNING -> med.morning > 0.0
        NOON -> med.noon > 0.0
        EVENING -> med.evening > 0.0
    }

    fun amount(med: Medication): Double = when (this) {
        ALL -> 0.0
        MORNING -> med.morning
        NOON -> med.noon
        EVENING -> med.evening
    }
}

/**
 * Einnahmemengen als farbige Marken („morgens 1“, „abends 0,5“), damit sie sich klar vom Bestand abheben.
 * Mit [filter] nur die Marke dieser Tageszeit.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoseChips(medication: Medication, filter: DoseFilter, modifier: Modifier = Modifier) {
    val slots = listOf(DoseFilter.MORNING, DoseFilter.NOON, DoseFilter.EVENING)
        .filter { it.amount(medication) > 0.0 && (filter == DoseFilter.ALL || filter == it) }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (slots.isEmpty()) {
            Text(stringResource(R.string.no_dose), style = MaterialTheme.typography.bodySmall)
        }
        slots.forEach { slot ->
            val (background, content) = doseColors(slot)
            Surface(color = background, contentColor = content, shape = RoundedCornerShape(50)) {
                Text(
                    text = stringResource(slot.chip, formatAmount(slot.amount(medication))),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
                )
            }
        }
        if (medication.everyDays > 1) {
            Text(
                text = pluralStringResource(R.plurals.every_days, medication.everyDays, medication.everyDays),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
