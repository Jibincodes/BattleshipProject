package com.example.battleshipproject.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.battleshipproject.models.Position
import com.example.battleshipproject.models.Ship
import com.example.battleshipproject.models.ShipType
import com.example.battleshipproject.viewmodel.BattleshipViewModel
import com.example.battleshipproject.viewmodel.GameState
// GameScreen has been generated with the help of AI tools like Gemini and Claude

@Composable
fun GameScreen(viewModel: BattleshipViewModel) {
    val gameState by viewModel.gameState.observeAsState(GameState.Setup)
    val errorMessage by viewModel.errorMessage.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(false)
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error messages as snackbars
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (gameState) {
                GameState.Setup -> SetupScreen(viewModel)
                GameState.InProgress -> GamePlayScreen(viewModel)
                GameState.GameOver -> GameOverScreen(viewModel)
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
//SetupScreen with AI - Jibin
@Composable
fun SetupScreen(viewModel: BattleshipViewModel) {
    var playerNameInput by remember { mutableStateOf("") }
    var gameKeyInput by remember { mutableStateOf("") }
    val ships by viewModel.ships.observeAsState(emptyList())
    val boardSize = 10
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Battleship Game Setup",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = gameKeyInput,
                onValueChange = { 
                    gameKeyInput = it
                    viewModel.setGameKey(it)
                },
                label = { Text("Game key") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            )
            
            OutlinedTextField(
                value = playerNameInput,
                onValueChange = { 
                    playerNameInput = it
                    viewModel.setPlayerName(it)
                },
                label = { Text("Player") },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    viewModel.pingServer()
                },
                modifier = Modifier.padding(4.dp)
            ) {
                Text("Test Connection")
            }
            
            Button(
                onClick = {
                    viewModel.generateRandomShips(boardSize)
                },
                modifier = Modifier.padding(4.dp)
            ) {
                Text("Generate Ships")
            }
        }
        
        if (ships.isNotEmpty()) {
            Text("Your Ships:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            
            BattleshipBoard(
                boardSize = boardSize,
                ships = ships,
                shots = emptyList(),
                isEnemyBoard = false,
                onCellClick = { _, _ -> /* No action during setup */ }
            )
        }
        
        Button(
            onClick = {
                viewModel.joinGame()
            },
            enabled = playerNameInput.length >= 3 && gameKeyInput.length >= 3 && ships.size == 5,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Start")
        }
    }
}
// Playscreen with AI - Jibin
@Composable
fun GamePlayScreen(viewModel: BattleshipViewModel) {
    val isYourTurn by viewModel.isYourTurn.observeAsState(false)
    val isLoading by viewModel.isLoading.observeAsState(false)
    val ships by viewModel.ships.observeAsState(emptyList())
    val yourShots by viewModel.yourShots.observeAsState(emptyList())
    val enemyShots by viewModel.enemyShots.observeAsState(emptyList())
    val sunkenShips by viewModel.sunkenShips.observeAsState(emptyList())
    val playerName by viewModel.playerName.observeAsState("")
    val shipSunkAnnouncement by viewModel.shipSunkAnnouncement.observeAsState()
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Ship sunk announcement
        shipSunkAnnouncement?.let { announcement ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text(
                    text = announcement,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Enemy's board (where player shoots)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
        ) {
            Column(
                modifier = Modifier.padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enemy",
                        color = Color.Red,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .rotate(90f)
                            .padding(4.dp)
                    )
                    
                    EnemyBoard(
                        boardSize = 10,
                        yourShots = yourShots,
                        onCellClick = { x, y ->
                            if (isYourTurn) {
                                viewModel.fireAtPosition(Position(x, y))
                            }
                        }
                    )
                    
                    Column(
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = "Instructions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        Text(
                            text = if (isYourTurn) "Your turn: Click to fire" else "Waiting...",
                            fontSize = 12.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Ships sunk",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        Text(
                            text = sunkenShips?.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "--",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        
        // Status indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (!isYourTurn) "Waiting for enemy move..." else "Processing...",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = if (isYourTurn) "Your turn: Click on enemy board to fire" else "Waiting for opponent...",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isYourTurn) Color.Green else Color.Red
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Instructions and Ships sunk section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left column - Instructions
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Instructions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = if (isYourTurn) "Tap on enemy grid to fire" else "Wait for opponent's move",
                    fontSize = 12.sp
                )
            }
            
            // Right column - Ships sunk
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Ships sunk",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = sunkenShips?.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "--",
                    fontSize = 12.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Player's board (where enemy shoots)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Green)
        ) {
            Column(
                modifier = Modifier.padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Friendly",
                        color = Color.Green,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .rotate(90f)
                            .padding(4.dp)
                    )
                    
                    BattleshipBoard(
                        boardSize = 10,
                        ships = ships,
                        shots = enemyShots,
                        isEnemyBoard = false,
                        onCellClick = { _, _ -> /* No action on own board */ }
                    )
                    
                    Column(
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = "Instructions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        Text(
                            text = "Defend your ships!",
                            fontSize = 12.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Ships sunk",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        Text(
                            text = sunkenShips?.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "--",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
//Gameover Screen with AI- Jibin
@Composable
fun GameOverScreen(viewModel: BattleshipViewModel) {
    val isGameOver by viewModel.isGameOver.observeAsState(false)
    val isGameWon by viewModel.isGameWon.observeAsState(false)
    val sunkenShips by viewModel.sunkenShips.observeAsState(emptyList())
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { //werent able to figure out a solution to display you won for the winner, but we settles down just to ship sinking, due to gameover boolean
        Text(
            text = if (isGameWon) "You Won!" else "Game Over",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
            color = if (isGameWon) Color(0xFF4CAF50) else Color.Red
        )
        //clearly all 5 ships are stated for winner , hence it is clear for the winner
        if (sunkenShips.isNotEmpty()) {
            Text(
                text = "Ships you've sunk: ${sunkenShips.joinToString(", ")}",
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        Button(
            onClick = { viewModel.resetGame() },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Play Again")
        }
    }
}
//BattleshipBoard with AI - Jibin
@Composable
fun BattleshipBoard(
    boardSize: Int,
    ships: List<Ship>,
    shots: List<Position>,
    isEnemyBoard: Boolean,
    onCellClick: (Int, Int) -> Unit
) {
    // Generate all ship positions - Jibin
    val shipPositionMap = mutableMapOf<Pair<Int, Int>, Char>()
    ships.forEach { ship ->
        val letter = when (ship.type) {
            ShipType.CARRIER -> 'C'
            ShipType.BATTLESHIP -> 'B'
            ShipType.DESTROYER -> 'D'
            ShipType.SUBMARINE -> 'S'
            ShipType.PATROL_BOAT -> 'P'
        }
        
        ship.getAllPositions().forEach { pos ->
            shipPositionMap[Pair(pos.x, pos.y)] = letter
        }
    }
    
    val shotsSet = shots.map { Pair(it.x, it.y) }.toSet()
    
    // Create board positions
    val positions = List(boardSize) { y -> 
        List(boardSize) { x -> 
            Pair(x, y)
        }
    }.flatten()
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(boardSize),
        modifier = Modifier
            .size(200.dp)
            .aspectRatio(1f)
            .border(1.dp, Color.Black)
    ) {
        items(positions) { (x, y) ->
            val positionPair = Pair(x, y)
            val isShipPosition = shipPositionMap.containsKey(positionPair)
            val isShot = positionPair in shotsSet
            val shipLetter = shipPositionMap[positionPair]
            
            val isHit = isShot && isShipPosition
            val backgroundColor = when {
                isHit -> Color(0xFFF88379) // Light red for hits
                isShot -> Color(0xFFD3D3D3) // Gray for misses
                isEnemyBoard -> Color(0xFFE6E6FA) // Light lavender for enemy board
                else -> Color(0xFFE6E6FA) // Light lavender for player board
            }
            
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .border(0.5.dp, Color.Gray)
                    .background(backgroundColor)
                    .clickable(enabled = isEnemyBoard) { onCellClick(x, y) },
                contentAlignment = Alignment.Center
            ) {
                // Show ship letter only on player's board, not on enemy board
                if (!isEnemyBoard && shipLetter != null) {
                    Text(
                        text = shipLetter.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Show hit marker
                if (isHit) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                } else if (isShot && !isShipPosition) {
                    // Show miss marker
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    )
                }
            }
        }
    }
}
//EnemyBoard with AI - Jibin
@Composable
fun EnemyBoard(
    boardSize: Int,
    yourShots: List<Triple<Int, Int, Boolean>>,
    onCellClick: (Int, Int) -> Unit
) {
    // Create lookup sets for hits and misses
    val hits = yourShots.filter { it.third }.map { Pair(it.first, it.second) }.toSet()
    val misses = yourShots.filter { !it.third }.map { Pair(it.first, it.second) }.toSet()
    val allShots = yourShots.map { Pair(it.first, it.second) }.toSet()
    
    // Create board positions
    val positions = List(boardSize) { y -> 
        List(boardSize) { x -> 
            Pair(x, y)
        }
    }.flatten()
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(boardSize),
        modifier = Modifier
            .size(200.dp)
            .aspectRatio(1f)
            .border(1.dp, Color.Black)
    ) {
        items(positions) { (x, y) ->
            val positionPair = Pair(x, y)
            val isHit = positionPair in hits
            val isMiss = positionPair in misses
            val backgroundColor = when {
                isHit -> Color(0xFFF88379) // Light red for hits
                isMiss -> Color(0xFFD3D3D3) // Gray for misses
                else -> Color(0xFFE6E6FA) // Light lavender for unshot cells
            }
            
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .border(0.5.dp, Color.Gray)
                    .background(backgroundColor)
                    .clickable(enabled = positionPair !in allShots) { onCellClick(x, y) },
                contentAlignment = Alignment.Center
            ) {
                if (isHit) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                } else if (isMiss) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    )
                }
            }
        }
    }
}

@Composable
fun Modifier.rotate(degrees: Float) = this.then(graphicsLayer {
    rotationZ = degrees
}) 