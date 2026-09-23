package com.chessapp.localclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chessapp.engine.ClockConfig
import com.chessapp.engine.Color
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

@Composable
private fun ChessApp() {
    val viewModel: GameViewModel = viewModel()

    // Which screen shows is the ViewModel's call, not a `remember`ed flag here: the Activity is
    // recreated on some configuration changes (switching dark mode, for one), which would reset
    // a flag held in composition back to Setup mid-game while the game itself kept running.
    if (viewModel.isInGame) {
        GameScreen(
            viewModel = viewModel,
            onBackToSetup = viewModel::leaveGame
        )
    } else {
        SetupScreen(
            onStartGame = { config: ClockConfig, botColor: Color? ->
                viewModel.startNewGame(config, botColor)
            }
        )
    }
}
