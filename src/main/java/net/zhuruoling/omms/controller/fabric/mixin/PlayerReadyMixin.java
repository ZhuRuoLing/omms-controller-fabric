package net.zhuruoling.omms.controller.fabric.mixin;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.zhuruoling.omms.controller.fabric.config.ConstantStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.UUID;

import static net.zhuruoling.omms.controller.fabric.util.Util.invokeHttpGetRequest;

import com.mojang.brigadier.CommandDispatcher;


@Mixin(PlayerManager.class)
public class PlayerReadyMixin {

    @Shadow @Final private MinecraftServer server;

    @Inject(method = "onPlayerConnect",at = @At("RETURN"))
    private void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci){
        if (!ConstantStorage.isEnableWhitelist())return;
        String playerName = player.getName().copy().getString();
        String url = String.format("http://%s:%d/whitelist/queryAll/%s",ConstantStorage.getHttpQueryAddress(), ConstantStorage.getHttpQueryPort(),playerName);
        String result = invokeHttpGetRequest(url);
        CompoundTag compound = new CompoundTag();
        compound.putString("servers", result);
        var dispatcher = Objects.requireNonNull(player.getServer()).getCommandManager().getDispatcher();

        Objects.requireNonNull(player.getServer()).getCommandManager().execute(player.getCommandSource(), String.format("menu %s",playerName));
        server.getPlayerManager().broadcastChatMessage(new LiteralText(String.format("<%s> o/",playerName)), MessageType.CHAT, UUID.randomUUID());
    }
}
