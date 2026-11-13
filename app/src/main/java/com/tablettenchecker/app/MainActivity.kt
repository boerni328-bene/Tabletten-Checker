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
import com.tablettenchecker.app.ui.EditScreen
import com.tablettenchecker.app.ui.MainViewModel
import com.tablettenchecker.app.ui.OverviewScreen
import com.tablettenchecker.app.ui.SettingsScreen
import com.tablettenchecker.app.ui.ShoppingScreen
import com.tablettenchecker.app.ui.TablettenTheme

// Seiten der App; „e:<Kennung>“ bearbeitet eine Tablette, „e:“ allein fügt eine neue hinzu.
private const val OVERVIEW = ""
private const val SETTINGS = "einstellungen"
private const val SHOPPING = "einkauf"
private const val EDIT = "e:"

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TablettenTheme {
                var screen by rememberSaveable { mutableStateOf(OVERVIEW) }
                val back = { screen = OVERVIEW }
                if (screen != OVERVIEW) BackHandler(onBack = back)
                val vm = viewModel
                when {
                    screen == OVERVIEW -> OverviewScreen(
                        medications = vm.medications,
                        nowMillis = vm.now,
                        plan = vm.plan,
                        showNoon = vm.settings.showNoon,
                        compact = vm.compact,
                        filter = vm.filter,
                        onToggleCompact = { vm.toggleCompact() },
                        onFilter = { vm.selectFilter(it) },
                        onAdd = { screen = EDIT },
                        onEdit = { screen = EDIT + it },
                        onAddPack = { vm.addPack(it) },
                        onShopping = { screen = SHOPPING },
                        onSettings = { screen = SETTINGS },
                    )
                    screen == SETTINGS -> SettingsScreen(
                        settings = vm.settings,
                        onSave = {
                            vm.updateSettings(it)
                            back()
                        },
                        onBack = back,
                        onExport = { vm.exportTo(it) },
                        onReadBackup = { vm.readBackup(it) },
                        onRestore = {
                            vm.restore(it)
                            back()
                        },
                    )
                    screen == SHOPPING -> ShoppingScreen(
                        medications = vm.medications,
                        nowMillis = vm.now,
                        plan = vm.plan,
                        supplyDays = vm.settings.supplyDays,
                        onBack = back,
                        onPurchased = {
                            vm.addPurchased(it)
                            back()
                        },
                    )
                    else -> {
                        val id = screen.removePrefix(EDIT)
                        val medication = vm.medications.firstOrNull { it.id == id }
                        EditScreen(
                            medication = medication,
                            currentStock = medication?.let { vm.currentStock(it) } ?: 0.0,
                            showNoon = vm.settings.showNoon,
                            defaultLead = vm.settings.leadDays,
                            onSave = {
                                vm.save(medication?.id, it)
                                back()
                            },
                            onDelete = {
                                back()
                                if (medication != null) vm.delete(medication.id)
                            },
                            onBack = back,
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshNow()
    }
}
