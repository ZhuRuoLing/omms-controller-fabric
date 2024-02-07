package icu.takeneko.omms.controller.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.server.command.ServerCommandSource;
import org.slf4j.Logger;


public class SendToConsoleCommand implements Command<ServerCommandSource> {

    private final Logger logger = LogUtils.getLogger();

    @Override
    public void register(CommandDispatcher<ServerCommandSource> commandDispatcher) {
        commandDispatcher.register(
                LiteralArgumentBuilder.<ServerCommandSource>literal("sendToConsole")
                        .then(RequiredArgumentBuilder.<ServerCommandSource, String>argument("content", StringArgumentType.greedyString()).requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(4)).executes(context -> {
                            logger.info("<OMMS_Controller> %s".formatted(StringArgumentType.getString(context, "content")));
                            return 0;
                        }))
        );
    }
}
