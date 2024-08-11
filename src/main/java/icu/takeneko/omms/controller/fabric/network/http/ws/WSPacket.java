package icu.takeneko.omms.controller.fabric.network.http.ws;

public interface WSPacket {
    void handle(WSPacketHandler handler);

    static <T extends WSPacket> WSPacket cast(T packet){
        return (WSPacket) packet;
    }
}
