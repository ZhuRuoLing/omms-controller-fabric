package icu.takeneko.omms.controller.fabric.network.http.packet

import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.logging.LogUtils
import icu.takeneko.omms.controller.fabric.network.http.sendPacket
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import net.minecraft.server.MinecraftServer

private val logger = LogUtils.getLogger()

class WSPacketHandlerImpl(
    private val minecraftServer: MinecraftServer,
    private val session: WebSocketSession
) : WSPacketHandler {
    var shouldDisconnect = false

    override fun onConnect(version: Int) {
        runBlocking {
            session.sendPacket(WSAckPacket(1, WSAckPacket.Action.CONNECT))
        }
    }

    override fun onDisconnect() {
        runBlocking {
            session.sendPacket(WSAckPacket(1, WSAckPacket.Action.DISCONNECT))
        }
        shouldDisconnect = true
    }

    override fun onCommand(line: String) {
        minecraftServer.execute {
            try {
                logger.debug("Command $line from console ${Integer.toHexString(this.hashCode())}")
                minecraftServer.commandManager.dispatcher.execute(
                    line,
                    minecraftServer.commandSource
                )
            } catch (e: Exception) {
                if (e is CommandSyntaxException) {
                    logger.error(e.message)
                } else throw e
            }
        }
    }
}
