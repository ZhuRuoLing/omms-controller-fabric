package icu.takeneko.omms.controller.fabric.network.http.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public class WSLogPacket implements WSPacket {

    public static final MapCodec<WSLogPacket> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            Codec.STRING.listOf().fieldOf("lines").forGetter(o -> o.lines)
    ).apply(ins, WSLogPacket::new));

    private final List<String> lines;

    public WSLogPacket(List<String> lines) {
        this.lines = lines;
    }
    @Override
    public MapCodec<? extends WSPacket> codec() {
        return CODEC;
    }
    @Override
    public void handle(WSPacketHandler handler) {
    }
}
