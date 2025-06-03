package com.example.battleshipproject.viewmodel

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.battleshipproject.models.EnemyFireResponse
import com.example.battleshipproject.models.FireResponse
import com.example.battleshipproject.models.Position
import com.example.battleshipproject.models.Ship
import com.example.battleshipproject.repository.BattleshipRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "BattleshipViewModel"

// to represent the state of the game - jibin
enum class GameState {
    Setup,      // Setting up ships and joining a game
    InProgress, // Game is ongoing
    GameOver    // Game is complete
}

// for viewmodel logic - jibin
class BattleshipViewModel : ViewModel() {
    private val repository = BattleshipRepository()
    private val handler = Handler(Looper.getMainLooper())
    
    // Game state
    private val _gameState = MutableLiveData<GameState>(GameState.Setup)
    val gameState: LiveData<GameState> = _gameState
    
    // Loading indicator
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Error messages
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage
    
    // Player and game information
    private val _playerName = MutableLiveData<String>("")
    val playerName: LiveData<String> = _playerName
    
    private val _gameKey = MutableLiveData<String>("")
    val gameKey: LiveData<String> = _gameKey
    
    // Ship placements
    private val _ships = MutableLiveData<List<Ship>>(emptyList())
    val ships: LiveData<List<Ship>> = _ships
    
    // Turn indicator
    private val _isYourTurn = MutableLiveData<Boolean>(false)
    val isYourTurn: LiveData<Boolean> = _isYourTurn
    
    // Shots fired and received
    private val _yourShots = MutableLiveData<List<Triple<Int, Int, Boolean>>>(emptyList())
    val yourShots: LiveData<List<Triple<Int, Int, Boolean>>> = _yourShots
    
    private val _enemyShots = MutableLiveData<List<Position>>(emptyList())
    val enemyShots: LiveData<List<Position>> = _enemyShots
    
    // Game over flag
    private val _isGameOver = MutableLiveData<Boolean>(false)
    val isGameOver: LiveData<Boolean> = _isGameOver
    
    // Game won flag - we werent able to figure out how to use this properly for the winner
    private val _isGameWon = MutableLiveData<Boolean>(false)
    val isGameWon: LiveData<Boolean> = _isGameWon
    
    // Ships sunk
    private val _sunkenShips = MutableLiveData<List<String>>(emptyList())
    val sunkenShips: LiveData<List<String>> = _sunkenShips
    
    // Ship sunk announcement
    private val _shipSunkAnnouncement = MutableLiveData<String?>(null)
    val shipSunkAnnouncement: LiveData<String?> = _shipSunkAnnouncement
    
    // Polling job for enemy moves
    private var pollingJob: Job? = null
    
    // Flag to track if we're the first player
    private var isFirstPlayer = false
    
    // Validation flags
    private val _isGameKeyValid = MutableLiveData<Boolean>(false)
    val isGameKeyValid: LiveData<Boolean> = _isGameKeyValid
    
    private val _isPlayerNameValid = MutableLiveData<Boolean>(false)
    val isPlayerNameValid: LiveData<Boolean> = _isPlayerNameValid
    
    private val _areShipsValid = MutableLiveData<Boolean>(false)
    val areShipsValid: LiveData<Boolean> = _areShipsValid
    
    // additional server pining to see if it is running - jibin
    fun pingServer() {
        _isLoading.value = true
        _errorMessage.value = null
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "Pinging server")
                val result = repository.pingServer()
                if (result.isSuccess) {
                    Log.d(TAG, "Server is running")
                    _errorMessage.value = null
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(TAG, "Server ping failed: $error")
                    _errorMessage.value = "Server is not responding: $error"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error pinging server", e)
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // to set player name and validate- jibin
    fun setPlayerName(name: String) {
        _playerName.value = name
        _isPlayerNameValid.value = name.trim().length >= 3
    }
    
    // to set game key and validate as per server behaviour - jibin
    fun setGameKey(key: String) {
        _gameKey.value = key
        _isGameKeyValid.value = key.trim().length >= 3
    }
    
    // random ship placement
    fun generateRandomShips(boardSize: Int = 10) {
        Log.d(TAG, "Generating random ships")
        _ships.value = repository.generateRandomShips(boardSize)
        _areShipsValid.value = true
    }
    
    // to join game - both sameh and jibin
    fun joinGame() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = repository.joinGame(
                _playerName.value,
                _gameKey.value,
                _ships.value ?: emptyList()
            )
            _isLoading.value = false

            if (result.isSuccess) {
                val response = result.getOrNull()
                if (response?.x != null && response.y != null) {
                    // We are second player
                    isFirstPlayer = false
                    _isYourTurn.value = false  // Second player doesn't get first turn - ran many issues here
                    _gameState.value = GameState.InProgress
                    startPollingForEnemyMoves()  // Start polling for first player's move
                } else {
                    // We are first player
                    isFirstPlayer = true
                    _isYourTurn.value = true  // First player gets first turn
                    _gameState.value = GameState.InProgress
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                _errorMessage.value = error
                Log.e(TAG, "Error joining game: $error")
            }
        }
    }
    
