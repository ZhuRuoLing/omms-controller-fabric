package icu.takeneko.omms.controller.fabric.network.http.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

public class WSAckPacket implements WSPacket {
    public static final MapCodec<WSAckPacket> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            Codec.INT.optionalFieldOf("clientVersion").forGetter(o ->
                    o.clientVersion <= 0 ? Optional.empty() : Optional.of(o.clientVersion)
            ),
            Action.CODEC.fieldOf("action").forGetter(o -> o.action)
    ).apply(ins, WSAckPacket::new));

    private final int clientVersion;
    private final Action action;

    public WSAckPacket(int clientVersion,Action action){
        this.clientVersion = clientVersion;
        this.action = action;
    }

    public WSAckPacket(Optional<Integer> clientVersion, Action action) {
        this.clientVersion = clientVersion.orElse(-1);
        this.action = action;
    }

    @Override
    public void handle(WSPacketHandler handler) {
        switch (action) {
            case CONNECT -> handler.onConnect(clientVersion);
            case DISCONNECT -> handler.onDisconnect();
        }
    }

    public enum Action implements StringRepresentable {
        CONNECT, DISCONNECT;

        public static final Codec<Action> CODEC = StringRepresentable.fromEnum(Action::values);


        @Override
        public String getSerializedName() {
            return name();
        }
    }

    @Override
    public MapCodec<? extends WSPacket> codec() {
        return CODEC;
    }
}
