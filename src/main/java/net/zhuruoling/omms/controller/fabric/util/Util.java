package net.zhuruoling.omms.controller.fabric.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatType;
import net.minecraft.stat.Stats;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.zhuruoling.omms.controller.fabric.announcement.Announcement;
import net.zhuruoling.omms.controller.fabric.config.Config;
import net.zhuruoling.omms.controller.fabric.config.SharedVariable;
import net.zhuruoling.omms.controller.fabric.network.Broadcast;
import net.zhuruoling.omms.controller.fabric.network.ControllerTypes;
import net.zhuruoling.omms.controller.fabric.network.Status;
import net.zhuruoling.omms.controller.fabric.network.UdpBroadcastSender;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.*;

public class Util {
    public static final Text LEFT_BRACKET = Text.of("[");
    public static final Text RIGHT_BRACKET = Text.of("]");
    public static final Text SPACE = Text.of(" ");

    public static final UdpBroadcastSender.Target TARGET_CHAT = new UdpBroadcastSender.Target("224.114.51.4", 10086);
    public static final UdpBroadcastSender.Target TARGET_CONTROL = new UdpBroadcastSender.Target("224.114.51.4", 10087);

    public static final Gson gson = new GsonBuilder().serializeNulls().create();

    public static String invokeHttpGetRequest(String httpUrl) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(httpUrl)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(Charset.defaultCharset()));
        return response.body();
    }

    public static void invokeHttpPostRequest(String url, String content) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(content))
                .header("Content-Type", "text/plain")
                .uri(URI.create(url)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(Charset.defaultCharset()));
        System.out.println(response.statusCode());
    }


    public static int calculateTokenByDate(int password) {
        Date date = new Date();
        int i = Integer.parseInt(new SimpleDateFormat("yyyyMMdd").format(date));
        int j = Integer.parseInt(new SimpleDateFormat("hhmm").format(date));
        int k = new SimpleDateFormat("yyyyMMddhhmm").format(date).hashCode();
        return calculateToken(password, i, j, k);
    }

    public static boolean resloveTokenByDate(int token, int password) {
        Date date = new Date();
        int i = Integer.parseInt(new SimpleDateFormat("yyyyMMdd").format(date));
        int j = Integer.parseInt(new SimpleDateFormat("hhmm").format(date));
        int k = new SimpleDateFormat("yyyyMMddhhmm").format(date).hashCode();
        return resolveToken(token, password, i, j, k);
    }


    public static int calculateToken(int password, int i, int j, int k) {
        int token = 114514;
        token += i;
        token += (j - k);
        token = password ^ token;
        return token;
    }

    public static boolean resolveToken(int token, int password, int i, int j, int k) {
        int t = token;
        int var1 = t ^ password;

        var1 = var1 - i - (j - k);
        return var1 == 114514;
    }

    public static Text fromServerString(String displayName, String proxyName, boolean isCurrentServer, boolean isMissingServer) {
        Style style = Style.EMPTY;
        if (isMissingServer) {
            style = style.withColor(TextColor.fromFormatting(Formatting.RED));
            style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Missing server name mapping key.")));
        } else {
            if (isCurrentServer) {
                style = style.withColor(TextColor.fromFormatting(Formatting.YELLOW));
                style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Current server")));
            } else {
                style = style.withColor(Formatting.AQUA);
                style = style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/server %s".formatted(proxyName)));
                style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Goto server %s".formatted(displayName))));
            }
        }
        Text name = Text.of(displayName).copyContentOnly().setStyle(style);
        List<Text> texts = List.of(Util.LEFT_BRACKET, name, Util.RIGHT_BRACKET);
        return Texts.join(texts, Text.empty());
    }

    public static Text fromAnnouncement(Announcement announcement) {
        Style style = Style.EMPTY;
        String pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.FULL, FormatStyle.FULL, Chronology.ofLocale(Locale.getDefault()), Locale.getDefault());
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        Text timeText = Text.of("Published at %s\n".formatted(format.format(new Date(announcement.getTimeMillis()))))
                .copyContentOnly().setStyle(style.withColor(TextColor.fromFormatting(Formatting.LIGHT_PURPLE)).withBold(true));
        StringBuilder builder = new StringBuilder();
        for (String s : announcement.getContent()) {
            builder.append(s);
            builder.append("\n");
        }
        Text contentText = Text.of(builder.toString()).copyContentOnly();
        Text titleText = Text.of(announcement.getTitle() + "\n").copyContentOnly().setStyle(style.withColor(Formatting.GREEN).withBold(true));

        return Texts.join(List.of(titleText, timeText, contentText), Text.empty());
    }


    public static Text fromBroadcast(Broadcast broadcast) {
        Style style = Style.EMPTY;

        List<Text> texts = List.of(Text.of(broadcast.getChannel()).copyContentOnly().setStyle(style.withColor(Formatting.AQUA)),
                Text.of("<").copyContentOnly(),
                Text.of(broadcast.getPlayer()).copyContentOnly().setStyle(style.withColor(Formatting.YELLOW).withBold(true).withObfuscated(Objects.equals(broadcast.getServer(), "OMMS CENTRAL"))),
                LEFT_BRACKET.copyContentOnly(),
                Text.of(broadcast.getServer()).copyContentOnly().setStyle(style.withColor(Formatting.GREEN)),
                Text.of("]>").copyContentOnly(),
                Text.of(broadcast.getContent()).copyContentOnly()
        );
        return Texts.join(texts, Text.of(""));
    }

    public static Text fromBroadcastToQQ(Broadcast broadcast) {
        Style style = Style.EMPTY;

        List<Text> texts = List.of(Text.of(broadcast.getChannel()).copyContentOnly().setStyle(style.withColor(Formatting.AQUA)),
                Text.of("<").copyContentOnly(),
                Text.of(broadcast.getPlayer().replaceFirst("\ufff3\ufff4", "")).copyContentOnly().setStyle(style.withColor(Formatting.YELLOW).withBold(true).withObfuscated(Objects.equals(broadcast.getServer(), "OMMS CENTRAL"))),
                LEFT_BRACKET.copyContentOnly(),
                Text.of(broadcast.getServer() + " -> QQ").copyContentOnly().setStyle(style.withColor(Formatting.GREEN)),
                Text.of("]>").copyContentOnly(),
                Text.of(broadcast.getContent()).copyContentOnly()
        );
        return Texts.join(texts, Text.of(""));
    }


    public static void sendChatBroadcast(String text, String playerName) {
        Broadcast broadcast = new Broadcast(playerName, text);
        Gson gson = new GsonBuilder().serializeNulls().create();
        String data = gson.toJson(broadcast, Broadcast.class);
        SharedVariable.getSender().addToQueue(Util.TARGET_CHAT, data);
    }


    public static String randomStringGen(int len) {
        String ch = "abcdefghijklmnopqrstuvwxyzABCDEFGHIGKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder stringBuffer = new StringBuilder();
        for (int i = 0; i < len; i++) {
            Random random = new Random(System.nanoTime());
            int num = random.nextInt(62);
            stringBuffer.append(ch.charAt(num));
        }
        return stringBuffer.toString();
    }

    public static void sendStatus(MinecraftServer server) {
        var status = new Status(
                Config.INSTANCE.getControllerName(),
                ControllerTypes.FABRIC,
                server.getCurrentPlayerCount(),
                server.getMaxPlayerCount(),
                Arrays.asList(server.getPlayerNames())
        );
        try {
            invokeHttpPostRequest("http://%s:%d/controller/status/upload".formatted(Config.INSTANCE.getHttpQueryAddress(), Config.INSTANCE.getHttpQueryPort()), new GsonBuilder().serializeNulls().create().toJson(status));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getAnnouncementToPlayerFromUrl(CommandContext<ServerCommandSource> context, String url) {
        try {
            String result = invokeHttpGetRequest(url);
            if (result != null) {
                if (result.equals("NO_ANNOUNCEMENT")) {
                    Text text = Texts.join(Text.of("No announcement.").copyContentOnly().getWithStyle(Style.EMPTY.withColor(Formatting.AQUA)), Text.empty());
                    context.getSource().sendFeedback(text, false);
                    return 0;
                }
                System.out.println(result);
                try {
                    String jsonStr = new String(Base64.getDecoder().decode(result.replace("\"", "")), StandardCharsets.UTF_8);
                    Gson gson = new GsonBuilder().serializeNulls().create();
                    var announcement = gson.fromJson(jsonStr, Announcement.class);
                    context.getSource().sendFeedback(fromAnnouncement(announcement), false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Text text = Texts.join(Text.of("No announcement.").copyContentOnly().getWithStyle(Style.EMPTY.withColor(Formatting.AQUA)), Text.empty());
                context.getSource().sendFeedback(text, false);
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void submitToExecutor(Runnable runnable) {
        synchronized (SharedVariable.getExecutorService()) {
            if (SharedVariable.getExecutorService().isShutdown()) {
                LogUtils.getLogger().error("Executor service already stopped!");
                return;
            }
            SharedVariable.getExecutorService().submit(runnable);
        }
    }

    public static void submitCommandLog(String cmd, String out) {
        try {
            invokeHttpPostRequest("http://%s:%d/controller/command/upload".formatted(Config.INSTANCE.getHttpQueryAddress(), Config.INSTANCE.getHttpQueryPort()),
                    gson.toJson(new CommandOutputData(Config.INSTANCE.getControllerName(), cmd, out)));
        } catch (Exception e) {
            LogUtils.getLogger().error("Error occurred while updating command log.", e);
        }
    }

    public static void submitCrashReportUsingExecutor(String content) {
        submitToExecutor(() -> {
            try {
                invokeHttpPostRequest("http://%s:%d/controller/crashReport/upload".formatted(Config.INSTANCE.getHttpQueryAddress(), Config.INSTANCE.getHttpQueryPort()),
                        content);
            } catch (Exception e) {
                LogUtils.getLogger().error("Error occurred while updating command log.", e);
            }
        });
    }

    public static Broadcast toPlayerConnectionStateBroadcast(String playerName, Text stateReason) {
        return new Broadcast(Config.INSTANCE.getChatChannel(),
                Config.INSTANCE.getControllerName(),
                playerName,
                stateReason.getString(),
                Util.randomStringGen(16)
        );
    }

}
