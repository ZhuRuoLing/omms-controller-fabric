package icu.takeneko.omms.central.controller.console.ws.packet

import icu.takeneko.omms.controller.fabric.network.http.packet.PacketType
import icu.takeneko.omms.controller.fabric.network.http.packet.WSPacket
import io.ktor.util.*
import net.minecraft.util.Identifier

object PacketRegistry {
    private val registry = mutableMapOf<Identifier, PacketType<*>>()
    private val reversedRegistry = mutableMapOf<PacketType<*>,Identifier>()

    fun get(key: Identifier):PacketType<*>?{
        return registry[key]
    }

    fun getKey(value:PacketType<*>):Identifier? {
        return reversedRegistry[value]
    }

    fun register(key: Identifier, value: PacketType<*>) {
        if (this.get(key) != null) {
            throw IllegalArgumentException("Duplicate packetType: $key")
        }
        this.registry[key] = value
        this.reversedRegistry[value] = key
    }

    fun encodePacket(packet: WSPacket<*>): String {
        val pt = packet.packetType
        val packetContent = packet.encodeSelf().encodeBase64()
        val registryKey = getKey(pt) ?: throw IllegalArgumentException("")
        return "${registryKey.toString().encodeBase64()}::$packetContent"
    }

    fun decodePacket(content: String): WSPacket<*> {
        val (key, line) = content.split("::")
        val packetType = get(Identifier(key.decodeBase64String()))
            ?: throw IllegalArgumentException("Unknown packet type: ${key.decodeBase64String()}")
        val packetContent = line.decodeBase64String()
        return packetType.decode(packetContent)
    }

}