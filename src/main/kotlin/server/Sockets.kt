package com.leonardo.server

import com.leonardo.game.WordWarGame
import com.leonardo.models.Player
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

val playerSockets = ConcurrentHashMap<Player, WebSocketSession>()

fun Application.configureSockets() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/ws") {

            var (roomId, playerId) = Pair("", "")

            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val data = routingEvent(frame.readText(), this)
                        if (data != null) {
                            roomId = data.first
                            playerId = data.second
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error: ${e.message}")
                this.send("Some error occurred while connecting to the server")
            } finally {
                WordWarGame.disposeRoom(roomId)
                close(CloseReason(CloseReason.Codes.NORMAL, "Room $roomId disposed"))
            }
        }
    }
}
