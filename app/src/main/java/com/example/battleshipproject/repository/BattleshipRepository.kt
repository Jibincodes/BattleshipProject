package com.example.battleshipproject.repository

import android.util.Log
import com.example.battleshipproject.models.*
import com.example.battleshipproject.network.ApiClient
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException
// in this repository, Gemini and Claude was used to assist us
class BattleshipRepository {
    private val apiService = ApiClient.battleshipService
    private val gson = Gson()
    private val TAG = "BattleshipRepository"
    
    //to test connection with the server - Jibin
    suspend fun pingServer(): Result<PingResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to ping server")
                val response = apiService.ping()
                handleResponse(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error pinging server", e)
                Result.failure(e)
            }
        }
    }
    
    // Join a game with the specified player name, game key, and ship positions as per server requirements - Sameh
    suspend fun joinGame(playerName: String, gameKey: String, ships: List<Ship>): Result<EnemyFireResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Basic validation as per server behaviour, given in PDF
                if (playerName.length < 3 || gameKey.length < 3) {
                    Log.e(TAG, "Invalid player name or game key")
                    return@withContext Result.failure(Exception("Player name and game key must be at least 3 characters"))
                }
                
                if (ships.size != 5) {
                    Log.e(TAG, "Invalid number of ships")
                    return@withContext Result.failure(Exception("Exactly 5 ships are required"))
                }
                
                Log.d(TAG, "Attempting to join game: $gameKey as player: $playerName")
                val request = JoinGameRequest(player = playerName, gamekey = gameKey, ships = ships)
                val response = apiService.joinGame(request)
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        return@withContext Result.success(responseBody)
                    } else {
                        return@withContext Result.failure(Exception("No response from server"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (errorBody?.contains("Error") == true) {
                        errorBody.substringAfter("Error\":\"").substringBefore("\"")
                    } else {
                        "Server error: ${response.code()}"
                    }
                    Log.e(TAG, "Error joining game: $errorMessage")
                    return@withContext Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error joining game", e)
                Result.failure(e)
            }
        }
    }
    
    // making fire at a specific position on opponent's grid - Jibin
    suspend fun fire(playerName: String, gameKey: String, x: Int, y: Int): Result<FireResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Validate coordinates as we dont allow negative values or values greater than 9
                if (x < 0 || x > 9 || y < 0 || y > 9) {
                    Log.e(TAG, "Invalid coordinates: ($x, $y)")
                    return@withContext Result.failure(Exception("Coordinates must be between 0 and 9"))
                }
                
                Log.d(TAG, "Firing at position ($x, $y) in game: $gameKey")
                val request = FireRequest(player = playerName, gamekey = gameKey, x = x, y = y)
                val response = apiService.fire(request)
                handleResponse(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error firing", e)
                Result.failure(e)
            }
        }
    }
    
    //checking for enermy move acoording to server behaviour, with request only player and gamekey -Jibin
    //reponse should have x,y,gameover
    suspend fun enemyFire(playerName: String, gameKey: String): Result<EnemyFireResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Checking for enemy move in game: $gameKey")
                val request = EnemyFireRequest(player = playerName, gamekey = gameKey)
                val response = apiService.enemyFire(request)
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        // First move case - only gameover field
                        if (responseBody.x == null && responseBody.y == null) {
                            Log.d(TAG, "First move response - no coordinates yet")
                            return@withContext Result.success(responseBody)
                        }
                        
                        // Normal move case - has coordinates
                        Log.d(TAG, "Enemy fire response: x=${responseBody.x}, y=${responseBody.y}, gameOver=${responseBody.gameOver}")
                        return@withContext Result.success(responseBody)
                    } else {
                        Log.e(TAG, "Enemy fire response body is null")
                        return@withContext Result.failure(Exception("No response from server"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Enemy fire request failed: ${response.code()}, Error: $errorBody")
                    return@withContext Result.failure(Exception("Server error: ${response.code()}"))
                }
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Timeout while waiting for enemy move")
                return@withContext Result.failure(e)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking enemy move: ${e.message}")
                return@withContext Result.failure(e)
            }
        }
    }
    
    //generating random ship placement for game with AI help - Sameh
    fun generateRandomShips(boardSize: Int = 10): List<Ship> {
        val ships = mutableListOf<Ship>()
        val occupiedPositions = mutableSetOf<Pair<Int, Int>>()
        
        val shipTypes = listOf(
            ShipType.CARRIER,      // 5 spaces
            ShipType.BATTLESHIP,   // 4 spaces
            ShipType.DESTROYER,    // 3 spaces
            ShipType.SUBMARINE,    // 3 spaces
            ShipType.PATROL_BOAT   // 2 spaces
        )
        
        for (shipType in shipTypes) {
            var ship: Ship? = null
            var attempts = 0
            
            while (ship == null && attempts < 100) {
                val orientation = if ((0..1).random() == 0) Orientation.HORIZONTAL else Orientation.VERTICAL
                
                val maxX = if (orientation == Orientation.HORIZONTAL) boardSize - shipType.size else boardSize - 1
                val maxY = if (orientation == Orientation.VERTICAL) boardSize - shipType.size else boardSize - 1
                
                if (maxX < 0 || maxY < 0) {
                    attempts++
                    continue
                }
                
                val startX = (0..maxX).random()
                val startY = (0..maxY).random()
                
                val tempShip = Ship(type = shipType, x = startX, y = startY, orientation = orientation)
                val positions = tempShip.getAllPositions()
                
                var positionsValid = true
                for (position in positions) {
                    if (Pair(position.x, position.y) in occupiedPositions) {
                        positionsValid = false
                        break
                    }
                }
                
                if (positionsValid) {
                    ship = tempShip
                    for (position in positions) {
                        occupiedPositions.add(Pair(position.x, position.y))
                    }
                    ships.add(ship)
                    break
                }
                
                attempts++
            }
            
            if (ship == null) {
                return generateRandomShips(boardSize)
            }
        }
        
        return ships
    }
    
    //to handle responses from server - Jibin
    private fun <T> handleResponse(response: Response<T>): Result<T> {
        return if (response.isSuccessful) {
            response.body()?.let {
                Log.d(TAG, "Request successful: ${response.code()}")
                Result.success(it)
            } ?: run {
                Log.e(TAG, "Response body is null")
                Result.failure(Exception("Response body is null"))
            }
        } else {
            val errorBody = response.errorBody()?.string()
            val errorMessage = if (!errorBody.isNullOrEmpty()) {
                try {
                    val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                    errorResponse.error
                } catch (e: Exception) {
                    "HTTP Error: ${response.code()}"
                }
            } else {
                "HTTP Error: ${response.code()}"
            }
            Log.e(TAG, "Request failed: $errorMessage")
            Result.failure(Exception(errorMessage))
        }
    }
} 