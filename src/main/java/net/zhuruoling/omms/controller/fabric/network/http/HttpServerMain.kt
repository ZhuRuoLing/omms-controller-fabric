package net.zhuruoling.omms.controller.fabric.network.http

import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import net.minecraft.server.MinecraftServer
import net.zhuruoling.omms.controller.fabric.config.Config.getControllerName
import net.zhuruoling.omms.controller.fabric.config.SharedVariable
import net.zhuruoling.omms.controller.fabric.network.ControllerTypes
import net.zhuruoling.omms.controller.fabric.network.Status
import net.zhuruoling.omms.controller.fabric.network.http.PermissionModificationData.Type.*
import net.zhuruoling.omms.controller.fabric.permission.PermissionRuleManager
import net.zhuruoling.omms.controller.fabric.util.OmmsCommandOutput
import net.zhuruoling.omms.controller.fabric.util.Util.gson
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread

lateinit var httpServer: ApplicationEngine
lateinit var httpServerThread: Thread
private lateinit var minecraftServer: MinecraftServer
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
                return@validate if ((it.name == getControllerName()) && (it.password == asSalted(getControllerName())))
                    UserIdPrincipal(it.name + it.password)
                else null
            }
        }
    }
}

fun asSalted(original: String) = original.encodeBase64() + "WTF IS IT".encodeBase64()

fun sendToAllConnection(string: String) {
    runBlocking {
        try {
            connectionList.forEach {
                it.send(string)
            }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                e.printStackTrace()
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
            webSocket("/") {
                logger.debug("New WebSocket Console ${Integer.toHexString(this.hashCode())} attached.")
                connectionList += this
                synchronized(SharedVariable.logCache) {
                    runBlocking {
                        send(SharedVariable.logCache.joinToString("\n"))
                    }
                }
                try {
                    for (frame in incoming) {
                        frame as? Frame.Text ?: continue
                        val received = frame.readText()
                        minecraftServer.execute {
                            try {
                                logger.debug("Command $received from console ${Integer.toHexString(this.hashCode())}")
                                minecraftServer.commandManager.dispatcher.execute(
                                    received,
                                    minecraftServer.commandSource
                                )
                            } catch (e: Exception) {
                                if (e is CommandSyntaxException) {
                                    logger.error(e.message)
                                } else throw e
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    logger.debug("Removing WebSocket Console ${Integer.toHexString(this.hashCode())}")
                    connectionList -= this
                }
            }
            get("/status") {
                logger.debug("Querying status.")
                val status = Status(
                    getControllerName(),
                    ControllerTypes.FABRIC,
                    minecraftServer.currentPlayerCount,
                    minecraftServer.maxPlayerCount,
                    listOf(*minecraftServer.playerNames)
                )
                call.respondText {
                    gson.toJson(status)
                }
            }
            route("/permissionRule") {
                get("list") {
                    val clazz = call.request.queryParameters["className"] ?: return@get call.respondText(
                        status = HttpStatusCode.OK
                    ) {
                        gson.toJson(PermissionRuleManager.INSTANCE.permissionRuleMap)
                    }
                    val rule = PermissionRuleManager.INSTANCE.permissionRuleMap[clazz] ?: return@get
                    call.respondText(status = HttpStatusCode.BadRequest) {
                        gson.toJson(PermissionModificationResult(false, "Class not exist.", null))
                    }
                    return@get call.respondText {
                        gson.toJson(PermissionModificationResult(true, "", rule))
                    }
                }
                post("status") {
                    val clazz = call.receiveText()
                    val status =
                        PermissionRuleManager.INSTANCE.permissionRuleMap[clazz]?.status ?: return@post call.respondText(
                            status = HttpStatusCode.BadRequest
                        ) { gson.toJson(PermissionModificationResult(false, "Class not exist.", false)) }
                    return@post call.respondText {
                        gson.toJson(PermissionModificationResult(true, "", status))
                    }
                }
                post("modify") {
                    val content = call.receiveText()
                    val dt = gson.fromJson(content, PermissionModificationData::class.java)
                    try{
                        when (dt.type) {
                            ENABLE -> {
                                PermissionRuleManager.INSTANCE.enableCheckFor(dt.className)
                            }

                            REMOVE -> {
                                PermissionRuleManager.INSTANCE.disableCheckFor(dt.className)
                            }

                            ADD_RULE -> {
                                PermissionRuleManager.INSTANCE.addRule(dt.className, dt.rule)
                            }

                            REMOVE_RULE -> {
                                PermissionRuleManager.INSTANCE.removeRule(dt.className, dt.removeAt)
                            }

                            else -> {
                                return@post call.respondText(
                                    status = HttpStatusCode.BadRequest
                                ) {
                                    gson.toJson(PermissionModificationResult(false, "Type not specified.", false))
                                }
                            }
                        }
                        return@post call.respondText(
                            status = HttpStatusCode.OK
                        ) {
                            gson.toJson(PermissionModificationResult(true, "", null))
                        }
                    }catch (e:Exception){
                        return@post call.respondText(
                            status = HttpStatusCode.InternalServerError
                        ) {
                            gson.toJson(PermissionModificationResult(false, "Server Internal Error", e.stackTraceToString()))
                        }
                    }
                }
            }
            post("/runCommand") {
                val command = call.receiveText()
                logger.debug("Command Input: $command")
                val future = CompletableFuture<CommandExecutionResult>()
                minecraftServer.execute {
                    val commandOutput = OmmsCommandOutput(minecraftServer)
                    val commandSource = commandOutput.createOmmsCommandSource()
                    future.complete(
                        try {
                            minecraftServer.commandManager.dispatcher.execute(command, commandSource)
                            val commandResult = commandOutput.asString()
                            CommandExecutionResult(
                                getControllerName(),
                                command,
                                commandResult.split("\n"),
                                true,
                                "",
                                ""
                            )
                        } catch (e: Exception) {
                            val commandResult = commandOutput.asString()
                            CommandExecutionResult(
                                getControllerName(),
                                command,
                                commandResult.split("\n"),
                                false,
                                e.message,
                                e.stackTraceToString()
                            )
                        }
                    )
                }
                runBlocking {
                    call.respondText(ContentType.Text.Plain, status = HttpStatusCode.OK) {
                        gson.toJson(future.get()!!)
                    }
                }
            }
        }
    }
}

fun toMultiLineErrorMessage(s: String): List<String> {
    val list = s.split("error").toMutableList()
    val res = mutableListOf(list[0] + "error")
    list.removeAt(0)
    res += list.joinToString(separator = "error")
    return res
}