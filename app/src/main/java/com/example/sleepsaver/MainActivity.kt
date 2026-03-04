package com.example.sleepsaver

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sleepsaver.ui.theme.SleepSaverTheme
import com.example.sleepsaver.worker.WorkScheduler

class MainActivity : ComponentActivity() {
    private val viewModel: SleepViewModel by viewModels {
        SleepViewModelFactory((application as SleepSaverApplication).appContainer)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WorkScheduler.scheduleSleepChecks(applicationContext)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
            SleepSaverTheme(darkTheme = settings.manualDarkMode) {
                LaunchedEffect(dashboard.isSleepModeActive) {
                    applySleepBrightness(dashboard.isSleepModeActive)
                }
                SleepSaverApp(viewModel = viewModel)
            }
        }
    }

    private fun applySleepBrightness(isSleepMode: Boolean) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = if (isSleepMode) 0.05f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = layoutParams
    }
}

private enum class Screen(val route: String, val title: String) {
    Dashboard("dashboard", "Dashboard"),
    Context("context", "Context Logic"),
    Privacy("privacy", "Privacy"),
    Settings("settings", "Settings")
}

@Composable
private fun SleepSaverApp(viewModel: SleepViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val screens = Screen.entries

    Scaffold(
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    val selected = backStack?.destination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(screen.title) },
                        icon = {}
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(Screen.Dashboard.route) {
                val state by viewModel.dashboard.collectAsStateWithLifecycle()
                DashboardScreen(state)
            }
            composable(Screen.Context.route) { ContextLogicScreen() }
            composable(Screen.Privacy.route) { PrivacyScreen(onDeleteData = viewModel::clearHistory) }
            composable(Screen.Settings.route) {
                val settings by viewModel.settings.collectAsStateWithLifecycle()
                SettingsScreen(
                    settings = settings,
                    onDetectionEnabledChanged = viewModel::onDetectionEnabledChanged,
                    onLightThresholdChanged = viewModel::onLightThresholdChanged,
                    onWindowStartChanged = viewModel::onWindowStartChanged,
                    onWindowEndChanged = viewModel::onWindowEndChanged,
                    onAutoDndChanged = viewModel::onAutoDndChanged,
                    onDarkModeChanged = viewModel::onDarkModeChanged
                )
            }
        }
    }
}

@Composable
private fun DashboardScreen(state: DashboardUiState) {
    val isTabletLayout = LocalConfiguration.current.screenWidthDp >= 700
    if (isTabletLayout) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusCard(state)
                SensorsCard(state)
            }
            WeeklySummaryCard(state, Modifier.weight(1f))
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { StatusCard(state) }
            item { SensorsCard(state) }
            item { WeeklySummaryCard(state, Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
private fun StatusCard(state: DashboardUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (state.isSleepModeActive) "Sleep Mode Active" else "Awake",
                style = MaterialTheme.typography.titleLarge
            )
            Text("Tonight's sleep start: ${state.tonightStartTime}")
            Text("Disturbances: ${state.disturbanceCountTonight}")
            Text("Consistency score: ${state.consistencyScore}")
        }
    }
}

@Composable
private fun SensorsCard(state: DashboardUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Context snapshot", style = MaterialTheme.typography.titleMedium)
            Text("Ambient light: ${state.ambientLightLabel}")
            Text("Charging: ${state.chargingLabel}")
            Text("Placement: ${state.faceDownLabel}")
        }
    }
}

@Composable
private fun WeeklySummaryCard(state: DashboardUiState, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Weekly summary", style = MaterialTheme.typography.titleMedium)
            if (state.weeklySummary.isEmpty()) {
                Text("No sleep sessions recorded yet.")
            } else {
                state.weeklySummary.forEach { night ->
                    Text("${night.startLabel} - ${night.durationMinutes} min, ${night.disturbances} disturbances")
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: com.example.sleepsaver.domain.AppSettings,
    onDetectionEnabledChanged: (Boolean) -> Unit,
    onLightThresholdChanged: (Float) -> Unit,
    onWindowStartChanged: (Int) -> Unit,
    onWindowEndChanged: (Int) -> Unit,
    onAutoDndChanged: (Boolean) -> Unit,
    onDarkModeChanged: (Boolean) -> Unit
) {
    var lightThreshold by remember(settings.lightThresholdLux) { mutableStateOf(settings.lightThresholdLux) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingToggle("Sleep detection", settings.sleepDetectionEnabled, onDetectionEnabledChanged)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Light threshold: ${"%.1f".format(lightThreshold)} lux")
                    Slider(
                        value = lightThreshold,
                        onValueChange = {
                            lightThreshold = it
                            onLightThresholdChanged(it)
                        },
                        valueRange = 1f..50f
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sleep window start hour: ${settings.sleepWindowStartHour}:00")
                    Slider(
                        value = settings.sleepWindowStartHour.toFloat(),
                        onValueChange = { onWindowStartChanged(it.toInt()) },
                        valueRange = 0f..23f,
                        steps = 22
                    )
                    Text("Sleep window end hour: ${settings.sleepWindowEndHour}:00")
                    Slider(
                        value = settings.sleepWindowEndHour.toFloat(),
                        onValueChange = { onWindowEndChanged(it.toInt()) },
                        valueRange = 0f..23f,
                        steps = 22
                    )
                }
            }
        }
        item { SettingToggle("Auto Do Not Disturb", settings.autoDndEnabled, onAutoDndChanged) }
        item { SettingToggle("Manual dark mode", settings.manualDarkMode, onDarkModeChanged) }
    }
}

@Composable
private fun SettingToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun ContextLogicScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Text("How sensor fusion works", style = MaterialTheme.typography.titleLarge) }
        item { Text("- Low ambient light suggests lights-off behavior.") }
        item { Text("- Proximity near-range is used as a face-down signal.") }
        item { Text("- Charging state indicates bedtime routine.") }
        item { Text("- Time window narrows detection to likely sleep hours.") }
        item { Text("Sleep Mode is activated only when all signals align.") }
    }
}

@Composable
private fun PrivacyScreen(onDeleteData: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Text("Privacy by design", style = MaterialTheme.typography.titleLarge) }
        item { Text("- Light sensor values are not images and are not stored continuously.") }
        item { Text("- Proximity sensor does not identify people.") }
        item { Text("- No audio recording and no location collection.") }
        item { Text("- Data remains local in on-device Room storage.") }
        item { Text("- You can delete all history from this screen.") }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Delete local sleep history")
                    Switch(checked = false, onCheckedChange = { if (it) onDeleteData() })
                }
            }
        }
    }
}