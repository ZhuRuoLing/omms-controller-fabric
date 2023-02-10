package net.zhuruoling.omms.controller.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.zhuruoling.omms.controller.fabric.command.AnnouncementCommand;
import net.zhuruoling.omms.controller.fabric.command.MenuCommand;
import net.zhuruoling.omms.controller.fabric.command.QQCommand;
import net.zhuruoling.omms.controller.fabric.command.SendToConsoleCommand;
import net.zhuruoling.omms.controller.fabric.config.Config;
import net.zhuruoling.omms.controller.fabric.config.SharedVariable;
import net.zhuruoling.omms.controller.fabric.network.*;
import net.zhuruoling.omms.controller.fabric.network.http.HttpServerMainKt;
import net.zhuruoling.omms.controller.fabric.util.OmmsCommandOutput;
import net.zhuruoling.omms.controller.fabric.util.Util;
import org.slf4j.Logger;

import java.util.Objects;

public class OmmsControllerFabric implements DedicatedServerModInitializer {

    private final Logger logger = LogUtils.getLogger();

    private static void onServerStop() {
        if (Config.INSTANCE.isEnableRemoteControl() || Config.INSTANCE.isEnableChatBridge())
            SharedVariable.getSender().setStopped(true);
        if (Config.INSTANCE.isEnableChatBridge())
            SharedVariable.getChatReceiver().interrupt();
        if (Config.INSTANCE.isEnableRemoteControl()) {
            SharedVariable.getInstructionReceiver().interrupt();
            HttpServerMainKt.httpServer.stop(1000, 1000);
            HttpServerMainKt.httpServerThread.interrupt();
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
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> new SendToConsoleCommand().register(dispatcher));
        }

        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> new AnnouncementCommand().register(dispatcher)));

        if (Config.INSTANCE.isEnableChatBridge()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> new QQCommand().register(dispatcher));
        }

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("crashNow").requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(4)).executes(context -> {
                SharedVariable.shouldCrash = (true);
                return 0;
            }));
        });

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStart);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> onServerStop());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> onServerStop());
        SharedVariable.ready = true;
        logger.info("Hello World!");
    }

    private void onServerStart(MinecraftServer server) {
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
            SharedVariable.setChatReceiver(chatReceiver);
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
            SharedVariable.setInstructionReceiver(instructionReceiver);
            HttpServerMainKt.httpServerThread = HttpServerMainKt.serverMain(Config.INSTANCE.getHttpServerPort(), server);
        }
        if (Config.INSTANCE.isEnableRemoteControl() || Config.INSTANCE.isEnableChatBridge()) {
            var sender = new UdpBroadcastSender();
            sender.setDaemon(true);
            sender.start();
            SharedVariable.setSender(sender);
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
