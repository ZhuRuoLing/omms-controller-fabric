package icu.takeneko.omms.controller.fabric.network.http.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class WSCommandPacket extends WSPacket<WSCommandPacket>{

    public static final Codec<WSCommandPacket> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("command").forGetter(o -> o.command)
    ).apply(ins, WSCommandPacket::new));

    private final String command;

    public WSCommandPacket(String command) {
        super(PacketTypes.COMMAND);
        this.command = command;
    }

    @Override
    public void handle(WSPacketHandler handler) {
        handler.onCommand(command);
    }
}
