package icu.takeneko.omms.controller.fabric.network.http.route

import icu.takeneko.omms.controller.fabric.config.Config.getControllerName
import icu.takeneko.omms.controller.fabric.network.ControllerTypes
import icu.takeneko.omms.controller.fabric.network.Status
import icu.takeneko.omms.controller.fabric.util.Util.gson
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger
import java.util.function.Supplier

fun Route.status(logger: Logger, serverSupplier: Supplier<MinecraftServer>) {
    get("/status") {
        val minecraftServer = serverSupplier.get()
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
}