package com.example.battleshipproject.network

import com.example.battleshipproject.models.EnemyFireRequest
import com.example.battleshipproject.models.EnemyFireResponse
import com.example.battleshipproject.models.FireRequest
import com.example.battleshipproject.models.FireResponse
import com.example.battleshipproject.models.JoinGameRequest
import com.example.battleshipproject.models.PingResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
//Gemini was used to quickly do these endpoints
interface BattleshipApiService {
    // defining all the required endpoints as per PDF- Sameh
    // testing connection with server, added additionally - Jibin
    @GET("ping")
    suspend fun ping(): Response<PingResponse>

    //Joining the  game with the specified player name, game key, and ship positions
    @POST("game/join")
    suspend fun joinGame(@Body request: JoinGameRequest): Response<EnemyFireResponse>

    //To make a move (fire) at a specific position on opponent's grid
    @POST("game/fire")
    suspend fun fire(@Body request: FireRequest): Response<FireResponse>

    //To get the enemies move
    @POST("game/enemyFire")
    suspend fun enemyFire(@Body request: EnemyFireRequest): Response<EnemyFireResponse>
} 