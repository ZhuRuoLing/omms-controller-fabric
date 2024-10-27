package icu.takeneko.omms.controller.fabric.network.http.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class WSConnectPacket implements WSPacket {
    public static final MapCodec<WSConnectPacket> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.INT.fieldOf("version").forGetter(o -> o.version)
    ).apply(ins, WSConnectPacket::new));

    private final int version;

    protected WSConnectPacket(int version) {
        this.version = version;
    }

    @Override
    public void handle(WSPacketHandler handler) {
        handler.onConnect(version);
    }

    @Override
    public MapCodec<? extends WSPacket> codec() {
        return CODEC;
    }
}
