package com.notbraker.tracker.feature.toolkit.focustimer

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.notbraker.tracker.core.components.AppCard
import com.notbraker.tracker.core.designsystem.HabitColors
import com.notbraker.tracker.core.designsystem.TrackerTheme
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FocusTimerUiState(
    val selectedMinutes: Int = 25,
    val customMinutes: String = "",
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val sessionsToday: Int = 0,
    val focusMinutesToday: Int = 0
)

class FocusTimerViewModel(
    private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(FocusTimerUiState())
    val uiState: StateFlow<FocusTimerUiState> = _uiState.asStateFlow()
    private var tickerJob: Job? = null

    init {
        refreshStats()
    }

    fun selectPreset(minutes: Int) {
        _uiState.update { it.copy(selectedMinutes = minutes, remainingSeconds = minutes * 60) }
    }

    fun setCustomMinutes(value: String) {
        _uiState.update { it.copy(customMinutes = value.filter { c -> c.isDigit() }.take(3)) }
    }

    fun applyCustomMinutes() {
        val minutes = _uiState.value.customMinutes.toIntOrNull()?.coerceIn(1, 180) ?: return
        selectPreset(minutes)
    }

    fun start() {
        val minutes = _uiState.value.selectedMinutes
        FocusTimerService.start(context, minutes)
        _uiState.update { it.copy(isRunning = true, remainingSeconds = minutes * 60) }
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (_uiState.value.isRunning && _uiState.value.remainingSeconds > 0) {
                delay(1_000L)
                _uiState.update { current ->
                    current.copy(remainingSeconds = (current.remainingSeconds - 1).coerceAtLeast(0))
                }
            }
            if (_uiState.value.remainingSeconds == 0) {
                _uiState.update { it.copy(isRunning = false) }
                refreshStats()
            }
        }
    }

    fun stop() {
        tickerJob?.cancel()
        FocusTimerService.stop(context)
        _uiState.update { it.copy(isRunning = false) }
    }

    fun refreshStats() {
        val prefs = context.getSharedPreferences(FocusTimerService.STATS_PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now().toEpochDay()
        val day = prefs.getLong(FocusTimerService.KEY_DAY, today)
        val sessions = if (day == today) prefs.getInt(FocusTimerService.KEY_SESSIONS, 0) else 0
        val minutes = if (day == today) prefs.getInt(FocusTimerService.KEY_MINUTES, 0) else 0
        _uiState.update { it.copy(sessionsToday = sessions, focusMinutesToday = minutes) }
    }
}

class FocusTimerViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FocusTimerViewModel(context.applicationContext) as T
    }
}

@Composable
fun FocusTimerRoute(
    viewModel: FocusTimerViewModel = viewModel(
        factory = FocusTimerViewModelFactory(LocalContext.current)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FocusTimerScreen(
        state = state,
        onPreset = viewModel::selectPreset,
        onCustomMinutes = viewModel::setCustomMinutes,
        onApplyCustom = viewModel::applyCustomMinutes,
        onStart = viewModel::start,
        onStop = viewModel::stop
    )
}

@Composable
fun FocusTimerScreen(
    state: FocusTimerUiState,
    onPreset: (Int) -> Unit,
    onCustomMinutes: (String) -> Unit,
    onApplyCustom: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val spacing = TrackerTheme.spacing
    val minutes = state.remainingSeconds / 60
    val seconds = state.remainingSeconds % 60
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = HabitColors.BackgroundPrimary
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text("Focus Timer", style = MaterialTheme.typography.displayLarge)
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        listOf(25, 50).forEach { preset ->
                            Surface(
                                onClick = { onPreset(preset) },
                                shape = MaterialTheme.shapes.medium,
                                color = if (state.selectedMinutes == preset) HabitColors.SurfaceAccentGlow else HabitColors.Surface
                            ) {
                                Text(
                                    if (preset == 25) "25/5" else "50/10",
                                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = state.customMinutes,
                        onValueChange = onCustomMinutes,
                        label = { Text("Custom minutes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Surface(onClick = onApplyCustom, shape = MaterialTheme.shapes.medium, color = HabitColors.Surface) {
                        Text("Apply custom", modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm))
                    }
                    Text(String.format("%02d:%02d", minutes, seconds), style = MaterialTheme.typography.displayLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Surface(onClick = onStart, shape = MaterialTheme.shapes.medium, color = HabitColors.SurfaceAccentGlow) {
                            Text("Start", modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm))
                        }
                        Surface(onClick = onStop, shape = MaterialTheme.shapes.medium, color = HabitColors.Surface) {
                            Text("Stop", modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm))
                        }
                    }
                }
            }
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text("Today stats", style = MaterialTheme.typography.titleMedium)
                    Text("Sessions: ${state.sessionsToday}", style = MaterialTheme.typography.bodyLarge)
                    Text("Focus minutes: ${state.focusMinutesToday}", style = MaterialTheme.typography.bodyLarge)
                }
            }
            Row(modifier = Modifier.navigationBarsPadding()) { Text("") }
        }
    }
}
