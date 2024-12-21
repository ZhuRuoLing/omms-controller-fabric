package icu.takeneko.omms.controller.fabric.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import icu.takeneko.omms.controller.fabric.util.logging.MemoryAppender;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import icu.takeneko.omms.controller.fabric.config.Config;
import icu.takeneko.omms.controller.fabric.config.SharedVariable;
import icu.takeneko.omms.controller.fabric.network.Broadcast;
import icu.takeneko.omms.controller.fabric.network.Status;
import icu.takeneko.omms.controller.fabric.network.UdpBroadcastSender;
import org.apache.logging.log4j.LogManager;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

public class Util {
    public static final Component LEFT_BRACKET = Component.literal("[");
    public static final Component RIGHT_BRACKET = Component.literal("]");
    public static final Component SPACE = Component.literal(" ");
    public static final ResourceLocation AUTH_PACKET_CHANNEL = new ResourceLocation("omms_auth","auth");

    public static final int PACKET_ID = kotlin.random.Random.Default.nextInt();

    public static final UdpBroadcastSender.Target TARGET_CHAT = new UdpBroadcastSender.Target("224.114.51.4", 10086);

    public static final Gson gson = new GsonBuilder().serializeNulls().create();

    public static Pair<Integer,String> invokeHttpGetRequest(String httpUrl) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(httpUrl)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(Charset.defaultCharset()));
        return new Pair<>(response.statusCode(), response.body());
    }

    public static void invokeHttpPostRequest(String url, String content) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(content))
                .header("Content-Type", "text/plain")
                .uri(URI.create(url)).build();
        client.send(request, HttpResponse.BodyHandlers.ofString(Charset.defaultCharset()));
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
        int var1 = token ^ password;

        var1 = var1 - i - (j - k);
        return var1 == 114514;
    }

    public static Component fromServerString(String displayName, String proxyName, boolean isCurrentServer, boolean isMissingServer) {
        Style style = Style.EMPTY;
        if (isMissingServer) {
            style = style.withColor(ChatFormatting.RED);
            style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Missing server name mapping key.")));
        } else {
            if (isCurrentServer) {
                style = style.withColor(ChatFormatting.YELLOW);
                style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Current server")));
            } else {
                style = style.withColor(ChatFormatting.AQUA);
                style = style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/server %s".formatted(proxyName)));
                style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Goto server %s".formatted(displayName))));
            }
        }
        Component name = Component.literal(displayName).copy().setStyle(style);
        List<Component> texts = List.of(Util.LEFT_BRACKET, name, Util.RIGHT_BRACKET);
        return ComponentUtils.formatList(texts, Component.empty());
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


    public static Component fromBroadcast(Broadcast broadcast) {
        Style style = Style.EMPTY;

        List<Component> texts = List.of(Component.literal(broadcast.getChannel()).copy().setStyle(style.withColor(ChatFormatting.AQUA)),
                Component.literal("<").copy(),
                Component.literal(broadcast.getPlayer()).copy().setStyle(style.withColor(ChatFormatting.YELLOW).withBold(true).withObfuscated(Objects.equals(broadcast.getServer(), "OMMS CENTRAL"))),
                LEFT_BRACKET.copy(),
                Component.literal(broadcast.getServer().equals("OMMS CENTRAL") ? "SERVER" : broadcast.getServer()).copy().setStyle(style.withColor(ChatFormatting.GREEN)/*.withObfuscated(Objects.equals(broadcast.getServer(), "OMMS CENTRAL"))*/),
                Component.literal("]>").copy(),
                Component.literal(broadcast.getContent()).copy()
        );
        return ComponentUtils.formatList(texts, Component.literal(""));
    }

    public static Component fromBroadcastToQQ(Broadcast broadcast) {
        Style style = Style.EMPTY;

        List<Component> texts = List.of(Component.literal(broadcast.getChannel()).copy().setStyle(style.withColor(ChatFormatting.AQUA)),
            Component.literal("<").copy(),
            Component.literal(broadcast.getPlayer().replaceFirst("\ufff3\ufff4", "")).copy().setStyle(style.withColor(ChatFormatting.YELLOW).withBold(true).withObfuscated(Objects.equals(broadcast.getServer(), "OMMS CENTRAL"))),
                LEFT_BRACKET.copy(),
            Component.literal(broadcast.getServer() + " -> QQ").copy().setStyle(style.withColor(ChatFormatting.GREEN)),
            Component.literal("]>").copy(),
            Component.literal(broadcast.getContent()).copy()
        );
        return ComponentUtils.formatList(texts, Component.literal(""));
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
                "fabric",
                server.getPlayerCount(),
                server.getMaxPlayers(),
                Arrays.asList(server.getPlayerNames())
        );
        try {
            invokeHttpPostRequest("http://%s:%d/controller/status/upload".formatted(Config.INSTANCE.getHttpQueryAddress(), Config.INSTANCE.getHttpQueryPort()), new GsonBuilder().serializeNulls().create().toJson(status));
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    @Deprecated
    public static void submitCommandLog(String cmd, String out) {
        try {
            invokeHttpPostRequest("http://%s:%d/controller/command/upload".formatted(Config.INSTANCE.getHttpQueryAddress(), Config.INSTANCE.getHttpQueryPort()),
                    gson.toJson(new CommandOutputData(Config.INSTANCE.getControllerName(), cmd, out)));
        } catch (Exception e) {
            LogUtils.getLogger().error("Error occurred while updating command log.", e);
        }
    }

    public static void submitCrashReport(String content) {
        submitToExecutor(() -> {
            try {
                CrashReportUploader.upload(content);
            } catch (Exception e) {
                LogUtils.getLogger().error("Error occurred while updating command log.", e);
            }
        });
    }

    public static Broadcast toPlayerConnectionStateBroadcast(String playerName, Component stateReason) {
        return new Broadcast(Config.INSTANCE.getChatChannel(),
                Config.INSTANCE.getControllerName(),
                playerName,
                " *"+stateReason.getString()+ "*",
                Util.randomStringGen(16)
        );
    }

}
