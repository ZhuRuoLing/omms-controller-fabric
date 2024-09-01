package icu.takeneko.omms.controller.fabric.network;

import icu.takeneko.omms.controller.fabric.util.Util;
import icu.takeneko.omms.controller.fabric.config.Config;

public class Broadcast {
    private String id;
    private String channel;
    private String server;
    private String player;
    private String content;
    private long timeMillis;

    public Broadcast(String player, String content) {
        this.server = Config.INSTANCE.getControllerName();
        this.channel = Config.INSTANCE.getChatChannel();
        this.player = player;
        this.content = content;
        this.id = Util.randomStringGen(16);
    }

    public Broadcast(String channel, String server, String player, String content, String id) {
        this.channel = channel;
        this.server = server;
        this.player = player;
        this.content = content;
        this.id = id;
    }

    public String getChannel() {
        return channel;
    }

    public String getServer() {
        return server;
    }

    public String getPlayer() {
        return player;
    }

    public String getContent() {
        return content;
    }
}