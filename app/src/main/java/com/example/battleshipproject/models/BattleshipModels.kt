package com.example.battleshipproject.models

import com.google.gson.annotations.SerializedName

// The possible ship orientations - Sameh
enum class Orientation {
    @SerializedName("horizontal") HORIZONTAL,
    @SerializedName("vertical") VERTICAL
}

// ship types - Sameh
enum class ShipType(val size: Int) {
    @SerializedName("Carrier") CARRIER(5),
    @SerializedName("Battleship") BATTLESHIP(4),
    @SerializedName("Destroyer") DESTROYER(3),
    @SerializedName("Submarine") SUBMARINE(3),
    @SerializedName("PatrolBoat") PATROL_BOAT(2)
}

// the possible game states - Sameh
data class Position(
    val x: Int,
    val y: Int
)

// Ship model with position and orientation - Jibin
data class Ship(
    @SerializedName("ship") val type: ShipType,
    val x: Int,
    val y: Int,
    val orientation: Orientation
) {
    // generating all positions occupied by the ship - Jibin
    fun getAllPositions(): List<Position> {
        val positions = mutableListOf<Position>()
        for (i in 0 until type.size) {
            val pos = when (orientation) {
                Orientation.HORIZONTAL -> Position(x + i, y)
                Orientation.VERTICAL -> Position(x, y + i)
            }
            positions.add(pos)
        }
        return positions
    }
}

// Joining the game request as per server requirements - Sameh
data class JoinGameRequest(
    val player: String,
    val gamekey: String,
    val ships: List<Ship>
)

// Fire request as per server requirements - Sameh
data class FireRequest(
    val player: String,
    val gamekey: String,
    val x: Int,
    val y: Int
)

// Enemy fire request as per server requirements - Sameh
data class EnemyFireRequest(
    val player: String,
    val gamekey: String
)

// Ping response, just to test out server is running - Jibin
data class PingResponse(
    val ping: Boolean
)

// Fire response - Jibin
data class FireResponse(
    val hit: Boolean,
    val shipsSunk: List<String>? = null,
    @SerializedName("shipType") val shipType: String? = null,
    @SerializedName("gameover") val gameOver: Boolean = false,
    @SerializedName("Error") val error: String? = null
)

// Enemy  fire response - Jibin
data class EnemyFireResponse(
    val x: Int?,
    val y: Int?,
    @SerializedName("gameover") val gameOver: Boolean
)

// in case of error - Jibin
data class ErrorResponse(
    @SerializedName("Error") val error: String
) 