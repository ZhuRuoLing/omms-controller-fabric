package net.zhuruoling.omms.controller.fabric;

import com.google.gson.Gson;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.zhuruoling.omms.controller.fabric.command.AnnouncementCommand;
import net.zhuruoling.omms.controller.fabric.command.MenuCommand;
import net.zhuruoling.omms.controller.fabric.command.QQCommand;
import net.zhuruoling.omms.controller.fabric.command.SendToConsoleCommand;
import net.zhuruoling.omms.controller.fabric.config.ChatbridgeImplementation;
import net.zhuruoling.omms.controller.fabric.config.Config;
import net.zhuruoling.omms.controller.fabric.config.SharedVariable;
import net.zhuruoling.omms.controller.fabric.network.Broadcast;
import net.zhuruoling.omms.controller.fabric.network.UdpBroadcastSender;
import net.zhuruoling.omms.controller.fabric.network.UdpReceiver;
import net.zhuruoling.omms.controller.fabric.network.WebsocketChatClient;
import net.zhuruoling.omms.controller.fabric.network.http.HttpServerMainKt;
import net.zhuruoling.omms.controller.fabric.util.Util;

import java.util.Objects;

public class OmmsControllerFabric implements DedicatedServerModInitializer {

    //private final Logger logger = LogUtils.getLogger();
    @SuppressWarnings("deprecated")
    private static void onServerStop(MinecraftServer minecraftServer) {
        if (Config.INSTANCE.isEnableChatBridge() && Config.INSTANCE.getChatbridgeImplementation() == ChatbridgeImplementation.UDP) {
            switch (Config.INSTANCE.getChatbridgeImplementation()) {
                case UDP -> {
                    if (SharedVariable.getSender() != null) {
                        SharedVariable.getSender().setStopped(true);
                        SharedVariable.getChatReceiver().interrupt();
                    }
                }
                case WS -> {
                    if (SharedVariable.getWebsocketChatClient() != null) {
                        SharedVariable.getWebsocketChatClient().interrupt();
                    }
                }
                case DISABLED -> {
                }
            }
        }
        if (Config.INSTANCE.isEnableRemoteControl()) {
            HttpServerMainKt.httpServer.stop(1000, 1000);
            HttpServerMainKt.httpServerThread.interrupt();
            HttpServerMainKt.sendToAllConnection("\u1145:END:\u1919");
        }
        SharedVariable.getExecutorService().shutdown();
    }

    private static void registerMenuCommand() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> new MenuCommand().register(dispatcher));
    }

    @Override
    public void onInitializeServer() {
        Config.INSTANCE.load();

        if (Config.INSTANCE.isEnableJoinMotd()) {
            registerMenuCommand();
        }

        if (Config.INSTANCE.isEnableRemoteControl()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                    new SendToConsoleCommand().register(dispatcher));
            Util.addAppender();
        }

        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) ->
                new AnnouncementCommand().register(dispatcher)));

        if (Config.INSTANCE.isEnableChatBridge()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                    new QQCommand().register(dispatcher));

        }

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("crashNow")
                        .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(4))
                        .executes(context -> {
                            SharedVariable.shouldCrash = (true);
                            return 0;
                        })));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("omms-reload")
                        .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(4))
                        .executes(context -> {
                            Config.INSTANCE.load();
                            HttpServerMainKt.httpServer.stop(1, 1);
                            HttpServerMainKt.httpServerThread.interrupt();
                            HttpServerMainKt.httpServerThread = HttpServerMainKt.serverMain(Config.INSTANCE.getHttpServerPort(), context.getSource().getServer());
                            context.getSource().sendFeedback(() -> Text.of("Config reloaded.").copyContentOnly().setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.AQUA))), true);
                            return 0;
                        })));

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStart);
        ServerLifecycleEvents.SERVER_STOPPING.register(OmmsControllerFabric::onServerStop);
        ServerLifecycleEvents.SERVER_STOPPED.register(OmmsControllerFabric::onServerStop);
        SharedVariable.ready = true;

        LogUtils.getLogger().info("Hello World!");

    }

    private void onServerStart(MinecraftServer server) {
        if (Config.INSTANCE.isEnableChatBridge()) {
            switch (Config.INSTANCE.getChatbridgeImplementation()) {
                case WS -> {
                    WebsocketChatClient websocketChatClient = new WebsocketChatClient(server);
                    websocketChatClient.setDaemon(true);
                    websocketChatClient.start();
                    SharedVariable.setWebsocketChatClient(websocketChatClient);
                }
                case UDP -> {
                    var chatReceiver = new UdpReceiver(server, Util.TARGET_CHAT, (s, m) -> {
                        var broadcast = new Gson().fromJson(m, Broadcast.class);
                        if (!(Objects.equals(broadcast.getChannel(), Config.INSTANCE.getChatChannel()))) return;
                        //LogUtils.getLogger().info(String.format("%s <%s[%s]> %s", Objects.requireNonNull(broadcast).getChannel(), broadcast.getPlayer(), broadcast.getServer(), broadcast.getContent()));
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
                    SharedVariable.setChatReceiver(chatReceiver);
                }
                case DISABLED -> {
                }
            }
        }

        if (Config.INSTANCE.isEnableRemoteControl()) {
            HttpServerMainKt.httpServerThread = HttpServerMainKt.serverMain(Config.INSTANCE.getHttpServerPort(), server);
        }
        if (Config.INSTANCE.isEnableChatBridge() && Config.INSTANCE.getChatbridgeImplementation() == ChatbridgeImplementation.UDP) {
            var sender = new UdpBroadcastSender();
            sender.setDaemon(true);
            sender.start();
            SharedVariable.setSender(sender);
        }
    }


}
