package net.zhuruoling.omms.controller.fabric.config;

import net.zhuruoling.omms.controller.fabric.network.UdpBroadcastSender;
import net.zhuruoling.omms.controller.fabric.network.UdpReceiver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RuntimeConstants {
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private static UdpBroadcastSender sender;
    private static UdpReceiver chatReceiver;
    private static UdpReceiver instructionReceiver;

    public static UdpReceiver getInstructionReceiver() {
        return instructionReceiver;
    }

    public static void setInstructionReceiver(UdpReceiver instructionReceiver) {
        RuntimeConstants.instructionReceiver = instructionReceiver;
    }

    public static UdpBroadcastSender getSender() {
        return sender;
    }

    public static void setSender(UdpBroadcastSender sender) {
        RuntimeConstants.sender = sender;
    }

    public static UdpReceiver getChatReceiver() {
        return chatReceiver;
    }

    public static void setChatReceiver(UdpReceiver receiver) {
        RuntimeConstants.chatReceiver = receiver;
    }

    public static ExecutorService getExecutorService() {
        return executorService;
    }




}