    // to fire at a position - jibin
    fun fireAtPosition(position: Position) {
        if (_isLoading.value == true || _isYourTurn.value != true) {
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _isYourTurn.value = false  // Set to false immediately to prevent double-firing

            val result = repository.fire(
                _playerName.value,
                _gameKey.value,
                position.x,
                position.y
            )

            if (result.isSuccess) {
                val fireResponse = result.getOrNull()
                if (fireResponse != null) {
                    // Check if there was an error response
                    if (fireResponse.error != null) {
                        _errorMessage.value = fireResponse.error
                        if (fireResponse.error.contains("Not your turn", ignoreCase = true)) {
                            // If it's not our turn, start polling for enemy moves
                            startPollingForEnemyMoves()
                        } else {
                            // For other errors, give back the turn
                            _isYourTurn.value = true
                        }
                        _isLoading.value = false
                        return@launch
                    }

                    // Check for newly sunk ships
                    val previousSunkShips = _sunkenShips.value ?: emptyList()
                    val newSunkShips = fireResponse.shipsSunk?.filter { it !in previousSunkShips } ?: emptyList()
                    
                    // Update sunken ships
                    _sunkenShips.value = fireResponse.shipsSunk ?: emptyList()
                    
                    // Announce hits and sunk ships
                    if (fireResponse.hit) {
                        if (fireResponse.shipType != null) {
                            _shipSunkAnnouncement.value = "You hit the enemy's ${fireResponse.shipType}!"
                        } else {
                            _shipSunkAnnouncement.value = "You hit an enemy ship!"
                        }
                    }
                    
                    if (newSunkShips.isNotEmpty()) {
                        _shipSunkAnnouncement.value = "You sunk the enemy's ${newSunkShips.joinToString(", ")}!"
                    }
                    
                    // Record the shot
                    val shotsList = _yourShots.value?.toMutableList() ?: mutableListOf()
                    shotsList.add(Triple(position.x, position.y, fireResponse.hit))
                    _yourShots.value = shotsList
                    
                    // Always give turn to opponent after shooting
                    startPollingForEnemyMoves()
                    
                    // Check for game over
                    if (fireResponse.gameOver) {
                        _isGameWon.value = true
                        _shipSunkAnnouncement.value = "Congratulations! You won the game!"
                        endGame(true)
                    }
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                _errorMessage.value = error
                Log.e(TAG, "Error firing: $error")
                _isYourTurn.value = true // Give back the turn on error
            }
            _isLoading.value = false
        }
    }
    
    // to end the game - did not had time to make an game won value - jibin
    private fun endGame(endGame: Boolean) {
        if (endGame) {
            Log.d(TAG, "Game over")
            _isGameOver.value = true
            _gameState.value = GameState.GameOver
            stopPollingForEnemyMoves()
        }
    }
    
    // to start polling for enemy moves - jibin
    private fun startPollingForEnemyMoves() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                if (!_isYourTurn.value!! && !_isLoading.value!!) {
                    checkForEnemyMove()
                }
                delay(5000)
            }
        }
    }
    
    // to stop polling for enemy moves - jibin
    private fun stopPollingForEnemyMoves() {
        pollingJob?.cancel()
        pollingJob = null
    }
    
    // to check for enemy move - jibin
    private fun checkForEnemyMove() {
        viewModelScope.launch {
            val result = repository.enemyFire(
                _playerName.value,
                _gameKey.value
            )
            if (result.isSuccess) {
                val response = result.getOrNull()
                if (response != null) {
                    if (response.gameOver) {
                        _isGameWon.value = false
                        _shipSunkAnnouncement.value = "Game Over - You lost!"
                        endGame(true)
                    } else if (response.x != null && response.y != null) {
                        // Record enemy's shot
                        val enemyShotsList = _enemyShots.value?.toMutableList() ?: mutableListOf()
                        enemyShotsList.add(Position(response.x, response.y))
                        _enemyShots.value = enemyShotsList
                        
                        // Check if any of our ships were hit
                        val hitPosition = Position(response.x, response.y)
                        val hitShip = _ships.value?.find { ship ->
                            ship.getAllPositions().any { it.x == hitPosition.x && it.y == hitPosition.y }
                        }
                        
                        if (hitShip != null) {
                            // Check if the ship is sunk by counting all its positions that have been hit
                            val shipPositions = hitShip.getAllPositions()
                            val hitPositions = _enemyShots.value?.filter { shot ->
                                shipPositions.any { it.x == shot.x && it.y == shot.y }
                            } ?: emptyList()
                            
                            if (hitPositions.size == hitShip.type.size) {
                                _shipSunkAnnouncement.value = "The enemy sunk your ${hitShip.type}!"
                            } else {
                                _shipSunkAnnouncement.value = "The enemy hit your ${hitShip.type}!"
                            }
                        }
                        
                        // It's now our turn
                        _isYourTurn.value = true
                        stopPollingForEnemyMoves()
                    }
                }
            }
        }
    }
    
    // to reset the game - sameh
    fun resetGame() {
        Log.d(TAG, "Resetting game")
        stopPollingForEnemyMoves()
        
        _gameState.value = GameState.Setup
        _isLoading.value = false
        _errorMessage.value = null
        _isYourTurn.value = false
        _isGameOver.value = false
        _isGameWon.value = false
        isFirstPlayer = false
        
        _yourShots.value = emptyList()
        _enemyShots.value = emptyList()
        _sunkenShips.value = emptyList()
        _shipSunkAnnouncement.value = null
        
        // Keep player name and game key for convenience
        // But reset ships to force the player to generate new ones
        _ships.value = emptyList()
        _areShipsValid.value = false
    }
    
    // to clear the polling job - jibin
    override fun onCleared() {
        super.onCleared()
        stopPollingForEnemyMoves()
    }
} 