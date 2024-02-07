package icu.takeneko.omms.controller.fabric.mixin;

import icu.takeneko.omms.controller.fabric.config.Config;
import icu.takeneko.omms.controller.fabric.config.SharedVariable;
import icu.takeneko.omms.controller.fabric.util.Util;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
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
        switch (Config.INSTANCE.getChatbridgeImplementation()) {
            case WS -> SharedVariable.getWebsocketChatClient().addToCache(Util.toPlayerConnectionStateBroadcast(
                    this.getPlayer().getName().getString(),
                    reason.getString().equals("Disconnected") ? reason : Texts.join(List.of(Text.of("Disconnected: "), reason), Text.empty())
            ));
            case UDP -> SharedVariable.getSender().addToQueue(Util.TARGET_CHAT,
                    Util.gson.toJson(Util.toPlayerConnectionStateBroadcast(
                            this.getPlayer().getName().getString(),
                            reason.getString().equals("Disconnected") ? reason : Texts.join(List.of(Text.of("Disconnected: "), reason), Text.empty())
                    )));
        }
    }
}
