package icu.takeneko.omms.controller.fabric.network.http.ws;

public interface WSPacketHandler {
    void onConnect();

    void onDisconnect();

    void onCommand(String line);
}
