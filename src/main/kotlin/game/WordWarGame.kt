package com.leonardo.game

import com.leonardo.models.CreateRoom
import com.leonardo.models.JoinRoom
import com.leonardo.models.Player
import com.leonardo.models.PlayerReady
import com.leonardo.models.Room
import com.leonardo.models.SelectLetter
import com.leonardo.models.ServerMessage
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.send
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

object WordWarGame {

    private val rooms = mutableMapOf<String, Room>()
    private val mutex = Mutex()

    suspend fun createRoom(data: CreateRoom, socket: WebSocketSession) {
        mutex.withLock {
            if (playerAlreadyExist(data.id, data.playerId)) {
                sendErrorMessage("Player ${data.id} already exists.", socket)
                return
            }

            val player = createPlayer(data.playerId, data.playerName, socket)

            if (roomAlreadyExist(data.id)) {
                sendErrorMessage("Room ${data.id} already exists.", socket)
                return
            }

            val playerState = PlayerState(player.name, player.id)

            val state = MutableStateFlow(
                WordWarGameState(
                    mutableListOf(playerState),
                    mutableListOf(),
                    mutableMapOf(),
                    GameStatus.WAITING
                )
            )

            val room = Room(data.id, mutableMapOf(player.id to player), state)

            rooms[data.id] = room

            room.state
                .onEach {
                    val message = ServerMessage(
                        "game_state_changed",
                        Json.encodeToString(WordWarGameState.serializer(), it)
                    )
                    broadcastToRoom(data.id, Json.encodeToString(message))
                }
                .launchIn(room.scope)
        }
    }

    suspend fun joinRoom(data: JoinRoom, socket: WebSocketSession) {
        mutex.withLock {
            if (playerAlreadyExist(data.id, data.playerId)) {
                sendErrorMessage("Player ${data.id} already exists.", socket)
                return
            }

            val player = createPlayer(data.playerId, data.playerName, socket)

            val room = rooms[data.id]

            if (room == null) {
                sendErrorMessage("Room ${data.id} does not exist.", socket)
                return
            }

            room.players.put(player.id, player)

            room.state.update { gameState ->
                gameState.copy(connectedPlayers = room.players.map { PlayerState(it.value.name, it.value.id) })
            }
        }
    }

    suspend fun startGame(data: PlayerReady, socket: WebSocketSession) {
        val room = rooms[data.roomId]

        if (room == null) {
            sendErrorMessage("Room ${data.roomId} does not exist.", socket)
            return
        }

        if (room.players.size <= 1) {
            sendErrorMessage("Room ${data.roomId} should await other player.", socket)
            return
        }

        val letters = generateWeightedLetters()

        var seconds = 1

        while (seconds < 4) {
            broadcastToRoom(room.id, "The match will start in $seconds")
            delay(1000)
            seconds++
        }

        room.state.update { gameState ->
            gameState.copy(letters = letters, gameStatus = GameStatus.PLAYING)
        }

        room.scope.launch {
            delay(120000)

            room.state.update { gameState ->
                gameState.copy(gameStatus = GameStatus.FINISHED)
            }
        }
    }

    suspend fun selectLetter(data: SelectLetter, socket: WebSocketSession) {
        val room = rooms[data.roomId]
        if (room == null) {
            sendErrorMessage("Room ${data.roomId} does not exist.", socket)
            return
        }
        if (!room.players.containsKey(data.playerId)) {
            sendErrorMessage("Player ${data.playerId} does not exist.", socket)
            return
        }
        if (room.state.value.selectedLetters.containsKey(data.letterId) || room.state.value.gameStatus != GameStatus.PLAYING ) {
            return
        }
        room.state.update { gameState ->
            val newSelectLetters = gameState.selectedLetters.toMutableMap()
            newSelectLetters.put(data.letterId, data.playerId)
            gameState.copy(selectedLetters = newSelectLetters)
        }
    }

    suspend fun disposeRoom(id: String) {
        mutex.withLock {
            broadcastToRoom(id, "Room $id will be disposed.")
            rooms.remove(id)
        }
    }

    suspend fun disconnectPlayer(roomId: String, playerId: String) {
        mutex.withLock {
            val room = rooms[roomId] ?: return
            room.players.remove(playerId)
            room.state.update { gameState ->
                gameState.copy(
                    connectedPlayers = room.players.map { PlayerState(it.value.name, it.value.id) }
                )
            }

            if (room.players.isEmpty()) {
                broadcastToRoom(roomId, "Room $roomId will be disposed.")
                rooms.remove(roomId)
            }
        }
    }

    /*
        PRIVATE FUNCTIONS
    */

    val weightedAlphabet = listOf(
        "A", "A", "A", "A", "A", "A", "A",
        "E", "E", "E", "E", "E", "E", "E", "E",
        "I", "I", "I", "I", "I",
        "O", "O", "O", "O",
        "U", "U", "U",
        "S", "S", "S", "S",
        "R", "R", "R",
        "T", "T", "T",
        "N", "N", "N",
        "M", "M",
        "L", "L",
        "D", "D",
        "C", "C",
        "P",
        "B",
        "V",
        "F",
        "G",
        "H",
        "J",
        "Q",
        "X",
        "Z",
        "Ç"
    )

    fun generateWeightedLetters(count: Int = 20): List<Letter> {
        return List(count) {
            Letter(it.toString(), weightedAlphabet.random())
        }
    }


    private fun createPlayer(id: String, name: String, socket: WebSocketSession): Player {
        val player = Player(id, name, socket)
        return player
    }

    private fun playerAlreadyExist(roomId: String, playerId: String): Boolean {
        rooms[roomId]?.let {
            if (it.players.containsKey(playerId)) {
                return true
            }
        }
        return false
    }

    private fun roomAlreadyExist(roomId: String) = rooms.containsKey(roomId)

    private suspend fun sendErrorMessage(message: String, socket: WebSocketSession) =  socket.send(message)

    private suspend fun broadcastToRoom(roomId: String, message: String) {
        rooms[roomId]?.let {
            it.players.forEach { player ->
                player.value.session.send(Frame.Text(message))
            }
        }
    }
}