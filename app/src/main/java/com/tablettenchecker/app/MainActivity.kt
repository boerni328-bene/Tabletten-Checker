package com.tablettenchecker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.tablettenchecker.app.data.StockCalculator
import com.tablettenchecker.app.ui.EditScreen
import com.tablettenchecker.app.ui.MainViewModel
import com.tablettenchecker.app.ui.OverviewScreen
import com.tablettenchecker.app.ui.TablettenTheme

/** Kennung für „neue Tablette“ auf der Bearbeiten-Seite. */
private const val NEW = ""

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TablettenTheme {
                // null = Übersicht, NEW = neue Tablette, sonst die Kennung der bearbeiteten Tablette.
                var editing by rememberSaveable { mutableStateOf<String?>(null) }
                val target = editing
                if (target == null) {
                    OverviewScreen(
                        medications = viewModel.medications,
                        nowMillis = viewModel.now,
                        compact = viewModel.compact,
                        onToggleCompact = { viewModel.toggleCompact() },
                        onAdd = { editing = NEW },
                        onEdit = { editing = it },
                        onAddPack = { viewModel.addPack(it) },
                    )
                } else {
                    BackHandler { editing = null }
                    val medication = viewModel.medications.firstOrNull { it.id == target }
                    EditScreen(
                        medication = medication,
                        currentStock = medication?.let { StockCalculator.currentStock(it, viewModel.now) } ?: 0.0,
                        onSave = {
                            viewModel.save(medication?.id, it)
                            editing = null
                        },
                        onDelete = {
                            editing = null
                            if (medication != null) viewModel.delete(medication.id)
                        },
                        onBack = { editing = null },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshNow()
    }
}
