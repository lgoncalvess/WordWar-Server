package com.leonardo.game

import kotlinx.serialization.Serializable

enum class GameStatus {
    WAITING,
    PLAYING,
    FINISHED,
}

@Serializable
data class WordWarGameState(
    val connectedPlayers: List<PlayerState>,
    val letters: List<Letter>,
    val selectedLetters: Map<String, String>,
    val gameStatus: GameStatus
)

@Serializable
data class PlayerState(
    val name: String,
    val id: String,
)

@Serializable
data class Letter(
    val id: String,
    val value: String,
)
