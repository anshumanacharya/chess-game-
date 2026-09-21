package com.chessapp.localclock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chessapp.engine.ClockConfig

private enum class ClockMode { TIMED, UNLIMITED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onStartGame: (clockConfig: ClockConfig, flipBoardEachTurn: Boolean) -> Unit
) {
    var clockMode by remember { mutableStateOf(ClockMode.TIMED) }
    var selectedPresetIndex by remember { mutableStateOf(3) } // "10 min"
    var useCustom by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableIntStateOf(15) }
    var customIncrement by remember { mutableIntStateOf(0) }
    var flipBoardEachTurn by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Local Chess", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Two players, one device",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

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

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        SectionHeader("Board")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Flip board each turn")
                Text(
                    "Each player sees the board from their own side",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = flipBoardEachTurn, onCheckedChange = { flipBoardEachTurn = it })
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val config = when {
                    clockMode == ClockMode.UNLIMITED -> ClockConfig.UNLIMITED
                    useCustom -> ClockConfig.preset(customMinutes, customIncrement)
                    else -> ClockConfig.PRESETS[selectedPresetIndex].second
                }
                onStartGame(config, flipBoardEachTurn)
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
