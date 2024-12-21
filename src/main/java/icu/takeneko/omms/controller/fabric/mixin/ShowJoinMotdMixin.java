package icu.takeneko.omms.controller.fabric.mixin;


import icu.takeneko.omms.controller.fabric.config.Config;
import icu.takeneko.omms.controller.fabric.config.ServerMapping;
import icu.takeneko.omms.controller.fabric.network.NetworkUtilKt;
import icu.takeneko.omms.controller.fabric.util.Util;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Mixin(PlayerList.class)
public class ShowJoinMotdMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void displayJoinMotd(Connection connection, ServerPlayer player, CallbackInfo ci) {
        if (!Config.INSTANCE.isEnableJoinMotd()) return;
        if (connection.getRemoteAddress() == null) {
            return;
        }
        try {
            connection.send(new ClientboundTabListPacket(Component.empty(), Component.empty()));
            String playerName = player.getName().getString();
            try {
                var servers = NetworkUtilKt.queryPlayerInAllWhitelist(playerName);
                List<Component> serverEntries = new ArrayList<>();

                String currentServer = Config.INSTANCE.getWhitelistName();

                for (String server : servers) {
                    boolean isCurrentServer = Objects.equals(currentServer, server);
                    ServerMapping mapping = Config.INSTANCE.getServerMappings().get(server);
                    if (mapping == null) {
                        serverEntries.add(Util.fromServerString(server, null, false, true));
                    } else {
                        serverEntries.add(Util.fromServerString(mapping.getDisplayName(), mapping.getProxyName(), isCurrentServer, false));
                    }
                }
                Component serverText = ComponentUtils.formatList(serverEntries, Util.SPACE);
                player.sendSystemMessage(Component.literal("----------Welcome to %s server!----------".formatted(Config.INSTANCE.getControllerName())), false);
                player.sendSystemMessage(Component.literal("    "), false);
                player.sendSystemMessage(serverText, false);
                player.sendSystemMessage(Component.literal("Type \"/announcement latest\" to fetch latest announcement."), false);
                server.getPlayerList().broadcastSystemMessage(Component.literal("<%s> o/".formatted(playerName)), false);
            } catch (RuntimeException ignored) {

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
