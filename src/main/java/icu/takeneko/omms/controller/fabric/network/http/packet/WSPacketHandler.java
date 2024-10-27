package icu.takeneko.omms.controller.fabric.network.http.packet;

public interface WSPacketHandler {
    void onConnect(int version);

    void onDisconnect();

    void onCommand(String line);

    void onCompletionRequest(String requestId,String partialCommand, int cursorPos);
}
