package icu.takeneko.omms.controller.fabric.network.http.route

import icu.takeneko.omms.controller.fabric.config.Config.getControllerName
import icu.takeneko.omms.controller.fabric.network.http.CommandExecutionResult
import icu.takeneko.omms.controller.fabric.util.OmmsCommandOutput
import icu.takeneko.omms.controller.fabric.util.Util.gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

fun Route.runCommand(logger: Logger, serverSupplier: Supplier<MinecraftServer>) {
    post("/runCommand") {
        val minecraftServer = serverSupplier.get()
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