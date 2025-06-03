package com.example.battleshipproject.repository

import android.util.Log
import com.example.battleshipproject.models.EnemyFireRequest
import com.example.battleshipproject.models.EnemyFireResponse
import com.example.battleshipproject.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// again we have used AI tools like Gemini and Claude to debug
class GameRepository {
    private val TAG = "GameRepository"
    private val apiService = ApiClient.battleshipService

    // Check game state and logging for necessary info - Jibin
    suspend fun checkGameState(playerName: String, gameKey: String): Result<EnemyFireResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Checking game state with playerName=$playerName, gameKey=$gameKey")
                val request = EnemyFireRequest(player = playerName, gamekey = gameKey)
                val response = apiService.enemyFire(request)
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    Log.d(TAG, "Game state response: $responseBody")
                    Result.success(responseBody ?: EnemyFireResponse(null, null, false))
                } else {
                    val errorMessage = "Error ${response.code()}: ${response.errorBody()?.string()}"
                    Log.e(TAG, errorMessage)
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking game state: ${e.message}")
                Result.failure(e)
            }
        }
    }
} 