package icu.takeneko.omms.controller.fabric.network.http.route

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import icu.takeneko.omms.controller.fabric.config.SharedVariable
import icu.takeneko.omms.controller.fabric.network.http.connectionList
import icu.takeneko.omms.controller.fabric.network.http.packet.WSLogPacket
import icu.takeneko.omms.controller.fabric.network.http.packet.WSPacket
import icu.takeneko.omms.controller.fabric.network.http.packet.WSPacketHandlerImpl
import icu.takeneko.omms.controller.fabric.network.http.sendPacket
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger
import java.util.function.Supplier

fun Route.webSocketConsole(logger: Logger, serverSupplier: Supplier<MinecraftServer>) {
    webSocket("/") {
        val minecraftServer = serverSupplier.get()
        logger.info("New WebSocket Console ${Integer.toHexString(this.hashCode())} attached.")
        connectionList += this
        val handler = WSPacketHandlerImpl(minecraftServer, this)
        synchronized(SharedVariable.logCache) {
            runBlocking {
                sendPacket(WSLogPacket(SharedVariable.logCache))
            }
        }
        try {
            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val text = frame.readText()
                WSPacket.CODEC.decode(JsonOps.INSTANCE, JsonParser.parseString(text))
                    .getOrThrow(false) {
                        logger.error("Failed to decode packet: $it")
                    }.first.handle(handler)
                if (handler.shouldDisconnect) break
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            logger.debug("Removing WebSocket Console ${Integer.toHexString(this.hashCode())}")
            connectionList -= this
        }
    }
}