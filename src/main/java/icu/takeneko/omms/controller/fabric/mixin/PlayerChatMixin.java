package icu.takeneko.omms.controller.fabric.mixin;


import icu.takeneko.omms.controller.fabric.util.Util;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.level.ServerPlayer;
import icu.takeneko.omms.controller.fabric.config.Config;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class PlayerChatMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(at = @At("RETURN"), method = "handleChat")
    private void handleMessage(ServerboundChatPacket chatPacket, CallbackInfo ci) {
        if (!Config.INSTANCE.isEnableChatBridge()) return;
        String raw = chatPacket.message();
        if (!raw.startsWith("/")) {
            Util.sendChatBroadcast(raw, this.player.getName().getString());
        }
    }
}
