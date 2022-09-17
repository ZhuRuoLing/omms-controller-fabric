package net.zhuruoling.omms.controller.fabric.mixin;


import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.message.MessageType;
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

    @Shadow @Final private MinecraftServer server;

    @Inject(method = "onPlayerConnect",at = @At("RETURN"))
    private void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci){
        if (!ConstantStorage.isEnable())return;
        String playerName = player.getName().copyContentOnly().getString();
        String url = "http://%s:%d/whitelist/queryAll/%s".formatted(ConstantStorage.getHttpQueryAddress(), ConstantStorage.getHttpQueryPort(),playerName);
        String result = getPlayerInWhitelists(url);
        NbtCompound compound = new NbtCompound();
        compound.putString("servers", result);
        var dispatcher = server.getCommandManager().getDispatcher();

        try {
            //System.out.printf("menu %s%n", compound.asString());
            dispatcher.execute("menu %s".formatted(compound.asString()), player.getCommandSource());
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }

        server.getPlayerManager().broadcast(Text.of("<%s> o/".formatted(playerName)),MessageType.CHAT);
    }
}
