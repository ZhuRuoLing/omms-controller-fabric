package icu.takeneko.omms.controller.fabric.network.http.packet;


public abstract class WSPacket<T extends WSPacket<T>> {
    private final PacketType<T> packetType;

    protected WSPacket(PacketType<T> packetType) {
        this.packetType = packetType;
    }

    public String encodeSelf() {
        return packetType.encode((T) this);
    }

    abstract public void handle(WSPacketHandler handler);

    public static <T extends WSPacket<T>> WSPacket<? extends T> cast(T packet) {
        return packet;
    }

    public PacketType<T> getPacketType() {
        return packetType;
    }
}
