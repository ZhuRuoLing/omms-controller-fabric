package icu.takeneko.omms.controller.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;


public class SendToConsoleCommand implements Command<CommandSourceStack> {

    private final Logger logger = LogUtils.getLogger();

    @Override
    public void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(
            LiteralArgumentBuilder.<CommandSourceStack>literal("sendToConsole")
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("content", StringArgumentType.greedyString()).requires(serverCommandSource -> serverCommandSource.hasPermission(4)).executes(context -> {
                    logger.info("<OMMS_Controller> %s".formatted(StringArgumentType.getString(context, "content")));
                    return 0;
                }))
        );
    }
}
