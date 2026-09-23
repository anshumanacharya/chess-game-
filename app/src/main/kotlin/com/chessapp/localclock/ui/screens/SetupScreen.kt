package com.chessapp.localclock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chessapp.engine.ClockConfig
import com.chessapp.engine.Color

private enum class ClockMode { TIMED, UNLIMITED }
private enum class Opponent { HUMAN, COMPUTER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onStartGame: (clockConfig: ClockConfig, botColor: Color?) -> Unit
) {
    // rememberSaveable (not remember): survives the Activity being recreated, e.g. by a dark
    // mode switch, instead of silently resetting the player's choices.
    var clockMode by rememberSaveable { mutableStateOf(ClockMode.TIMED) }
    var selectedPresetIndex by rememberSaveable { mutableIntStateOf(3) } // "10 min"
    var useCustom by rememberSaveable { mutableStateOf(false) }
    var customMinutes by rememberSaveable { mutableIntStateOf(15) }
    var customIncrement by rememberSaveable { mutableIntStateOf(0) }
    var opponent by rememberSaveable { mutableStateOf(Opponent.HUMAN) }
    var humanPlaysWhite by rememberSaveable { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxHeight()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Local Chess", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Two players, one device – lay it flat between you",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        SectionHeader("Opponent")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = opponent == Opponent.HUMAN,
                onClick = { opponent = Opponent.HUMAN },
                label = { Text("Pass and play") }
            )
            FilterChip(
                selected = opponent == Opponent.COMPUTER,
                onClick = { opponent = Opponent.COMPUTER },
                label = { Text("Computer (~1000 Elo)") }
            )
        }

        if (opponent == Opponent.COMPUTER) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = humanPlaysWhite,
                    onClick = { humanPlaysWhite = true },
                    label = { Text("Play as White") }
                )
                FilterChip(
                    selected = !humanPlaysWhite,
                    onClick = { humanPlaysWhite = false },
                    label = { Text("Play as Black") }
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        SectionHeader("Chess clock")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = clockMode == ClockMode.TIMED,
                onClick = { clockMode = ClockMode.TIMED },
                label = { Text("Timed") }
            )
            FilterChip(
                selected = clockMode == ClockMode.UNLIMITED,
                onClick = { clockMode = ClockMode.UNLIMITED },
                label = { Text("Unlimited time") }
            )
        }

        if (clockMode == ClockMode.TIMED) {
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(96.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ClockConfig.PRESETS.size) { index ->
                    val (label, _) = ClockConfig.PRESETS[index]
                    FilterChip(
                        selected = !useCustom && selectedPresetIndex == index,
                        onClick = {
                            useCustom = false
                            selectedPresetIndex = index
                        },
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    FilterChip(
                        selected = useCustom,
                        onClick = { useCustom = true },
                        label = { Text("Custom") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (useCustom) {
                Spacer(Modifier.height(12.dp))
                NumberStepper(
                    label = "Minutes per side",
                    value = customMinutes,
                    range = 1..90,
                    onValueChange = { customMinutes = it }
                )
                Spacer(Modifier.height(8.dp))
                NumberStepper(
                    label = "Increment (seconds)",
                    value = customIncrement,
                    range = 0..60,
                    onValueChange = { customIncrement = it }
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val config = when {
                    clockMode == ClockMode.UNLIMITED -> ClockConfig.UNLIMITED
                    useCustom -> ClockConfig.preset(customMinutes, customIncrement)
                    else -> ClockConfig.PRESETS[selectedPresetIndex].second
                }
                val botColor = if (opponent == Opponent.COMPUTER) {
                    if (humanPlaysWhite) Color.BLACK else Color.WHITE
                } else {
                    null
                }
                onStartGame(config, botColor)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors()
        ) {
            Text("Start game", style = MaterialTheme.typography.titleMedium)
        }
    }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

@Composable
private fun NumberStepper(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (value - 1 >= range.first) onValueChange(value - 1) }) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease")
            }
            Text("$value", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { if (value + 1 <= range.last) onValueChange(value + 1) }) {
                Icon(Icons.Filled.Add, contentDescription = "Increase")
            }
        }
    }
}
