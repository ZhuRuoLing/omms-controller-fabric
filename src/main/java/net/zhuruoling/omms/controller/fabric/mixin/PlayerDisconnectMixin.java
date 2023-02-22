package net.zhuruoling.omms.controller.fabric.mixin;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.zhuruoling.omms.controller.fabric.config.SharedVariable;
import net.zhuruoling.omms.controller.fabric.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class PlayerDisconnectMixin {
    @Shadow
    public abstract ServerPlayerEntity getPlayer();

    @Inject(method = "onDisconnected", at = @At("HEAD"))
    void onPlayerDisconnect(Text reason, CallbackInfo ci) {
        String s = reason.getString();
        SharedVariable.getSender().addToQueue(Util.TARGET_CHAT,
                Util.gson.toJson(Util.toPlayerConnectionStateBroadcast(
                        this.getPlayer().getName().getString(),
                        reason.getString().equals("Disconnected") ? reason : Texts.join(List.of(Text.of("Disconnected: "), reason), Text.empty())
                )));
    }
}
