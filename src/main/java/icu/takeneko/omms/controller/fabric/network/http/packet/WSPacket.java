package icu.takeneko.omms.controller.fabric.network.http.packet;


import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import icu.takeneko.omms.controller.fabric.util.serization.DispatchedCodec;
import net.minecraft.resources.ResourceLocation;

public interface WSPacket {
    @SuppressWarnings("unchecked")
    Codec<WSPacket> CODEC = new DispatchedCodec<>(
        PacketRegistry.INSTANCE,
        it -> (MapCodec<WSPacket>) it.codec(),
        ins -> PacketRegistry.INSTANCE.reversedLookup().get((MapCodec<WSPacket>) ins.codec()),
        ResourceLocation.CODEC,
        "type"
    ).codec();

    void handle(WSPacketHandler handler);

    MapCodec<? extends WSPacket> codec();
}
