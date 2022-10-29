package net.zhuruoling.omms.controller.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.zhuruoling.omms.controller.fabric.announcement.Announcement;
import net.zhuruoling.omms.controller.fabric.config.ConstantStorage;
import net.zhuruoling.omms.controller.fabric.network.*;
import net.zhuruoling.omms.controller.fabric.util.Util;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
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
                        context.getSource().sendFeedback(Text.of("Type \"/announcement latest\" to fetch latest announcement."), false);
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

        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("announcement")
                    .then(LiteralArgumentBuilder.<ServerCommandSource>literal("latest")
                            .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0))
                            .executes(context -> {
                                String url = "http://%s:%d/announcement/latest".formatted(ConstantStorage.getHttpQueryAddress(), ConstantStorage.getHttpQueryPort());
                                return getAnnouncementToPlayerFromUrl(context, url);
                            })
                    )
                    .then(LiteralArgumentBuilder.<ServerCommandSource>literal("get")
                            .then(
                                    RequiredArgumentBuilder.<ServerCommandSource, String>argument("name", word())
                                            .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0))
                                            .executes(context -> {
                                                String name = StringArgumentType.getString(context, "name");
                                                String url = "http://%s:%d/announcement/get/%s".formatted(ConstantStorage.getHttpQueryAddress(), ConstantStorage.getHttpQueryPort(), name);
                                                return getAnnouncementToPlayerFromUrl(context, url);
                                            })
                            )
                    )
                    .then(LiteralArgumentBuilder.<ServerCommandSource>literal("list")
                            .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0)).executes(context -> {
                                String url = "http://%s:%d/announcement/list".formatted(ConstantStorage.getHttpQueryAddress(), ConstantStorage.getHttpQueryPort());
                                String result = Objects.requireNonNull(Util.invokeHttpGetRequest(url));
                                try {
                                    String[] list = new Gson().fromJson(result, String[].class);
                                    ArrayList<Text> texts = new ArrayList<>();
                                    for (String s : list) {
                                        System.out.println(s);
                                        var text=Texts.join(
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
                                                        )
                                        ;
                                        System.out.println(text);

                                    }
                                    System.out.println(texts);
                                    context.getSource().sendFeedback(Text.of("-------Announcements-------"), false);
                                    context.getSource().sendFeedback(Text.of(""), false);
                                    context.getSource().sendFeedback(Texts.join(texts, Text.of(" ")), false);
                                    context.getSource().sendFeedback(Text.of(""), false);
                                    return 0;
                                }
                                catch(Exception e){
                                    e.printStackTrace();
                                }
                                return 0;
                            })
                    )
            );
        }));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("qq").requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0))
                    .then(
                            RequiredArgumentBuilder.<ServerCommandSource, String>argument("content", StringArgumentType.greedyString()).requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0)).executes(context -> {
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
                if (broadcast.getPlayer().startsWith("\ufff3\ufff4")) {
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

    private int getAnnouncementToPlayerFromUrl(CommandContext<ServerCommandSource> context, String url) {
        String result = Util.invokeHttpGetRequest(url);
        if (result != null) {
            if (result.equals("NO_ANNOUNCEMENT")) {
                Text text = Texts.join(Text.of("No announcement.").copyContentOnly().getWithStyle(Style.EMPTY.withColor(Formatting.AQUA)), Text.empty());
                context.getSource().sendFeedback(text, false);
                return 0;
            }
            System.out.println(result);
            try {
                String jsonStr = new String(Base64.getDecoder().decode(result.replace("\"", "")), StandardCharsets.UTF_8);
                Gson gson = new GsonBuilder().serializeNulls().create();
                var announcement = gson.fromJson(jsonStr, Announcement.class);
                context.getSource().sendFeedback(Util.fromAnnouncement(announcement), false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Text text = Texts.join(Text.of("No announcement.").copyContentOnly().getWithStyle(Style.EMPTY.withColor(Formatting.AQUA)), Text.empty());
            context.getSource().sendFeedback(text, false);
            return 0;
        }

        return 0;
    }
}
