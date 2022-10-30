package net.zhuruoling.omms.controller.fabric.network;

import java.util.List;

public class Status {
    String name;
    ControllerTypes type;
    int playerCount;
    int maxPlayerCount;
    List<String> players;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ControllerTypes getType() {
        return type;
    }

    public void setType(ControllerTypes type) {
        this.type = type;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public int getMaxPlayerCount() {
        return maxPlayerCount;
    }

    public void setMaxPlayerCount(int maxPlayerCount) {
        this.maxPlayerCount = maxPlayerCount;
    }

    public List<String> getPlayers() {
        return players;
    }

    public void setPlayers(List<String> players) {
        this.players = players;
    }

    public Status(String name, ControllerTypes type, int playerCount, int maxPlayerCount, List<String> players) {
        this.name = name;
        this.type = type;
        this.playerCount = playerCount;
        this.maxPlayerCount = maxPlayerCount;
        this.players = players;
    }

    public Status(ControllerTypes type, int playerCount, int maxPlayerCount, List<String> players) {
        this.type = type;
        this.playerCount = playerCount;
        this.maxPlayerCount = maxPlayerCount;
        this.players = players;
    }
}
