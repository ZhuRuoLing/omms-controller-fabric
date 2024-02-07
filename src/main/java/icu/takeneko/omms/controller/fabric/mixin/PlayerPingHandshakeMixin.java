package icu.takeneko.omms.controller.fabric.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.server.network.ServerHandshakeNetworkHandler;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerHandshakeNetworkHandler.class)
public class PlayerPingHandshakeMixin {
    @Shadow
    public ClientConnection connection;
    private final Logger logger = LogUtils.getLogger();
    @Inject(method = "onHandshake", at = @At("HEAD"))
    void noMoreFml(HandshakeC2SPacket packet, CallbackInfo ci) {
        if (packet.getAddress().contains("FML")) {
            connection.disconnect(Text.of(""));
        }
    }
}
