package net.zhuruoling.omms.controller.fabric.mixin;


import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.filter.TextStream;
import net.minecraft.server.network.ServerPlayerEntity;
import net.zhuruoling.omms.controller.fabric.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//Great thanks to Gugle and his SuperEvent
@Mixin(value = net.minecraft.server.network.ServerPlayNetworkHandler.class)
public class PlayerChatMixin {
    @Shadow public ServerPlayerEntity player;

    @Inject(at = @At("RETURN"), method = "onChatMessage")
    private void handleMessage(ChatMessageC2SPacket packet, CallbackInfo ci){
        String raw = packet.getChatMessage();
        if (!raw.startsWith("/")){
            Util.sendBroadcast(Util.TARGET_CHAT, raw, this.player.getDisplayName().asString());
        }
    }
}
