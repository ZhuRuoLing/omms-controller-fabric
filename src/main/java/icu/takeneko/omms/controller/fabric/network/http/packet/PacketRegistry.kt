package icu.takeneko.omms.controller.fabric.network.http.packet

import com.mojang.serialization.MapCodec
import icu.takeneko.omms.controller.fabric.OmmsControllerFabric
import icu.takeneko.omms.controller.fabric.util.Lookup
import io.ktor.util.*
import net.minecraft.util.Identifier

object PacketRegistry :Lookup<Identifier, MapCodec<WSPacket>>{
    private val registry = mutableMapOf<Identifier, MapCodec<WSPacket>>()
    private val reversedRegistry = mutableMapOf<MapCodec<WSPacket>, Identifier>()

    init {
        register(
            OmmsControllerFabric.of("connect"),
            WSConnectPacket.CODEC as MapCodec<WSPacket>
        )
        register(
            OmmsControllerFabric.of("ack"),
            WSAckPacket.CODEC as MapCodec<WSPacket>
        )
        register(
            OmmsControllerFabric.of("command"),
            WSCommandPacket.CODEC as MapCodec<WSPacket>
        )
        register(
            OmmsControllerFabric.of("log"),
            WSLogPacket.CODEC as MapCodec<WSPacket>
        )
        register(
            OmmsControllerFabric.of("disconnect"),
            WSDisconnectPacket.CODEC as MapCodec<WSPacket>
        )
        register(
            OmmsControllerFabric.of("completion_request"),
            WSCompletionRequestPacket.CODEC as MapCodec<WSPacket>
        )
        register(
            OmmsControllerFabric.of("completion_result"),
            WSCompletionResultPacket.CODEC as MapCodec<WSPacket>
        )
    }

    override fun get(key: Identifier): MapCodec<WSPacket>? {
        return registry[key]
    }

    fun getKey(value: MapCodec<WSPacket>): Identifier? {
        return reversedRegistry[value]
    }

    fun register(key: Identifier, value: MapCodec<WSPacket>) {
        if (get(key) != null) {
            throw IllegalArgumentException("Duplicate packetType: $key")
        }
        registry[key] = value
        reversedRegistry[value] = key
    }

    fun reversedLookup(): Lookup<MapCodec<WSPacket>, Identifier> {
        return Lookup {
            return@Lookup getKey(it)
        }
    }

}