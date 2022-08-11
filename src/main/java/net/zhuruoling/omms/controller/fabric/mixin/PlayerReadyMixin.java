package net.zhuruoling.omms.controller.fabric.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;


@Mixin(PlayerManager.class)
public class PlayerReadyMixin {
    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final private MinecraftServer server;

    @Inject(method = "onPlayerConnect",at = @At("RETURN"))
    private void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci){
        Objects.requireNonNull(player.getServer()).getCommandManager().execute(player.getCommandSource(),"say o/");
        var commandManager = Objects.requireNonNull(player.getServer()).getCommandManager();
        try {
            commandManager.getDispatcher().execute(String.format("execute as %s at @s run say o/", player.getName().asString()),server.getCommandSource());
            //player.sendMessage(Texts.join(ServerEntryTextFactory.generateServerEntryText("a","b",true),Texts.toText(() -> "")), false);
        } catch (CommandSyntaxException e) {
            player.sendMessage(Text.of(String.valueOf(e)),false);
            LOGGER.error("Cannot execute /menu command.",e);
        }
    }
}
