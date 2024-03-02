package icu.takeneko.omms.controller.fabric.mixin;

import com.mojang.authlib.GameProfile;
import icu.takeneko.omms.controller.fabric.config.Config;
import icu.takeneko.omms.controller.fabric.config.SharedVariable;
import icu.takeneko.omms.controller.fabric.network.NetworkUtilKt;
import icu.takeneko.omms.controller.fabric.util.Util;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.UserCache;
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


@Mixin(value = net.minecraft.server.PlayerManager.class)
public abstract class PlayerJoinMixin {

    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    @Nullable
    public abstract ServerPlayerEntity getPlayer(String name);

    @Inject(method = "checkCanJoin", at = @At("HEAD"), cancellable = true)
    private void checkCanJoin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir) {
        if (address == null) {
            return;
        }
        if (!Config.INSTANCE.isEnableWhitelist()) return;
        String player = profile.getName();
        Text authResult = NetworkUtilKt.authPlayer(player);
        if (authResult != null){
            cir.setReturnValue(authResult);
        }
    }

    @Inject(method = "onPlayerConnect", at = @At("RETURN"))
    void sendPlayerJoinMsg(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        if (connection.getAddress() == null) {
            return;
        }
        GameProfile gameProfile = player.getGameProfile();
        UserCache userCache = this.server.getUserCache();
        Optional<GameProfile> optionalGameProfile = userCache.getByUuid(gameProfile.getId());
        String string = optionalGameProfile.map(GameProfile::getName).orElse(gameProfile.getName());
        MutableText mutableText;
        if (player.getGameProfile().getName().equalsIgnoreCase(string)) {
            mutableText = Text.translatable("multiplayer.player.joined", player.getDisplayName());
        } else {
            mutableText = Text.translatable("multiplayer.player.joined.renamed", player.getDisplayName(), string);
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
