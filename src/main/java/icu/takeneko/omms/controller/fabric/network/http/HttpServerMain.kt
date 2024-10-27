package icu.takeneko.omms.controller.fabric.network.http

import com.mojang.logging.LogUtils
import com.mojang.serialization.JsonOps
import icu.takeneko.omms.controller.fabric.config.Config.getControllerName
import icu.takeneko.omms.controller.fabric.config.SharedVariable
import icu.takeneko.omms.controller.fabric.network.http.packet.WSLogPacket
import icu.takeneko.omms.controller.fabric.network.http.packet.WSPacket
import icu.takeneko.omms.controller.fabric.network.http.route.permissionRule
import icu.takeneko.omms.controller.fabric.network.http.route.runCommand
import icu.takeneko.omms.controller.fabric.network.http.route.status
import icu.takeneko.omms.controller.fabric.network.http.route.webSocketConsole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import net.minecraft.server.MinecraftServer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.CancellationException
import kotlin.concurrent.thread

lateinit var httpServer: ApplicationEngine
lateinit var httpServerThread: Thread
private lateinit var minecraftServer: MinecraftServer
private val logger = LogUtils.getLogger()
val connectionList: MutableSet<DefaultWebSocketSession> = Collections.synchronizedSet(LinkedHashSet())


fun serverMain(port: Int, server: MinecraftServer): Thread {
    minecraftServer = server
    val thread = thread(false, name = "Ktor Server Thread") {
        try {
            httpServer = embeddedServer(CIO, port = port, host = "0.0.0.0", module = Application::module)
            httpServer.start(wait = true)
        } catch (e: Exception) {
            if (e !is InterruptedException)
                e.printStackTrace()
        }
    }
    thread.start()
    return thread
}

fun Application.module() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    configureAuthentication()
    configureRouting()
}

fun Application.configureAuthentication() {
    authentication {
        basic(name = "omms-simple-auth") {
            realm = "Access to the client"
            validate {
                return@validate if ((it.name == getControllerName()) && (it.password == getControllerName().encodeBase64()))
                    UserIdPrincipal(it.name + it.password)
                else null
            }
        }
    }
}

fun Application.configureRouting() {
    val logger = LoggerFactory.getLogger("HttpRouting")
    routing {
        get("/") {
            call.respondText(status = HttpStatusCode.OK) {
                SharedVariable.sessionId
            }
        }
        authenticate("omms-simple-auth") {
            webSocketConsole(logger, ::getServer)
            status(logger, ::getServer)
            runCommand(logger, ::getServer)

            permissionRule()
        }
    }
}

fun sendToAllConnection(string: String) {
    runBlocking {
        try {
            connectionList.forEach {
                it.sendPacket(WSLogPacket(listOf(string)))
            }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                e.printStackTrace()
            }
        }
    }
}

private fun getServer(): MinecraftServer{
    return minecraftServer
}

suspend fun WebSocketSession.sendPacket(s: WSPacket) {
    send(WSPacket.CODEC.encodeStart(JsonOps.INSTANCE, s).getOrThrow(false) {
        logger.error("Failed to decode packet: $it")
    }.toString())
}