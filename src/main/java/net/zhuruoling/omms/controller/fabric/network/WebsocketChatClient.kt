package net.zhuruoling.omms.controller.fabric.network

//import io.ktor.client.engine.cio.*
import com.mojang.logging.LogUtils
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.zhuruoling.omms.controller.fabric.config.Config
import net.zhuruoling.omms.controller.fabric.config.Config.getControllerName
import net.zhuruoling.omms.controller.fabric.util.Util
import org.slf4j.Logger
import java.util.concurrent.CancellationException

class WebsocketChatClient(private val minecraftServer: MinecraftServer) : Thread("WebsocketChatBridge") {
    private val logger: Logger = LogUtils.getLogger()
    private val cache = mutableListOf<Broadcast>()
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 1000
        }
        engine {
            threadsCount = 4
            pipelining = true
        }
        install(Auth) {
            basic {
                credentials {
                    BasicAuthCredentials(
                        username = getControllerName(),
                        password = getControllerName().hashCode().toString(16)
                    )
                }
                realm = "chatbridge"
            }
        }
    }

    fun addToCache(broadcast: Broadcast) {
        synchronized(cache) {
            cache.add(broadcast)
        }
    }

    private suspend fun broadcastFromWS(session: DefaultClientWebSocketSession) {
        try {
            for (message in session.incoming) {
                message as? Frame.Text ?: continue
                val content = message.readText()
                if (content.startsWith("PONG")) continue
                minecraftServer.execute {
                    val broadcast = Util.gson.fromJson(content, Broadcast::class.java)
                    if (broadcast.getPlayer().startsWith("\ufff3\ufff4")) {
                        minecraftServer.playerManager
                            .broadcast(Util.fromBroadcastToQQ(broadcast), false)
                    }
                    if (broadcast.getServer() != getControllerName()) {
                        minecraftServer.playerManager
                            .broadcast(Util.fromBroadcast(broadcast), false)
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Chatbridge disconnected because ${e.message}.")
            throw e
        }
    }

    private suspend fun sendBroadcastFromCache(session: DefaultClientWebSocketSession) {
        while (true) {
            synchronized(cache) {
                if (cache.isNotEmpty()) {
                    for (broadcast in cache) {
                        runBlocking {
                            session.send(Util.gson.toJson(broadcast).toString())
                        }
                    }
                    cache.clear()
                }
            }
            sleep(50)
        }
    }

    override fun run() {
        try {
            logger.info("Websocket Chatbridge connecting.")
            runBlocking {
                while (true) {
                    try {
                        launchWS()
                        minecraftServer.playerManager.broadcast(
                            Text.literal("Chatbridge disconnected from server, client will try to reconnect after 3 seconds.")
                                .copy().setStyle(Style.EMPTY.withColor(Formatting.YELLOW)),
                            false
                        )
                        //logger.warn("Websocket Chatbridge disconnected from server. Client will try to reconnect after 3 seconds.")
                    } catch (e: Exception) {
                        if (e is InterruptedException) {
                            logger.info("exit")
                            break
                        }
                        logger.warn("Cannot connect to Chatbridge server, reason: $e. Client will try to reconnect after 3 seconds.")
                    }
                    sleep(5000)
                }
            }
        } catch (_: InterruptedException) {

        }
        logger.info("Exiting websocket Chatbridge client.")
    }

    private suspend fun launchWS() {
        client.webSocket(
            method = HttpMethod.Get,
            host = Config.getHttpQueryAddress(),
            port = Config.getHttpQueryPort(),
            path = "chatbridge"
        ) {
            //logger.info("Chatbridge Connected.")
            minecraftServer.playerManager.broadcast(
                Text.literal("Chatbridge Connected.")
                    .copy().setStyle(Style.EMPTY.withColor(Formatting.AQUA)),
                false
            )
            val outRoutine = launch { broadcastFromWS(this@webSocket) }
            val inRoutine = launch { sendBroadcastFromCache(this@webSocket) }
            inRoutine.invokeOnCompletion {
                if (outRoutine.isActive)
                    outRoutine.cancel()
            }
            outRoutine.invokeOnCompletion {
                if (inRoutine.isActive)
                    inRoutine.cancel(CancellationException("Chatbridge disconnected."))
            }

            inRoutine.join()
            outRoutine.cancelAndJoin()

        }

    }
}