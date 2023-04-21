package net.zhuruoling.omms.controller.fabric.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.zhuruoling.omms.controller.fabric.announcement.Announcement;
import net.zhuruoling.omms.controller.fabric.config.Config;
import net.zhuruoling.omms.controller.fabric.config.SharedVariable;
import net.zhuruoling.omms.controller.fabric.network.Broadcast;
import net.zhuruoling.omms.controller.fabric.network.ControllerTypes;
import net.zhuruoling.omms.controller.fabric.network.Status;
import net.zhuruoling.omms.controller.fabric.network.UdpBroadcastSender;
import net.zhuruoling.omms.controller.fabric.util.logging.MemoryAppender;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
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

    public static Pair<Integer,String> invokeHttpGetRequest(String httpUrl) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(httpUrl)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(Charset.defaultCharset()));
        return new Pair<>(response.statusCode(), response.body());
    }

    public static int invokeHttpPostRequest(String url, String content) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(content))
                .header("Content-Type", "text/plain")
                .uri(URI.create(url)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(Charset.defaultCharset()));
        return response.statusCode();
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

    public static List<Text> fromAnnouncement(Announcement announcement) {
        Style style = Style.EMPTY;
        String pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.FULL, FormatStyle.FULL, Chronology.ofLocale(Locale.getDefault()), Locale.getDefault());
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        Text timeText = Text.of("Published at %s\n".formatted(format.format(new Date(announcement.getTimeMillis()))))
                .copy().setStyle(style.withColor(TextColor.fromFormatting(Formatting.LIGHT_PURPLE)).withBold(true));
        Text titleText = Text.of("Title: "+announcement.getTitle()).copy().setStyle(style.withColor(Formatting.GREEN).withBold(true));
        ArrayList<Text> textArrayList = new ArrayList<>();
        textArrayList.add(Text.empty());
        textArrayList.add(titleText);
        textArrayList.add(timeText);
        for (String s : announcement.getContent()) {
            textArrayList.add(Text.of(s));
        }
        textArrayList.add(Text.empty());
        return textArrayList;
    }

    public static void addAppender() {
        var rootLogger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        var config = rootLogger.get();
        var appender = MemoryAppender.newAppender("OMMSMemoryLogger");
        appender.start();
        config.addAppender(appender, null, null);
        rootLogger.addAppender(appender);
        config.start();
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
        switch (Config.INSTANCE.getChatbridgeImplementation()){
            case UDP -> SharedVariable.getSender().addToQueue(Util.TARGET_CHAT, data);
            case WS -> SharedVariable.getWebsocketChatClient().addToCache(broadcast);
            case DISABLED -> {
                //do  nothing
            }
        }
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

    public static int sendAnnouncementFromUrlToPlayer(CommandContext<ServerCommandSource> context, String url) {
        try {
            var value = invokeHttpGetRequest(url);
            if (value.getLeft() != 200){
                context.getSource().sendError(Text.of("Cannot communicate with OMMS Central Server."));
                return -1;
            }
            String result = value.getRight();
            if (result != null) {
                if (result.equals("NO_ANNOUNCEMENT")) {
                    Text text = Texts.join(Text.of("No announcement.").copyContentOnly().getWithStyle(Style.EMPTY.withColor(Formatting.AQUA)), Text.empty());
                    context.getSource().sendFeedback(text, false);
                    return 0;
                }
                try {
                    var announcement = gson.fromJson(result, Announcement.class);
                    fromAnnouncement(announcement).forEach(text -> context.getSource().sendFeedback(text, false));
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
                " *"+stateReason.getString()+ "*",
                Util.randomStringGen(16)
        );
    }

}
