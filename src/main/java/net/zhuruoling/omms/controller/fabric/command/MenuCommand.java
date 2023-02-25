package net.zhuruoling.omms.controller.fabric.command;

import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.zhuruoling.omms.controller.fabric.config.Config;
import net.zhuruoling.omms.controller.fabric.config.ServerMapping;
import net.zhuruoling.omms.controller.fabric.util.Util;

import java.util.ArrayList;
import java.util.Objects;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class MenuCommand implements Command<ServerCommandSource> {

    @Override
    public void register(CommandDispatcher<ServerCommandSource> commandDispatcher) {
        commandDispatcher.register(
                literal("menu")
                        .then(argument("data", NbtCompoundArgumentType.nbtCompound()).requires(source -> source.hasPermissionLevel(0)).executes(context -> {
                            try {
                                NbtCompound compound = NbtCompoundArgumentType.getNbtCompound(context, "data");
                                if (!compound.contains("servers")) {
                                    context.getSource().sendError(Text.of("Wrong server data."));
                                    return -1;
                                }
                                String data = compound.getString("servers");
                                String[] servers = new GsonBuilder().serializeNulls().create().fromJson(data, String[].class);
                                ArrayList<Text> serverEntries = new ArrayList<>();
                                String currentServer = Config.INSTANCE.getWhitelistName();
                                for (String server : servers) {
                                    boolean isCurrentServer = Objects.equals(currentServer, server);
                                    ServerMapping mapping = Config.INSTANCE.getServerMappings().get(server);
                                    if (Objects.isNull(mapping)) {
                                        serverEntries.add(Util.fromServerString(server, null, false, true));
                                        continue;
                                    }
                                    serverEntries.add(Util.fromServerString(mapping.getDisplayName(), mapping.getProxyName(), isCurrentServer, false));
                                }
                                Text serverText = Texts.join(serverEntries, Util.SPACE);
                                context.getSource().sendFeedback(Text.of("----------Welcome to %s server!----------".formatted(Config.INSTANCE.getControllerName())), false);
                                context.getSource().sendFeedback(Text.of("    "), false);
                                context.getSource().sendFeedback(serverText, false);
                                context.getSource().sendFeedback(Text.of("Type \"/announcement latest\" to fetch latest announcement."), false);
                                return 1;

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return 1;
                        }))
        );
    }
}
