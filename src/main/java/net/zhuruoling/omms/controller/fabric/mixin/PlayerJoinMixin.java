package net.zhuruoling.omms.controller.fabric.mixin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Pair;
import net.minecraft.util.UserCache;
import net.zhuruoling.omms.controller.fabric.config.Config;
import net.zhuruoling.omms.controller.fabric.config.SharedVariable;
import net.zhuruoling.omms.controller.fabric.util.Util;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.ConnectException;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static net.zhuruoling.omms.controller.fabric.util.Util.invokeHttpGetRequest;


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
        try {
            String player = profile.getName();
            String url = "http://%s:%d/whitelist/queryAll/%s".formatted(Config.INSTANCE.getHttpQueryAddress(), Config.INSTANCE.getHttpQueryPort(), player);
            var pair = invokeHttpGetRequest(url);
            if (pair.getLeft() != 200) {
                var text = Texts.toText(() -> "Cannot auth with OMMS Central server.");
                cir.setReturnValue(text);
                return;
            }
            String result = pair.getRight();
            if (result != null) {
                if (result.isEmpty()) {
                    var text = Texts.toText(() -> "Cannot auth with OMMS Central server.");
                    cir.setReturnValue(text);
                    return;
                }
                Gson gson = new GsonBuilder().serializeNulls().create();
                String[] whitelists = gson.fromJson(result, String[].class);
                if (Objects.isNull(whitelists)) {
                    var text = Texts.toText(() -> "Cannot auth with OMMS Central server.");
                    cir.setReturnValue(text);
                    return;
                }
                if (Arrays.stream(whitelists).toList().contains(Config.INSTANCE.getWhitelistName())) {
                    LOGGER.info("Successfully authed player %s".formatted(player));

                } else {
                    LOGGER.info("Cannot auth player %s".formatted(player));
                    cir.setReturnValue(Texts.toText(() -> "You are not in whitelist."));
                }
            } else {
                cir.setReturnValue(Texts.toText(() -> "You are not in whitelist."));
            }
        } catch (Exception e) {
            if (e instanceof ConnectException) {
                LOGGER.error("Cannot Connect to OMMS Central server, reason: %s".formatted(e.toString()));
            } else {
                e.printStackTrace();
                LOGGER.error("Failed to auth player.");
            }
            var text = Texts.toText(() -> "Cannot auth with OMMS Central server.");
            cir.setReturnValue(text);
        }
    }

    @Inject(method = "onPlayerConnect", at = @At("RETURN"))
    void sendPlayerJoinMsg(ClientConnection connection, ServerPlayerEntity player, int latency, CallbackInfo ci) {
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
