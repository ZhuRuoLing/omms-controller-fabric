package net.zhuruoling.omms.controller.fabric.mixin;


import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.zhuruoling.omms.controller.fabric.config.ConstantStorage;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

import static net.zhuruoling.omms.controller.fabric.util.Util.getPlayerInWhitelists;


@Mixin(PlayerManager.class)
public class PlayerReadyMixin {
    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final private MinecraftServer server;

    @Inject(method = "onPlayerConnect",at = @At("RETURN"))
    private void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci){
        String url = "http://%s:%d/whitelist/queryAll/%s".formatted(ConstantStorage.getHttpQueryAddress(), ConstantStorage.getHttpQueryPort(),player.getName().asString());
        String result = getPlayerInWhitelists(url);
        NbtCompound compound = new NbtCompound();
        compound.putString("servers", result);
        Objects.requireNonNull(player.getServer()).getCommandManager().execute(player.getCommandSource(),"/menu %s".formatted(compound.asString()));
        server.getPlayerManager().broadcast(Text.of("<%s> o/".formatted(player.getName().asString())), MessageType.CHAT, player.getUuid());
    }
}
