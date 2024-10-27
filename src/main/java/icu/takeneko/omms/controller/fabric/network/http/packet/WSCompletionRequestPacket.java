package icu.takeneko.omms.controller.fabric.network.http.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class WSCompletionRequestPacket implements WSPacket{
    public static final MapCodec<WSCompletionRequestPacket> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            Codec.STRING.fieldOf("requestId").forGetter(o -> o.requestId),
            Codec.STRING.fieldOf("input").forGetter(o -> o.input),
            Codec.INT.fieldOf("cursorPosition").forGetter(o -> o.cursorPosition)
    ).apply(ins, WSCompletionRequestPacket::new));

    private final String requestId;
    private final String input;
    private final int cursorPosition;

    public WSCompletionRequestPacket(String requestId, String input, int cursorPosition) {
        this.requestId = requestId;
        this.input = input;
        this.cursorPosition = cursorPosition;
    }

    @Override
    public void handle(WSPacketHandler handler) {
        handler.onCompletionRequest(requestId, input, cursorPosition);
    }
    @Override
    public MapCodec<? extends WSPacket> codec() {
        return CODEC;
    }
}
