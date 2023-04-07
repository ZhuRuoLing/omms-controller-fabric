package net.zhuruoling.omms.controller.fabric.mixin;


import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.zhuruoling.omms.controller.fabric.config.Config;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

import static net.zhuruoling.omms.controller.fabric.util.Util.invokeHttpGetRequest;


@Mixin(PlayerManager.class)
public class ShowJoinMotdMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "onPlayerConnect", at = @At("RETURN"))
    private void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        if (!Config.INSTANCE.isEnableJoinMotd()) return;
        if (connection.getAddress() == null) {
            return;
        }
        try {
            connection.send(new PlayerListHeaderS2CPacket(Text.empty(), Text.empty()));
            String playerName = player.getName().copyContentOnly().getString();
            String url = "http://%s:%d/whitelist/queryAll/%s".formatted(Config.INSTANCE.getHttpQueryAddress(), Config.INSTANCE.getHttpQueryPort(), playerName);
            var response = invokeHttpGetRequest(url);
            if (response.getLeft() != 200) {
                player.sendMessageToClient(
                        Text.of("Cannot communicate with OMMS Central Server.")
                                .copyContentOnly()
                                .setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.RED))),
                        true);
                return;
            }
            String result = response.getRight();
            NbtCompound compound = new NbtCompound();
            compound.putString("servers", result);
            var dispatcher = Objects.requireNonNull(player.getServer()).getCommandManager().getDispatcher();

            Objects.requireNonNull(player.getServer()).getCommandManager().execute(
                    dispatcher.parse(
                            "menu %s".formatted(compound.asString())
                            , player.getCommandSource()
                    )
                    , "menu %s".formatted(compound.asString())
            );
            server.getPlayerManager().broadcast(Text.of("<%s> o/".formatted(playerName)), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
