package icu.takeneko.omms.controller.fabric;

import com.google.gson.Gson;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import icu.takeneko.omms.controller.fabric.network.http.HttpServerMainKt;
import icu.takeneko.omms.controller.fabric.permission.PermissionRuleManager;
import icu.takeneko.omms.controller.fabric.util.Util;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import icu.takeneko.omms.controller.fabric.command.AnnouncementCommand;
import icu.takeneko.omms.controller.fabric.command.QQCommand;
import icu.takeneko.omms.controller.fabric.command.SendToConsoleCommand;
import icu.takeneko.omms.controller.fabric.config.ChatbridgeImplementation;
import icu.takeneko.omms.controller.fabric.config.Config;
import icu.takeneko.omms.controller.fabric.config.SharedVariable;
import icu.takeneko.omms.controller.fabric.network.Broadcast;
import icu.takeneko.omms.controller.fabric.network.UdpBroadcastSender;
import icu.takeneko.omms.controller.fabric.network.UdpReceiver;
import icu.takeneko.omms.controller.fabric.network.WebsocketChatClient;
import icu.takeneko.omms.controller.fabric.permission.MappedNames;
import icu.takeneko.omms.controller.fabric.permission.PatchUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static net.minecraft.server.command.CommandManager.literal;

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
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("save").executes(context -> {
                var src = context.getSource();
                try{
                    PermissionRuleManager.INSTANCE.save();
                }catch (Exception e){
                    e.printStackTrace();
                }
                return 0;
            }));
        }));
    }

    @Override
    public void onInitializeServer() {
        Config.INSTANCE.load();
        if (!Config.INSTANCE.getPermissionConfig().isEmpty()) {
            MappedNames.mapNames();
            PatchUtil.init();
            PatchUtil.initTransformer();
            var path = FabricLoader.getInstance().getConfigDir().resolve(Config.INSTANCE.getPermissionConfig());
            PermissionRuleManager.INSTANCE.loadFromRulesFile(path.toFile());
            PermissionRuleManager.INSTANCE.init();
        }

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

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            Thread launchThread = new Thread(null, () -> onServerStart(server), "OMMS-LauncherThread");
            launchThread.start();
        });
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
                    var chatReceiver = getUdpReceiver(server);
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

    @NotNull
    private static UdpReceiver getUdpReceiver(MinecraftServer server) {
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
        return chatReceiver;
    }


}
