package icu.takeneko.omms.controller.fabric.network.http.packet;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import icu.takeneko.omms.controller.fabric.OmmsControllerFabric;
import net.minecraft.util.Identifier;

public class PacketTypes {
    public static final PacketType<WSConnectPacket> CONNECT = register(
            OmmsControllerFabric.of("connect"),
            WSConnectPacket.class,
            WSConnectPacket.CODEC
    );

    public static final PacketType<WSAckPacket> ACK = register(
            OmmsControllerFabric.of("ack"),
            WSAckPacket.class,
            WSAckPacket.CODEC
    );
    public static final PacketType<WSCommandPacket> COMMAND = register(
            OmmsControllerFabric.of("command"),
            WSCommandPacket.class,
            WSCommandPacket.CODEC
    );
    public static final PacketType<WSLogPacket> LOG = register(
            OmmsControllerFabric.of("log"),
            WSLogPacket.class,
            WSLogPacket.CODEC
    );

    public static final PacketType<WSDisconnectPacket> DISCONNECT = register(
            OmmsControllerFabric.of("disconnect"),
            WSDisconnectPacket.class,
            WSDisconnectPacket.CODEC
    );

    public static final PacketType<WSCompletionRequestPacket> COMPLETION_REQUEST = register(
        OmmsControllerFabric.of("completion_request"),
        WSCompletionRequestPacket.class,
        WSCompletionRequestPacket.CODEC
    );

    public static final PacketType<WSCompletionResultPacket> COMPLETION_RESULT = register(
        OmmsControllerFabric.of("completion_result"),
        WSCompletionResultPacket.class,
        WSCompletionResultPacket.CODEC
    );


    private static <T extends WSPacket<T>> PacketType<T> register(Identifier id, Class<? extends T> clazz, Codec<T> codec) {
        PacketType<T> pc = new PacketType<>(
                clazz,
                s -> codec.decode(JsonOps.INSTANCE, JsonParser.parseString(s))
                        .getOrThrow(false, s1 -> {
                        })
                        .getFirst(),
                it -> codec.encodeStart(JsonOps.INSTANCE, it)
                        .getOrThrow(false, s1 -> {
                        })
                        .toString()

        );
        PacketRegistry.INSTANCE.register(id, pc);
        return pc;
    }
}
