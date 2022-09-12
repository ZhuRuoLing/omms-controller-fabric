package net.zhuruoling.omms.controller.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.zhuruoling.omms.controller.fabric.config.ConstantStorage;
import net.zhuruoling.omms.controller.fabric.network.*;
import net.zhuruoling.omms.controller.fabric.util.Util;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Objects;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class OmmsControllerFabric implements DedicatedServerModInitializer {

    private final Logger logger = LogUtils.getLogger();

    @Override
    public void onInitializeServer() {
        ConstantStorage.init();
        if (!ConstantStorage.isEnable()) return;
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("menu")
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
                        String currentServer = ConstantStorage.getWhitelistName();
                        for (String server : servers) {
                            boolean isCurrentServer = Objects.equals(currentServer, server);
                            ConstantStorage.ServerMapping mapping = ConstantStorage.getServerMappings().get(server);
                            if (Objects.isNull(mapping)) {
                                serverEntries.add(Util.fromServerString(server, null, false, true));
                                continue;
                            }
                            serverEntries.add(Util.fromServerString(mapping.getDisplayName(), mapping.getProxyName(), isCurrentServer, false));
                        }
                        Text serverText = Texts.join(serverEntries, Util.SPACE);
                        context.getSource().sendFeedback(Text.of("----------Welcome to %s server!----------".formatted(ConstantStorage.getControllerName())), false);
                        context.getSource().sendFeedback(Text.of("    "), false);
                        context.getSource().sendFeedback(serverText, false);
                        return 1;

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return 1;
                }))));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("sendToConsole")
                    .then(
                            RequiredArgumentBuilder.<ServerCommandSource, String>argument("content", StringArgumentType.greedyString()).requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(4)).executes(context -> {
                                        logger.info("<OMMS_Controller> %s".formatted(StringArgumentType.getString(context, "content")));
                                        return 0;
                                    }

                            )
                    )
            );
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("qq")
                    .then(
                            RequiredArgumentBuilder.<ServerCommandSource, String>argument("content", StringArgumentType.greedyString()).requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(4)).executes(context -> {
                                        var content = StringArgumentType.getString(context, "content");
                                        var sender = context.getSource().getDisplayName().getString();
                                        Util.sendChatBroadcast(content, "\ufff3\ufff4" + sender);
                                        return 0;
                                    }
                            )
                    )
            );
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var chatReceiver = new UdpReceiver(server, Util.TARGET_CHAT, (s, m) -> {
                var broadcast = new Gson().fromJson(m, Broadcast.class);
                //logger.info(String.format("%s <%s[%s]> %s", Objects.requireNonNull(broadcast).getChannel(), broadcast.getPlayer(), broadcast.getServer(), broadcast.getContent()));
                if (broadcast.getPlayer().startsWith("\ufff3\ufff4")){
                    server.execute(() -> server.getPlayerManager().broadcast(Util.fromBroadcastToQQ(broadcast), false));

                }
                if (!Objects.equals(broadcast.getServer(), ConstantStorage.getControllerName())) {
                    server.execute(() -> server.getPlayerManager().broadcast(Util.fromBroadcast(broadcast), false));
                }
            });
            var instructionReceiver = new UdpReceiver(server, Util.TARGET_CONTROL, (s, m) -> {
                Gson gson = new GsonBuilder().serializeNulls().create();
                Instruction instruction = gson.fromJson(m, Instruction.class);
                if (instruction.getControllerType() == ControllerTypes.FABRIC) {
                    if (Objects.equals(instruction.getTargetControllerName(), ConstantStorage.getControllerName())) {
                        logger.info("Received Command: %s".formatted(instruction.getCommandString()));
                        server.execute(() -> {
                            var dispatcher = server.getCommandManager().getDispatcher();
                            var results = dispatcher.parse(instruction.getCommandString(), server.getCommandSource());
                            server.getCommandManager().execute(results, instruction.getCommandString());
                        });

                    }
                }
            });
            var sender = new UdpBroadcastSender();
            chatReceiver.setDaemon(true);
            sender.setDaemon(true);
            instructionReceiver.setDaemon(true);
            instructionReceiver.start();
            sender.start();
            chatReceiver.start();
            ConstantStorage.setSender(sender);
            ConstantStorage.setChatReceiver(chatReceiver);
            ConstantStorage.setInstructionReceiver(instructionReceiver);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ConstantStorage.getSender().setStopped(true);
            ConstantStorage.getChatReceiver().interrupt();
            ConstantStorage.getInstructionReceiver().interrupt();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ConstantStorage.getSender().setStopped(true);
            ConstantStorage.getChatReceiver().interrupt();
            ConstantStorage.getInstructionReceiver().interrupt();
        });
        logger.info("Hello World!");
    }
}
