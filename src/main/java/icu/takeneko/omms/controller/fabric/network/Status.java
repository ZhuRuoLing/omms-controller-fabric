package icu.takeneko.omms.controller.fabric.network;

import java.util.List;

public class Status {
    String name;
    String type;
    int playerCount;
    int maxPlayerCount;
    List<String> players;

    public Status(String name, String type, int playerCount, int maxPlayerCount, List<String> players) {
        this.name = name;
        this.type = type;
        this.playerCount = playerCount;
        this.maxPlayerCount = maxPlayerCount;
        this.players = players;
    }

}
