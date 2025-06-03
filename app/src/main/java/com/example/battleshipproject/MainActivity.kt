package com.example.battleshipproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.battleshipproject.ui.screens.GameScreen
import com.example.battleshipproject.ui.theme.BattleshipProjectTheme
import com.example.battleshipproject.viewmodel.BattleshipViewModel
// jibin and sameh
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BattleshipProjectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: BattleshipViewModel = viewModel()
                    GameScreen(viewModel = viewModel)
                }
            }
        }
    }
}