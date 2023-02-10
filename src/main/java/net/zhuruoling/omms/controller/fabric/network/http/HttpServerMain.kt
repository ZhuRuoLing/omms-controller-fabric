package net.zhuruoling.omms.controller.fabric.network.http

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.ParseResults
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.zhuruoling.omms.controller.fabric.config.Config.getControllerName
import net.zhuruoling.omms.controller.fabric.config.SharedVariable
import net.zhuruoling.omms.controller.fabric.network.ControllerTypes
import net.zhuruoling.omms.controller.fabric.network.Status
import net.zhuruoling.omms.controller.fabric.util.CommandOutputData
import net.zhuruoling.omms.controller.fabric.util.OmmsCommandOutput
import net.zhuruoling.omms.controller.fabric.util.Util
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch
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
            realm = "Access to the client"
            validate {
                return@validate if ((it.name == getControllerName()) && (it.password == asSalted(getControllerName())))
                    UserIdPrincipal(it.name + it.password)
                else null
            }
        }
    }
}

fun asSalted(original: String) = original + "WTF IS IT".encodeBase64()


fun Application.configureRouting() {
    val logger = LoggerFactory.getLogger("HttpRouting")
    routing {
        get("/") {
            call.respondText(status = HttpStatusCode.OK) {
                "PONG"
            }
        }
        get("/log") {
            call.respondText (status = HttpStatusCode.OK){
                "${System.currentTimeMillis()}\n" + SharedVariable.logUpdateThread.logCache.joinToString(separator = "\n")
            }
        }
        authenticate("omms-simple-auth") {
            get("/status") {
                logger.info("Querying status.")
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
                logger.info("Got Command $command")
                val countDownLatch = CountDownLatch(1)
                var data: CommandOutputData? = null
                minecraftServer.execute {
                    val dispatcher: CommandDispatcher<ServerCommandSource> = minecraftServer.commandManager.dispatcher
                    val commandOutput = OmmsCommandOutput(minecraftServer)
                    val commandSource = commandOutput.createOmmsCommandSource()
                    val results: ParseResults<ServerCommandSource> =
                        dispatcher.parse(command, commandSource)
                    minecraftServer.commandManager.execute(results, command)
                    val commandResult = commandOutput.asString()
                    data = CommandOutputData(getControllerName(), command, commandResult)
                    countDownLatch.countDown()
                }
                runBlocking {
                    countDownLatch.await()
                    call.respondText(ContentType.Text.Plain, status = HttpStatusCode.OK) {
                        Util.gson.toJson(data!!)
                    }
                }
            }
        }
    }
}