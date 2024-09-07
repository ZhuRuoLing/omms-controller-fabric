package icu.takeneko.omms.controller.fabric.network.http.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public class WSCompletionResultPacket extends WSPacket<WSCompletionResultPacket>{
    public static final Codec<WSCompletionResultPacket> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("requestId").forGetter(o -> o.requestId),
            Codec.STRING.listOf().fieldOf("results").forGetter(o -> o.results)
    ).apply(ins, WSCompletionResultPacket::new));

    private final String requestId;
    private final List<String> results;

    public WSCompletionResultPacket(String requestId, List<String> results) {
        super(PacketTypes.COMPLETION_RESULT);
        this.requestId = requestId;
        this.results = results;
    }
    @Override
    public void handle(WSPacketHandler handler) {

    }
}
