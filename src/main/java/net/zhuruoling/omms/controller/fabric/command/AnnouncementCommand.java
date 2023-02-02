package net.zhuruoling.omms.controller.fabric.command;

import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.zhuruoling.omms.controller.fabric.config.Config;
import net.zhuruoling.omms.controller.fabric.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class AnnouncementCommand extends Command<ServerCommandSource>{
    @Override
    public void register(CommandDispatcher<ServerCommandSource> commandDispatcher) {
        commandDispatcher.register(
                LiteralArgumentBuilder.<ServerCommandSource>literal("announcement")
                        .then(LiteralArgumentBuilder.<ServerCommandSource>literal("latest")
                                .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0))
                                .executes(context -> {
                                    String url = "http://%s:%d/announcement/latest".formatted(Config.INSTANCE.getHttpQueryAddress(), Config.INSTANCE.getHttpQueryPort());
                                    return Util.getAnnouncementToPlayerFromUrl(context, url);
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
                                                    return Util.getAnnouncementToPlayerFromUrl(context, url);
                                                })
                                )
                        )
                        .then(LiteralArgumentBuilder.<ServerCommandSource>literal("list")
                                .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(0)).executes(context -> {
                                    String url = "http://%s:%d/announcement/list".formatted(Config.INSTANCE.getHttpQueryAddress(), Config.INSTANCE.getHttpQueryPort());
                                    try {
                                        String result = Objects.requireNonNull(Util.invokeHttpGetRequest(url));
                                        String[] list = new Gson().fromJson(result, String[].class);
                                        ArrayList<Text> texts = new ArrayList<>();
                                        for (String s : list) {
                                            System.out.println(s);
                                            var text = Texts.join(
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
                                                    );
                                            System.out.println(text);

                                        }
                                        System.out.println(texts);
                                        context.getSource().sendFeedback(Text.of("-------Announcements-------"), false);
                                        context.getSource().sendFeedback(Text.of(""), false);
                                        context.getSource().sendFeedback(Texts.join(texts, Text.of(" ")), false);
                                        context.getSource().sendFeedback(Text.of(""), false);
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
