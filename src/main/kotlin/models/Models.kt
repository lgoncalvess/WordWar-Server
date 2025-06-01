package com.leonardo.models

import com.leonardo.game.WordWarGameState
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow

data class Room(
    val id: String,
    val players: MutableMap<String, Player>,
    val state: MutableStateFlow<WordWarGameState>,
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
)

data class Player(
    val id: String,
    val name: String,
    val session: WebSocketSession
)