package net.zhuruoling.omms.controller.fabric.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.server.network.ServerHandshakeNetworkHandler;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerHandshakeNetworkHandler.class)
public class PlayerPingHandshakeMixin {

    private final Logger logger = LogUtils.getLogger();
    @Inject(method = "onHandshake",at = @At("HEAD"))
    void inj(HandshakeC2SPacket packet, CallbackInfo ci){
        logger.info("Ping packet address:%s".formatted(packet.getAddress()));
        if (packet.getAddress().contains("FML")){
            logger.info("FML Client Ping Detected.");
        }

    }
}
