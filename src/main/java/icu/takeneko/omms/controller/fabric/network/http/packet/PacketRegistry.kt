package icu.takeneko.omms.controller.fabric.network.http.packet

import com.mojang.serialization.MapCodec
import icu.takeneko.omms.controller.fabric.OmmsControllerFabric
import icu.takeneko.omms.controller.fabric.util.Lookup
import net.minecraft.resources.ResourceLocation

object PacketRegistry :Lookup<ResourceLocation, MapCodec<WSPacket>>{
    private val registry = mutableMapOf<ResourceLocation, MapCodec<WSPacket>>()
    private val reversedRegistry = mutableMapOf<MapCodec<WSPacket>, ResourceLocation>()

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

    override fun get(key: ResourceLocation): MapCodec<WSPacket>? {
        return registry[key]
    }

    fun getKey(value: MapCodec<WSPacket>): ResourceLocation? {
        return reversedRegistry[value]
    }

    fun register(key: ResourceLocation, value: MapCodec<WSPacket>) {
        if (get(key) != null) {
            throw IllegalArgumentException("Duplicate packetType: $key")
        }
        registry[key] = value
        reversedRegistry[value] = key
    }

    fun reversedLookup(): Lookup<MapCodec<WSPacket>, ResourceLocation> {
        return Lookup {
            return@Lookup getKey(it)
        }
    }

}