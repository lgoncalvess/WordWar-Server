package com.leonardo.models

import kotlinx.serialization.Serializable

enum class Event {
    create_room, join_room, start_game, select_letter
}

@Serializable
data class CreateRoom(
    val id: String,
    val playerId: String,
    val playerName: String
)

@Serializable
data class JoinRoom(
    val id: String,
    val playerId: String,
    val playerName: String
)

@Serializable
data class PlayerReady(
    val roomId: String,
)

@Serializable
data class SelectLetter(
    val letterId: String,
    val roomId: String,
    val playerId: String
)

@Serializable
data class ServerMessage<T>(
    val event: String,
    val payload: T
)