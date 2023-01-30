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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.zhuruoling.omms.controller.fabric.config.Config;
import net.zhuruoling.omms.controller.fabric.config.RuntimeConstants;
import net.zhuruoling.omms.controller.fabric.config.ServerMapping;
import net.zhuruoling.omms.controller.fabric.network.*;
import net.zhuruoling.omms.controller.fabric.network.http.HttpServerMainKt;
import net.zhuruoling.omms.controller.fabric.util.OmmsCommandOutput;
import net.zhuruoling.omms.controller.fabric.util.Util;
import org.slf4j.Logger;

import java.util.*;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class OmmsControllerFabric implements DedicatedServerModInitializer {

    private final Logger logger = LogUtils.getLogger();

    private static void onServerStop() {
        if (Config.INSTANCE.isEnableRemoteControl() || Config.INSTANCE.isEnableChatBridge())
            RuntimeConstants.getSender().setStopped(true);
        if (Config.INSTANCE.isEnableChatBridge())
            RuntimeConstants.getChatReceiver().interrupt();
        if (Config.INSTANCE.isEnableRemoteControl()) {
            RuntimeConstants.getInstructionReceiver().interrupt();
            HttpServerMainKt.httpServer.stop(1000, 1000);
            HttpServerMainKt.httpServerThread.interrupt();
        }
        RuntimeConstants.getExecutorService().shutdownNow();
    }

    @Override
    public void onInitializeServer() {
        Config.INSTANCE.load();
        //if (RuntimeConstants.isEnableWhitelist()) return;
        if (Config.INSTANCE.isEnableJoinMotd()) {
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
                    }))));
        }

        if (Config.INSTANCE.isEnableRemoteControl()) {
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
        }

        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("announcement")
                .then(LiteralArgumentBuilder.<ServerCommandSource>literal("latest")
                        .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0))
                        .executes(context -> {
                            String url = "http://%s:%d/announcement/latest".formatted(Config.INSTANCE.getHttpQueryAddress(), Config.INSTANCE.getHttpQueryPort());
                            return Util.getAnnouncementToPlayerFromUrl(context, url);
                        })
                )
                .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0))
                .then(LiteralArgumentBuilder.<ServerCommandSource>literal("get")
                        .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0))
                        .then(
                                RequiredArgumentBuilder.<ServerCommandSource, String>argument("name", word())
                                        .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0))
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "name");
                                            String url = "http://%s:%d/announcement/get/%s".formatted(Config.INSTANCE.getHttpQueryAddress(), Config.INSTANCE.getHttpQueryPort(), name);
                                            return Util.getAnnouncementToPlayerFromUrl(context, url);
                                        })
                        )
                )
                .then(LiteralArgumentBuilder.<ServerCommandSource>literal("list")
                        .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0)).executes(context -> {
                            String url = "http://%s:%d/announcement/list".formatted(Config.INSTANCE.getHttpQueryAddress(), Config.INSTANCE.getHttpQueryPort());
                            try {
                                String result = Objects.requireNonNull(Util.invokeHttpGetRequest(url));
                                String[] list = new Gson().fromJson(result, String[].class);
                                ArrayList<Text> texts = new ArrayList<>();
                                for (String s : list) {
                                    System.out.println(s);
                                    var text = Texts.join(
                                                    List.of(
                                                            Text.of("["),
                                                            Text.of(s)
                                                                    .copyContentOnly()
                                                                    .setStyle(
                                                                            Style.EMPTY
                                                                                    .withColor(Formatting.GREEN)
                                                                    ),
                                                            Text.of("]")
                                                    ),
                                                    Text.of("")
                                            )
                                            .copyContentOnly()
                                            .setStyle(
                                                    Style.EMPTY
                                                            .withHoverEvent(
                                                                    new HoverEvent(
                                                                            HoverEvent.Action.SHOW_TEXT,
                                                                            Text.of("Click to get announcement.")
                                                                    )
                                                            )
                                                            .withClickEvent(
                                                                    new ClickEvent(
                                                                            ClickEvent.Action.RUN_COMMAND,
                                                                            "/announcement get %s".formatted(s)
                                                                    )
                                                            )
                                            );
                                    System.out.println(text);

                                }
                                System.out.println(texts);
                                context.getSource().sendFeedback(Text.of("-------Announcements-------"), false);
                                context.getSource().sendFeedback(Text.of(""), false);
                                context.getSource().sendFeedback(Texts.join(texts, Text.of(" ")), false);
                                context.getSource().sendFeedback(Text.of(""), false);
                                return 0;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return 0;
                        })
                )
        )));

        if (Config.INSTANCE.isEnableChatBridge()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("qq")
                    .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0))
                    .then(
                            RequiredArgumentBuilder.<ServerCommandSource, String>argument("content", StringArgumentType.greedyString()).requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0)).executes(context -> {
                                        var content = StringArgumentType.getString(context, "content");
                                        var sender = context.getSource().getDisplayName().getString();
                                        Util.sendChatBroadcast(content, "\ufff3\ufff4" + sender);
                                        return 0;
                                    }
                            )
                    )
            ));
        }


        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            onServerStart(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            onServerStop();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            onServerStop();
        });
        logger.info("Hello World!");
    }

    private void onServerStart(MinecraftServer server){
        if (Config.INSTANCE.isEnableChatBridge()) {
            var chatReceiver = new UdpReceiver(server, Util.TARGET_CHAT, (s, m) -> {
                var broadcast = new Gson().fromJson(m, Broadcast.class);
                if (!(Objects.equals(broadcast.getChannel(), Config.INSTANCE.getChatChannel()))) return;
                //logger.info(String.format("%s <%s[%s]> %s", Objects.requireNonNull(broadcast).getChannel(), broadcast.getPlayer(), broadcast.getServer(), broadcast.getContent()));
                if (broadcast.getPlayer().startsWith("\ufff3\ufff4")) {
                    server.execute(() -> server.getPlayerManager().broadcast(Util.fromBroadcastToQQ(broadcast), false));
                    return;
                }
                if (!Objects.equals(broadcast.getServer(), Config.INSTANCE.getControllerName())) {
                    server.execute(() -> server.getPlayerManager().broadcast(Util.fromBroadcast(broadcast), false));
                }
            });

            chatReceiver.setDaemon(true);
            chatReceiver.start();
            RuntimeConstants.setChatReceiver(chatReceiver);
        }

        if (Config.INSTANCE.isEnableRemoteControl()) {
            var instructionReceiver = new UdpReceiver(server, Util.TARGET_CONTROL, (s, m) -> {
                Gson gson = new GsonBuilder().serializeNulls().create();
                Instruction instruction = gson.fromJson(m, Instruction.class);
                if (instruction.getControllerType() != ControllerTypes.FABRIC) {
                    return;
                }
                runCommand(s, instruction);
            });
            instructionReceiver.setDaemon(true);
            instructionReceiver.start();
            RuntimeConstants.setInstructionReceiver(instructionReceiver);
            HttpServerMainKt.httpServerThread = HttpServerMainKt.serverMain(Config.INSTANCE.getHttpServerPort(), server);
        }
        if (Config.INSTANCE.isEnableRemoteControl() || Config.INSTANCE.isEnableChatBridge()) {
            var sender = new UdpBroadcastSender();
            sender.setDaemon(true);
            sender.start();
            RuntimeConstants.setSender(sender);
        }
    }

    private void runCommand(MinecraftServer server, Instruction instruction) {
        switch (instruction.getType()) {
            case UPLOAD_STATUS -> {
                logger.info("Sending status.");
                Util.sendStatus(server);
            }
            case RUN_COMMAND -> {
                if (!Objects.equals(instruction.getTargetControllerName(), Config.INSTANCE.getControllerName()))
                    return;
                logger.info("Received Command: %s".formatted(instruction.getCommandString()));
                server.execute(() -> {
                    var dispatcher = server.getCommandManager().getDispatcher();
                    var commandOutput = new OmmsCommandOutput(server);
                    var commandSource = commandOutput.createOmmsCommandSource();
                    var results = dispatcher.parse(instruction.getCommandString(), commandSource);
                    server.getCommandManager().execute(results, instruction.getCommandString());
                    var commandResult = commandOutput.asString();
                    Util.submitToExecutor(() -> Util.submitCommandLog(instruction.getCommandString(), commandResult));
                });
            }
        }
    }

}
