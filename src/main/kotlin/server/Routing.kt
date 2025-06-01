package com.leonardo.server

import com.leonardo.game.WordWarGame
import com.leonardo.models.CreateRoom
import com.leonardo.models.Event
import com.leonardo.models.JoinRoom
import com.leonardo.models.PlayerReady
import com.leonardo.models.SelectLetter
import io.ktor.websocket.WebSocketSession
import kotlinx.serialization.json.Json

suspend fun routingEvent(message: String, socket: WebSocketSession): Pair<String, String>? {
    val type = message.substringBefore("#")
    val body = message.substringAfter("#")
    when (type) {
        Event.create_room.name -> {
            val data = Json.decodeFromString<CreateRoom>(body)
            WordWarGame.createRoom(data, socket)
            return Pair(data.id, data.playerId)
        }
        Event.join_room.name -> {
            val data = Json.decodeFromString<JoinRoom>(body)
            WordWarGame.joinRoom(data, socket)
            return Pair(data.id, data.playerId)
        }
        Event.start_game.name -> {
            val data = Json.decodeFromString<PlayerReady>(body)
            WordWarGame.startGame(data, socket)
        }
        Event.select_letter.name -> {
            val data = Json.decodeFromString<SelectLetter>(body)
            WordWarGame.selectLetter(data, socket)
        }
    }
    return null
}