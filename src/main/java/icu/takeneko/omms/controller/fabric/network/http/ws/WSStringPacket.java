package icu.takeneko.omms.controller.fabric.network.http.ws;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public class WSStringPacket implements WSPacket {
    public static final Codec<WSStringPacket> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            PacketType.CODEC.fieldOf("packetType").forGetter(o -> o.packetType),
            Codec.either(
                    Codec.STRING.listOf(),
                    Codec.STRING
            ).fieldOf("content").forGetter(o ->
                    o.packetType == PacketType.COMMAND
                            ? Either.right(o.line)
                            : Either.left(o.lines)
            )
    ).apply(ins, WSStringPacket::new));

    public WSStringPacket(PacketType packetType, List<String> lines) {
        this.packetType = packetType;
        this.lines = lines;
    }

    private final PacketType packetType;
    private String line;
    private List<String> lines;

    public WSStringPacket(PacketType packetType, Either<List<String>, String> listStringEither) {
        this.packetType = packetType;
        listStringEither.ifLeft(it -> lines = it).ifRight(it -> line = it);
    }


    @Override
    public void handle(WSPacketHandler handler) {
        if (packetType == PacketType.COMMAND) {
            handler.onCommand(line);
        }
    }
}
