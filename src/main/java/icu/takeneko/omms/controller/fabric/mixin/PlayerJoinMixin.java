package icu.takeneko.omms.controller.fabric.mixin;

import com.mojang.authlib.GameProfile;
import icu.takeneko.omms.controller.fabric.config.Config;
import icu.takeneko.omms.controller.fabric.config.SharedVariable;
import icu.takeneko.omms.controller.fabric.network.NetworkUtilKt;
import icu.takeneko.omms.controller.fabric.util.Util;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;
import java.util.Optional;


@Mixin(PlayerList.class)
public abstract class PlayerJoinMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "canPlayerLogin", at = @At("HEAD"), cancellable = true)
    private void checkCanJoin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Component> cir) {
        if (address == null) {
            return;
        }
        if (!Config.INSTANCE.isEnableWhitelist()) return;
        String player = profile.getName();
        Component authResult = NetworkUtilKt.authPlayer(player);
        if (authResult != null) {
            cir.setReturnValue(authResult);
        }
    }

    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    void sendPlayerJoinMsg(Connection connection, ServerPlayer player, CallbackInfo ci) {
        if (connection.getRemoteAddress() == null) {
            return;
        }
        GameProfile gameProfile = player.getGameProfile();
        GameProfileCache userCache = this.server.getProfileCache();
        Optional<GameProfile> optionalGameProfile = userCache.get(gameProfile.getId());
        String string = optionalGameProfile.map(GameProfile::getName).orElse(gameProfile.getName());
        MutableComponent mutableText;
        if (player.getGameProfile().getName().equalsIgnoreCase(string)) {
            mutableText = Component.translatable("multiplayer.player.joined", player.getDisplayName());
        } else {
            mutableText = Component.translatable("multiplayer.player.joined.renamed", player.getDisplayName(), string);
        }
        switch (Config.INSTANCE.getChatbridgeImplementation()) {
            case WS -> SharedVariable.getWebsocketChatClient().addToCache(Util.toPlayerConnectionStateBroadcast(
                player.getName().getString(),
                mutableText
            ));
            case UDP -> SharedVariable.getSender().addToQueue(Util.TARGET_CHAT,
                Util.gson.toJson(Util.toPlayerConnectionStateBroadcast(
                    player.getName().getString(),
                    mutableText
                )));
        }
    }
}
