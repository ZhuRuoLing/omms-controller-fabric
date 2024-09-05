package icu.takeneko.omms.controller.fabric.network.http.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class WSAckPacket extends WSPacket<WSAckPacket> {
    public static final Codec<WSAckPacket> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("clientVersion").forGetter(o ->
                    o.clientVersion <= 0 ? Optional.empty() : Optional.of(o.clientVersion)
            ),
            Action.CODEC.fieldOf("action").forGetter(o -> o.action)
    ).apply(ins, WSAckPacket::new));

    private final int clientVersion;
    private final Action action;

    public WSAckPacket(int clientVersion,Action action){
        super(PacketTypes.ACK);
        this.clientVersion = clientVersion;
        this.action = action;
    }

    public WSAckPacket(Optional<Integer> clientVersion, Action action) {
        super(PacketTypes.ACK);
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

    public enum Action implements StringIdentifiable {
        CONNECT, DISCONNECT;

        public static final Codec<Action> CODEC = StringIdentifiable.createCodec(Action::values);

        @Override
        @NotNull
        public String asString() {
            return name();
        }
    }
}
