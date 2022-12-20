package net.zhuruoling.omms.controller.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.argument.NbtCompoundTagArgumentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.MessageType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.zhuruoling.omms.controller.fabric.announcement.Announcement;
import net.zhuruoling.omms.controller.fabric.config.ConstantStorage;
import net.zhuruoling.omms.controller.fabric.network.*;
import net.zhuruoling.omms.controller.fabric.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class OmmsControllerFabric implements DedicatedServerModInitializer {

    private final Logger logger = LogManager.getLogger();

    @Override
    public void onInitializeServer() {
        ConstantStorage.init();
        if (!ConstantStorage.isEnableWhitelist()) return;

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(literal("menu")
                .then(argument("data", NbtCompoundTagArgumentType.nbtCompound()).requires(source -> source.hasPermissionLevel(0)).executes(context -> {
                    try {
                        CompoundTag compound = NbtCompoundTagArgumentType.getCompoundTag(context, "data");
                        if (!compound.contains("servers")) {
                            context.getSource().sendError(new LiteralText("Wrong server data."));
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
                        Text serverText = Texts.join(serverEntries, (text -> new LiteralText(text.asString() + "  ")));
                        context.getSource().sendFeedback(new LiteralText(String.format("----------Welcome to %s server!----------",ConstantStorage.getControllerName())), false);
                        context.getSource().sendFeedback(new LiteralText("    "), false);
                        context.getSource().sendFeedback(serverText, false);
                        context.getSource().sendFeedback(new LiteralText("Type \"/announcement latest\" to fetch latest announcement."), false);
                        return 1;

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return 1;
                }))));

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("sendToConsole")
                    .then(
                            RequiredArgumentBuilder.<ServerCommandSource, String>argument("content", StringArgumentType.greedyString()).requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(4)).executes(context -> {
                                        logger.info(String.format("<OMMS_Controller> %s",StringArgumentType.getString(context, "content")));
                                        return 0;
                                    }

                            )
                    )
            );
        });

        CommandRegistrationCallback.EVENT.register(((dispatcher, dedicated) -> dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("announcement")
                .then(LiteralArgumentBuilder.<ServerCommandSource>literal("latest")
                        .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0))
                        .executes(context -> {
                            String url = String.format("http://%s:%d/announcement/latest",ConstantStorage.getHttpQueryAddress(), ConstantStorage.getHttpQueryPort());
                            return getAnnouncementToPlayerFromUrl(context, url);
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
                                            String url = String.format("http://%s:%d/announcement/get/%s",ConstantStorage.getHttpQueryAddress(), ConstantStorage.getHttpQueryPort(), name);
                                            return getAnnouncementToPlayerFromUrl(context, url);
                                        })
                        )
                )
                .then(LiteralArgumentBuilder.<ServerCommandSource>literal("list")
                        .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0)).executes(context -> {
                            String url = String.format("http://%s:%d/announcement/list",ConstantStorage.getHttpQueryAddress(), ConstantStorage.getHttpQueryPort());
                            String result = Objects.requireNonNull(Util.invokeHttpGetRequest(url));
                            try {
                                String[] list = new Gson().fromJson(result, String[].class);
                                ArrayList<Text> texts = new ArrayList<>();
                                for (String s : list) {
                                    System.out.println(s);
                                    var text = Texts.join(
                                                    List.of(
                                                            new LiteralText("["),
                                                            new LiteralText(s)
                                                                    .copy()
                                                                    .setStyle(
                                                                            Style.EMPTY
                                                                                    .withColor(Formatting.GREEN)
                                                                    ),
                                                            new LiteralText("]")
                                                    ),
                                                    text1 -> text1
                                            )
                                            .copy()
                                            .setStyle(
                                                    Style.EMPTY
                                                            .withHoverEvent(
                                                                    new HoverEvent(
                                                                            HoverEvent.Action.SHOW_TEXT,
                                                                            new LiteralText("Click to get announcement.")
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
                                context.getSource().sendFeedback(new LiteralText("-------Announcements-------"), false);
                                context.getSource().sendFeedback(new LiteralText(""), false);
                                context.getSource().sendFeedback(Texts.join(texts, text -> text.copy().append(new LiteralText(" "))), false);
                                context.getSource().sendFeedback(new LiteralText(""), false);
                                return 0;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return 0;
                        })
                )
        )));

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("qq")
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

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var chatReceiver = new UdpReceiver(server, Util.TARGET_CHAT, (s, m) -> {
                var broadcast = new Gson().fromJson(m, Broadcast.class);
                if (Objects.equals(broadcast.getId(), ConstantStorage.getOldId())) return;
                //logger.info(String.format("%s <%s[%s]> %s", Objects.requireNonNull(broadcast).getChannel(), broadcast.getPlayer(), broadcast.getServer(), broadcast.getContent()));
                if (broadcast.getPlayer().startsWith("\ufff3\ufff4")) {
                    server.execute(() -> server.getPlayerManager().broadcastChatMessage(Util.fromBroadcastToQQ(broadcast), MessageType.CHAT, UUID.randomUUID()));
                    return;
                }
                if (!Objects.equals(broadcast.getServer(), ConstantStorage.getControllerName())) {
                    server.execute(() -> server.getPlayerManager().broadcastChatMessage(Util.fromBroadcast(broadcast), MessageType.CHAT, UUID.randomUUID()));
                }
            });

            var instructionReceiver = new UdpReceiver(server, Util.TARGET_CONTROL, (s, m) -> {

                Gson gson = new GsonBuilder().serializeNulls().create();
                Instruction instruction = gson.fromJson(m, Instruction.class);

                if (instruction.getControllerType() == ControllerTypes.FABRIC) {
                    if (instruction.getType() == InstructionType.UPLOAD_STATUS) {
                        logger.info("Sending status.");
                        UdpBroadcastSender.Target target = gson.fromJson(instruction.getCommandString(), UdpBroadcastSender.Target.class);
                        Util.sendStatus(s, target);
                    } else {
                        if (instruction.getType() == InstructionType.RUN_COMMAND) {
                            if (Objects.equals(instruction.getTargetControllerName(), ConstantStorage.getControllerName())) {
                                logger.info(String.format("Received Command: %s",instruction.getCommandString()));
                                server.execute(() -> {
                                    var dispatcher = server.getCommandManager().getDispatcher();
                                    //var results = dispatcher.parse(instruction.getCommandString(), server.getCommandSource());
                                    server.getCommandManager().execute(server.getCommandSource(),instruction.getCommandString());
                                });

                            }
                        }
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
                Text text = new LiteralText("No announcement.").copy().setStyle(Style.EMPTY.withColor(Formatting.AQUA));
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
            Text text = new LiteralText("No announcement.").copy().setStyle(Style.EMPTY.withColor(Formatting.AQUA));
            context.getSource().sendFeedback(text, false);
            return 0;
        }

        return 0;
    }
}
