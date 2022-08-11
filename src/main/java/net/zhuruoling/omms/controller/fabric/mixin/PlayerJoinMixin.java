package net.zhuruoling.omms.controller.fabric.mixin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.zhuruoling.omms.controller.fabric.config.Config;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;
import java.util.Arrays;

import static net.zhuruoling.omms.controller.fabric.util.Util.getPlayerInWhitelists;


@Mixin(value = net.minecraft.server.PlayerManager.class)
public abstract class PlayerJoinMixin {
    @Shadow @Final private static Logger LOGGER;

    @Shadow @Nullable public abstract ServerPlayerEntity getPlayer(String name);

    @Inject(method = "checkCanJoin", at = @At("HEAD"), cancellable = true)
    private void checkCanJoin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir){
        try {
            String player = profile.getName();
            String url = "http://%s:%d/whitelist/queryAll/%s".formatted(Config.getHttpQueryAddress(),Config.getHttpQueryPort(),player);
            String result = getPlayerInWhitelists(url);
            Gson gson = new GsonBuilder().serializeNulls().create();
            String[] whitelists = gson.fromJson(result,String[].class);
            if (Arrays.stream(whitelists).toList().contains(Config.getWhitelistName())){
                LOGGER.info("Successfully authed player %s".formatted(player));
                cir.setReturnValue(null);
            }
            else {
                LOGGER.info("Cannot auth player %s".formatted(player));
                cir.setReturnValue(Texts.toText(() -> "You are not in whitelist."));
            }
        }
        catch (Exception e){
            LOGGER.error("Failed to parse whitelist server return value.",e);
            var text = Texts.toText(() -> "Cannot auth with OMMS Central server.");
            cir.setReturnValue(text);
        }
    }
}
