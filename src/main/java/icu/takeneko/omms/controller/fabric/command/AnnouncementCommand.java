package icu.takeneko.omms.controller.fabric.command;

import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import icu.takeneko.omms.controller.fabric.util.Util;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import icu.takeneko.omms.controller.fabric.config.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class AnnouncementCommand implements Command<ServerCommandSource> {
    @Override
    public void register(CommandDispatcher<ServerCommandSource> commandDispatcher) {
        commandDispatcher.register(
                LiteralArgumentBuilder.<ServerCommandSource>literal("announcement")
                        .then(LiteralArgumentBuilder.<ServerCommandSource>literal("latest")
                                .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0))
                                .executes(context -> {
                                    String url = "http://%s:%d/announcement/latest".formatted(Config.INSTANCE.getHttpQueryAddress(), Config.INSTANCE.getHttpQueryPort());
                                    return Util.sendAnnouncementFromUrlToPlayer(context, url);
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
                                                    return Util.sendAnnouncementFromUrlToPlayer(context, url);
                                                })
                                )
                        )
                        .then(LiteralArgumentBuilder.<ServerCommandSource>literal("list")
                                .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0)).executes(context -> {
                                    String url = "http://%s:%d/announcement/list".formatted(Config.INSTANCE.getHttpQueryAddress(), Config.INSTANCE.getHttpQueryPort());
                                    try {
                                        var pair = Objects.requireNonNull(Util.invokeHttpGetRequest(url));
                                        if (pair.getLeft() != 200) {
                                            context.getSource().sendError(Text.of("Cannot communicate with OMMS Central Server."));
                                            return -1;
                                        }
                                        String result = pair.getRight();
                                        HashMap<String, String> map = new HashMap<>();
                                        map = new Gson().fromJson(result, map.getClass());
                                        ArrayList<Text> texts = new ArrayList<>();
                                        map.forEach((s, s2) -> {
                                            var text = Texts.join(
                                                    List.of(
                                                            Text.of("["),
                                                            Text.of(s2)
                                                                    .copyContentOnly()
                                                                    .setStyle(
                                                                            Style.EMPTY
                                                                                    .withColor(Formatting.GREEN)
                                                                    ),
                                                            Text.of("]")
                                                    ),
                                                    Text.of("")
                                            ).copy().setStyle(Style.EMPTY
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
                                            ).copy();
                                            texts.add(text);
                                        });
                                        context.getSource().sendFeedback(() -> Text.of("-------Announcements-------"), false);
                                        //context.getSource().sendFeedback(Text.of(""), false);
                                        for (Text text : texts) {
                                            context.getSource().sendFeedback(() -> text, false);
                                        }
                                        //context.getSource().sendFeedback(Text.of(""), false);
                                        return 0;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    return 0;
                                })
                        )
        );
    }
}
