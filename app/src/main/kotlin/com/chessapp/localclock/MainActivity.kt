package com.chessapp.localclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chessapp.engine.ClockConfig
import com.chessapp.localclock.ui.screens.GameScreen
import com.chessapp.localclock.ui.screens.SetupScreen
import com.chessapp.localclock.ui.theme.LocalChessClockTheme
import com.chessapp.localclock.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocalChessClockTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ChessApp()
                }
            }
        }
    }
}

private sealed class Screen {
    data object Setup : Screen()
    data object Game : Screen()
}

@Composable
private fun ChessApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Setup) }
    val viewModel: GameViewModel = viewModel()

    when (screen) {
        is Screen.Setup -> SetupScreen(
            onStartGame = { config: ClockConfig ->
                viewModel.startNewGame(config)
                screen = Screen.Game
            }
        )
        is Screen.Game -> GameScreen(
            viewModel = viewModel,
            onBackToSetup = { screen = Screen.Setup }
        )
    }
}
