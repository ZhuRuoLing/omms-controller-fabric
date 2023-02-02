package net.zhuruoling.omms.controller.fabric.network.http

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.minecraft.server.MinecraftServer
import net.zhuruoling.omms.controller.fabric.config.Config
import net.zhuruoling.omms.controller.fabric.config.Config.getControllerName
import net.zhuruoling.omms.controller.fabric.network.ControllerTypes
import net.zhuruoling.omms.controller.fabric.network.Status
import net.zhuruoling.omms.controller.fabric.util.Util
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.thread

lateinit var httpServer: ApplicationEngine
lateinit var httpServerThread: Thread
private lateinit var minecraftServer: MinecraftServer

fun serverMain(port: Int, server: MinecraftServer): Thread {
    minecraftServer = server
    val thread = thread(false, name = "Ktor Server Thread") {
        httpServer = embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        httpServer.start(wait = true)
    }
    thread.start()
    return thread
}

fun Application.module() {
    configureAuthentication()
    configureRouting()
}

fun Application.configureAuthentication() {
    authentication {
        basic(name = "omms-simple-auth") {
            validate {
                return@validate if ((getControllerName() == it.name) && (asSalted(getControllerName()) == it.password))
                    UserIdPrincipal(it.name + it.password)
                else null
            }
        }
    }
}

fun asSalted(original: String): String {
    TODO("广告位招商")
}

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText(status = HttpStatusCode.OK) {
                "PONG"
            }
        }
        authenticate ("omms-simple-auth"){
            get("/status") {
                val status = Status(
                    getControllerName(),
                    ControllerTypes.FABRIC,
                    minecraftServer.currentPlayerCount,
                    minecraftServer.maxPlayerCount,
                    listOf(*minecraftServer.playerNames)
                )
                call.respondText {
                    Util.gson.toJson(status)
                }
            }
            post("/runCommand") {
                val command = call.receiveText()

            }
        }
    }
}