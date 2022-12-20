package net.zhuruoling.omms.controller.fabric.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.realms.util.JsonUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.zhuruoling.omms.controller.fabric.announcement.Announcement;
import net.zhuruoling.omms.controller.fabric.config.ConstantStorage;
import net.zhuruoling.omms.controller.fabric.network.Broadcast;
import net.zhuruoling.omms.controller.fabric.network.ControllerTypes;
import net.zhuruoling.omms.controller.fabric.network.Status;
import net.zhuruoling.omms.controller.fabric.network.UdpBroadcastSender;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.*;

public class Util {
    public static final Text LEFT_BRACKET = new LiteralText("[");
    public static final Text RIGHT_BRACKET = new LiteralText("]");
    public static final Text SPACE = new LiteralText(" ");

    public static final UdpBroadcastSender.Target TARGET_CHAT = new UdpBroadcastSender.Target("224.114.51.4", 10086);
    public static final UdpBroadcastSender.Target TARGET_CONTROL = new UdpBroadcastSender.Target("224.114.51.4", 10087);

    public static final Gson gson = new GsonBuilder().serializeNulls().create();

    public static String invokeHttpGetRequest(String httpUrl) {
        HttpURLConnection connection = null;
        InputStream is = null;
        BufferedReader br = null;
        StringBuilder result = new StringBuilder();
        try {
            URL url = new URL(httpUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(500);
            connection.connect();
            if (connection.getResponseCode() == 200) {
                is = connection.getInputStream();
                if (null != is) {
                    br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    String temp;
                    while (null != (temp = br.readLine())) {
                        result.append(temp);
                    }
                }
            }
            else {
                connection.disconnect();
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != br) {
                try {
                    br.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (null != is) {
                try {
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result.toString();
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
        return resloveToken(token, password, i, j, k);
    }


    public static int calculateToken(int password, int i, int j, int k) {
        int token = 114514;
        token += i;
        token += (j - k);
        token = password ^ token;
        return token;
    }

    public static boolean resloveToken(int token, int password, int i, int j, int k) {
        int t = token;
        int var1 = t ^ password;

        var1 = var1 - i - (j - k);
        return var1 == 114514;
    }

    public static Text fromServerString(String displayName, String proxyName, boolean isCurrentServer, boolean isMissingServer) {
        Style style = Style.EMPTY;
        if (isMissingServer) {
            style = style.withColor(TextColor.fromFormatting(Formatting.RED));
            style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Missing server name mapping key.")));
        } else {
            if (isCurrentServer) {
                style = style.withColor(TextColor.fromFormatting(Formatting.YELLOW));
                style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Current server")));
            } else {
                style = style.withColor(Formatting.AQUA);
                style = style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/server %s",proxyName)));
                style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(String.format("Goto server %s",displayName))));
            }
        }
        Text name = new LiteralText(displayName).copy().setStyle(style);
        List<Text> texts = List.of(Util.LEFT_BRACKET, name, Util.RIGHT_BRACKET);
        return Texts.join(texts, text -> text);
    }

    public static Text fromAnnouncement(Announcement announcement){
        Style style = Style.EMPTY;
        String pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.FULL, FormatStyle.FULL, Chronology.ofLocale(Locale.getDefault()), Locale.getDefault());
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        Text timeText = new LiteralText(String.format("Published at %s\n",format.format(new Date(announcement.getTimeMillis()))))
                .copy().setStyle(style.withColor(TextColor.fromFormatting(Formatting.LIGHT_PURPLE)).withBold(true));
        StringBuilder builder = new StringBuilder();
        for (String s : announcement.getContent()) {
            builder.append(s);
            builder.append("\n");
        }
        Text contentText = new LiteralText(builder.toString()).copy();
        Text titleText = new LiteralText(announcement.getTitle() + "\n").copy().setStyle(style.withColor(Formatting.GREEN).withBold(true));
        return Texts.join(List.of(titleText, timeText, contentText), text -> text);
    }

    public static Text fromBroadcast(Broadcast broadcast) {
        Style style = Style.EMPTY;

        List<Text> texts = List.of(new LiteralText(broadcast.getChannel()).copy().setStyle(style.withColor(Formatting.AQUA)),
                new LiteralText("<").copy(),
                new LiteralText(broadcast.getPlayer()).copy().setStyle(style.withColor(Formatting.YELLOW).withBold(true)),
                LEFT_BRACKET.copy(),
                new LiteralText(broadcast.getServer()).copy().setStyle(style.withColor(Formatting.GREEN)),
                new LiteralText("]>").copy(),
                new LiteralText(broadcast.getContent()).copy()
        );
        return Texts.join(texts, text -> text);
    }

    public static Text fromBroadcastToQQ(Broadcast broadcast) {
        Style style = Style.EMPTY;

        List<Text> texts = List.of(new LiteralText(broadcast.getChannel()).copy().setStyle(style.withColor(Formatting.AQUA)),
                new LiteralText("<").copy(),
                new LiteralText(broadcast.getPlayer().replaceFirst("\ufff3\ufff4", "")).copy().setStyle(style.withColor(Formatting.YELLOW).withBold(true)),
                LEFT_BRACKET.copy(),
                new LiteralText(broadcast.getServer() + " -> QQ").copy().setStyle(style.withColor(Formatting.GREEN)),
                new LiteralText("]>").copy(),
                new LiteralText(broadcast.getContent()).copy()
        );
        return Texts.join(texts, text -> text);
    }


    public static void sendChatBroadcast(String text, String playerName) {
        Broadcast broadcast = new Broadcast(playerName, text);
        Gson gson = new GsonBuilder().serializeNulls().create();
        String data = gson.toJson(broadcast, Broadcast.class);
        ConstantStorage.getSender().addToQueue(Util.TARGET_CHAT, data);
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
    public static void sendStatus(MinecraftServer server, UdpBroadcastSender.Target target){
        var status = new Status(
                ConstantStorage.getControllerName(),
                ControllerTypes.FABRIC,
                server.getCurrentPlayerCount(),
                server.getMaxPlayerCount(),
                Arrays.asList(server.getPlayerNames())
        );
        ConstantStorage.getSender().createMulticastSocketCache(target);
        try {
            Thread.sleep(50);
            ConstantStorage.getSender().addToQueue(target,gson.toJson(status));
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
