package net.zhuruoling.omms.controller.fabric.mixin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.zhuruoling.omms.controller.fabric.config.ConstantStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Objects;

import static net.zhuruoling.omms.controller.fabric.util.Util.invokeHttpGetRequest;


@Mixin(value = net.minecraft.server.PlayerManager.class)
public abstract class PlayerJoinMixin {
    Logger LOGGER = LogManager.getLogger(PlayerJoinMixin.class);

    @Shadow
    @Nullable
    public abstract ServerPlayerEntity getPlayer(String name);

    @Inject(method = "checkCanJoin", at = @At("HEAD"), cancellable = true)
    private void checkCanJoin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir) {
        if (!ConstantStorage.isEnableWhitelist()) return;
        try {
            String player = profile.getName();
            String url = String.format("http://%s:%d/whitelist/queryAll/%s",ConstantStorage.getHttpQueryAddress(), ConstantStorage.getHttpQueryPort(), player);
            String result = invokeHttpGetRequest(url);
            if (result.isEmpty()) {
                var text = Texts.toText(() -> "Cannot auth with OMMS Central server.");
                cir.setReturnValue(text);
            }
            Gson gson = new GsonBuilder().serializeNulls().create();
            String[] whitelists = gson.fromJson(result, String[].class);
            if (Objects.isNull(whitelists)) {
                var text = Texts.toText(() -> "Cannot auth with OMMS Central server.");
                cir.setReturnValue(text);
            }
            if (Arrays.asList(whitelists).contains(ConstantStorage.getWhitelistName())) {
                LOGGER.info(String.format("Successfully authed player %s",player));
                return;
            } else {
                LOGGER.info(String.format("Cannot auth player %s",player));
                cir.setReturnValue(Texts.toText(() -> "You are not in whitelist."));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse whitelist server return value.");
            var text = Texts.toText(() -> "Cannot auth with OMMS Central server.");
            cir.setReturnValue(text);
        }
    }
}
