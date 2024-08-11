package icu.takeneko.omms.controller.fabric.network.http.ws;

import net.minecraft.util.StringIdentifiable;

public enum PacketType implements StringIdentifiable {
    CONNECT, DISCONNECT,
    COMMAND, LOG;


    public static final Codec<PacketType> CODEC = StringIdentifiable.createCodec(PacketType::values);

    @Override
    public String asString() {
        return name();
    }
}