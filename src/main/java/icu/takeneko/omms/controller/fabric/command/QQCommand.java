package icu.takeneko.omms.controller.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import icu.takeneko.omms.controller.fabric.util.Util;
import net.minecraft.commands.CommandSourceStack;

public class QQCommand implements Command<CommandSourceStack> {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("qq")
                .requires(serverCommandSource -> serverCommandSource.hasPermission(0))
                .then(
                        RequiredArgumentBuilder.<CommandSourceStack, String>argument("content", StringArgumentType.greedyString()).requires(serverCommandSource -> serverCommandSource.hasPermission(0)).executes(context -> {
                                    var content = StringArgumentType.getString(context, "content");
                                    var sender = context.getSource().getDisplayName().getString();
                                    Util.sendChatBroadcast(content, "\ufff3\ufff4" + sender);
                                    return 0;
                                }
                        )

                ));
    }
}
