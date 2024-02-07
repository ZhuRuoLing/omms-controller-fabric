package icu.takeneko.omms.controller.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import icu.takeneko.omms.controller.fabric.util.Util;
import net.minecraft.server.command.ServerCommandSource;

public class QQCommand implements Command<ServerCommandSource> {

    @Override
    public void register(CommandDispatcher<ServerCommandSource> commandDispatcher) {
        commandDispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("qq")
                .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0))
                .then(
                        RequiredArgumentBuilder.<ServerCommandSource, String>argument("content", StringArgumentType.greedyString()).requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0)).executes(context -> {
                                    var content = StringArgumentType.getString(context, "content");
                                    var sender = context.getSource().getDisplayName().getString();
                                    Util.sendChatBroadcast(content, "\ufff3\ufff4" + sender);
                                    return 0;
                                }
                        )

                ));
    }
}
