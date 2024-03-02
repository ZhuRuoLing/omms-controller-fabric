package icu.takeneko.omms.controller.fabric.mixin;


import icu.takeneko.omms.controller.fabric.config.Config;
import icu.takeneko.omms.controller.fabric.config.ServerMapping;
import icu.takeneko.omms.controller.fabric.network.NetworkUtilKt;
import icu.takeneko.omms.controller.fabric.util.Util;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Mixin(PlayerManager.class)
public class ShowJoinMotdMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "onPlayerConnect", at = @At("RETURN"))
    private void displayJoinMotd(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        if (!Config.INSTANCE.isEnableJoinMotd()) return;
        if (connection.getAddress() == null) {
            return;
        }
        try {
            connection.send(new PlayerListHeaderS2CPacket(Text.empty(), Text.empty()));
            String playerName = player.getName().copyContentOnly().getString();
            try {
                var servers = NetworkUtilKt.queryPlayerInAllWhitelist(playerName);
                List<Text> serverEntries = new ArrayList<>(servers.stream().map(Text::of).toList());
                String currentServer = Config.INSTANCE.getWhitelistName();
                for (String server : servers) {
                    boolean isCurrentServer = Objects.equals(currentServer, server);
                    ServerMapping mapping = Config.INSTANCE.getServerMappings().get(server);
                    if (mapping == null) {
                        serverEntries.add(Util.fromServerString(server, null, false, true));
                        continue;
                    }
                    serverEntries.add(Util.fromServerString(mapping.getDisplayName(), mapping.getProxyName(), isCurrentServer, false));
                }
                Text serverText = Texts.join(serverEntries, Util.SPACE);
                player.sendMessage(Text.of("----------Welcome to %s server!----------".formatted(Config.INSTANCE.getControllerName())), false);
                player.sendMessage(Text.of("    "), false);
                player.sendMessage(serverText, false);
                player.sendMessage(Text.of("Type \"/announcement latest\" to fetch latest announcement."), false);
                server.getPlayerManager().broadcast(Text.of("<%s> o/".formatted(playerName)), false);
            }catch (RuntimeException ignored){

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
