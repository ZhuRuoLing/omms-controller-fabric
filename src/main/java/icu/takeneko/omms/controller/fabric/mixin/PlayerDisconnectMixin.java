package icu.takeneko.omms.controller.fabric.mixin;

import icu.takeneko.omms.controller.fabric.config.Config;
import icu.takeneko.omms.controller.fabric.config.SharedVariable;
import icu.takeneko.omms.controller.fabric.util.Util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PlayerDisconnectMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    void onPlayerDisconnect(Component reason, CallbackInfo ci) {
        String s = reason.getString();
        switch (Config.INSTANCE.getChatbridgeImplementation()) {
            case WS -> SharedVariable.getWebsocketChatClient().addToCache(Util.toPlayerConnectionStateBroadcast(
                this.player.getName().getString(),
                reason.getString().equals("Disconnected") ? reason : ComponentUtils.formatList(List.of(Component.literal("Disconnected: "), reason), Component.empty())
            ));
            case UDP -> SharedVariable.getSender().addToQueue(Util.TARGET_CHAT,
                Util.gson.toJson(Util.toPlayerConnectionStateBroadcast(
                    this.player.getName().getString(),
                    reason.getString().equals("Disconnected") ? reason : ComponentUtils.formatList(List.of(Component.literal("Disconnected: "), reason), Component.empty())
                )));
        }
    }
}
