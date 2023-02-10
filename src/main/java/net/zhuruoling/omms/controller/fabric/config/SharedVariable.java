package net.zhuruoling.omms.controller.fabric.config;

import net.zhuruoling.omms.controller.fabric.network.UdpBroadcastSender;
import net.zhuruoling.omms.controller.fabric.network.UdpReceiver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SharedVariable {
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);
    public static boolean shouldCrash = false;
    public static boolean ready = false;
    private static UdpBroadcastSender sender;
    private static UdpReceiver chatReceiver;
    private static UdpReceiver instructionReceiver;

    public static UdpReceiver getInstructionReceiver() {
        return instructionReceiver;
    }

    public static void setInstructionReceiver(UdpReceiver instructionReceiver) {
        SharedVariable.instructionReceiver = instructionReceiver;
    }

    public static UdpBroadcastSender getSender() {
        return sender;
    }

    public static void setSender(UdpBroadcastSender sender) {
        SharedVariable.sender = sender;
    }

    public static UdpReceiver getChatReceiver() {
        return chatReceiver;
    }

    public static void setChatReceiver(UdpReceiver receiver) {
        SharedVariable.chatReceiver = receiver;
    }

    public static ExecutorService getExecutorService() {
        return executorService;
    }

}
