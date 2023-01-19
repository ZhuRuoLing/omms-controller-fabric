package net.zhuruoling.omms.controller.fabric.config;

import net.zhuruoling.omms.controller.fabric.network.UdpBroadcastSender;
import net.zhuruoling.omms.controller.fabric.network.UdpReceiver;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ConstantStorage {
    private static boolean enableWhitelist = false;

    private static boolean enableChatBridge=false;
    private static boolean enableJoinMotd=false;
    private static boolean enableRemoteControl=false;
    private static String httpQueryAddress;
    private static String oldId;
    private static int httpQueryPort;
    private static String controllerName;
    private static String whitelistName;
    private static String chatChannel;
    private static HashMap<String, ServerMapping> serverMappings;
    private static UdpBroadcastSender sender;
    private static UdpReceiver chatReceiver;
    private static UdpReceiver instructionReceiver;
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);

    private static String allowedFakePlayerPrefix = "";
    private static  String allowedFakePlayerSuffix = "";

    public static String getOldId() {
        return oldId;
    }

    public static void setOldId(String oldId) {
        ConstantStorage.oldId = oldId;
    }

    public static UdpReceiver getInstructionReceiver() {
        return instructionReceiver;
    }

    public static void setInstructionReceiver(UdpReceiver instructionReceiver) {
        ConstantStorage.instructionReceiver = instructionReceiver;
    }

    public static UdpBroadcastSender getSender() {
        return sender;
    }

    public static void setSender(UdpBroadcastSender sender) {
        ConstantStorage.sender = sender;
    }

    public static UdpReceiver getChatReceiver() {
        return chatReceiver;
    }

    public static void setChatReceiver(UdpReceiver receiver) {
        ConstantStorage.chatReceiver = receiver;
    }

    private static String provider(String filename) {
        return """
                #OMMS config
                enableWhitelist=false
                enableChatBridge=false
                enableJoinMotd=false
                enableRemoteControl=false
                httpQueryAddr=localhost
                httpQueryPort=50001
                controllerName=omms-controller
                usesWhitelist=my_whitelist
                channel=GLOBAL
                allowedFakePlayerPrefix=bot_
                allowedFakePlayerSuffix=_bot
                serverMappings""";
    }

    public static void init() {
        SimpleConfig config = SimpleConfig.of("omms").provider(ConstantStorage::provider).request();
        setEnableWhitelist(config.getOrDefault("enableWhitelist", false));
        setEnableChatBridge(config.getOrDefault("enableChatBridge", false));
        setEnableJoinMotd(config.getOrDefault("enableJoinMotd", false));
        setEnableRemoteControl(config.getOrDefault("enableRemoteControl", false));
        setChatChannel(config.getOrDefault("channel", "GLOBAL"));
        setControllerName(config.getOrDefault("controllerName", "omms-controller"));
        setHttpQueryAddress(config.getOrDefault("httpQueryAddr", "localhost"));
        setHttpQueryPort(config.getOrDefault("httpQueryPort", 50001));
        setWhitelistName(config.getOrDefault("usesWhitelist", "my_whitelist"));
        setAllowedFakePlayerPrefix(config.getOrDefault("allowedFakePlayerPrefix", "bot_"));
        setAllowedFakePlayerSuffix(config.getOrDefault("allowedFakePlayerSuffix", "_bot"));
        String serverMappingNames = config.getOrDefault("serverMappings", "");
        if (serverMappingNames.contains(",")) {
            if (serverMappingNames.isBlank()) {
                setServerMappings(null);
                return;
            }
            HashMap<String, ServerMapping> map = new HashMap<>();
            for (String name : serverMappingNames.split(",")) {
                if (name.isBlank()) {
                    continue;
                }
                ServerMapping mapping = new ServerMapping();
                mapping.setWhitelistName(serverMappingNames);
                String displayName = config.getOrDefault("serverMapping.%s.displayName".formatted(name), "");
                String proxyName = config.getOrDefault("serverMapping.%s.proxyName".formatted(name), "");
                if (displayName.isBlank() || proxyName.isBlank()) {
                    setServerMappings(null);
                    continue;
                }
                mapping.setDisplayName(displayName);
                mapping.setProxyName(proxyName);
                map.put(name, mapping);
            }
            setServerMappings(map);
        } else {
            ServerMapping mapping = new ServerMapping();
            mapping.setWhitelistName(serverMappingNames);
            String displayName = config.getOrDefault("serverMapping.%s.displayName".formatted(serverMappingNames), "");
            String proxyName = config.getOrDefault("serverMapping.%s.proxyName".formatted(serverMappingNames), "");
            if (displayName.isBlank() || proxyName.isBlank()) {
                setServerMappings(null);
                return;
            }
            mapping.setDisplayName(displayName);
            mapping.setProxyName(proxyName);
            HashMap<String, ServerMapping> hashMap = new HashMap<>();
            hashMap.put(serverMappingNames, mapping);
            setServerMappings(hashMap);
        }
    }

    public static boolean isEnableChatBridge() {
        return enableChatBridge;
    }

    private static void setEnableChatBridge(boolean enableChatBridge) {
        ConstantStorage.enableChatBridge = enableChatBridge;
    }

    public static boolean isEnableJoinMotd() {
        return enableJoinMotd;
    }

    private static void setEnableJoinMotd(boolean enableJoinMotd) {
        ConstantStorage.enableJoinMotd = enableJoinMotd;
    }

    public static boolean isEnableRemoteControl() {
        return enableRemoteControl;
    }

    private static void setEnableRemoteControl(boolean enableRemoteControl) {
        ConstantStorage.enableRemoteControl = enableRemoteControl;
    }

    public static String getAllowedFakePlayerPrefix() {
        return allowedFakePlayerPrefix;
    }

    private static void setAllowedFakePlayerPrefix(String allowedFakePlayerPrefix) {
        ConstantStorage.allowedFakePlayerPrefix = allowedFakePlayerPrefix;
    }

    public static String getAllowedFakePlayerSuffix() {
        return allowedFakePlayerSuffix;
    }

    private static void setAllowedFakePlayerSuffix(String allowedFakePlayerSuffix) {
        ConstantStorage.allowedFakePlayerSuffix = allowedFakePlayerSuffix;
    }

    public static boolean isEnableWhitelist() {
        return enableWhitelist;
    }

    public static void setEnableWhitelist(boolean enableWhitelist) {
        ConstantStorage.enableWhitelist = enableWhitelist;
    }

    public static String getHttpQueryAddress() {
        return httpQueryAddress;
    }

    public static void setHttpQueryAddress(String httpQueryAddress) {
        ConstantStorage.httpQueryAddress = httpQueryAddress;
    }

    public static int getHttpQueryPort() {
        return httpQueryPort;
    }

    public static void setHttpQueryPort(int httpQueryPort) {
        ConstantStorage.httpQueryPort = httpQueryPort;
    }

    public static String getControllerName() {
        return controllerName;
    }

    public static void setControllerName(String controllerName) {
        ConstantStorage.controllerName = controllerName;
    }

    public static String getWhitelistName() {
        return whitelistName;
    }

    public static void setWhitelistName(String whitelistName) {
        ConstantStorage.whitelistName = whitelistName;
    }


    public static String getChatChannel() {
        return chatChannel;
    }

    public static void setChatChannel(String chatChannel) {
        ConstantStorage.chatChannel = chatChannel;
    }

    public static HashMap<String, ServerMapping> getServerMappings() {
        return serverMappings;
    }

    public static void setServerMappings(HashMap<String, ServerMapping> serverMappings) {
        ConstantStorage.serverMappings = serverMappings;
    }

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    public static class ServerMapping {
        private String displayName;
        private String proxyName;
        private String whitelistName;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getProxyName() {
            return proxyName;
        }

        public void setProxyName(String proxyName) {
            this.proxyName = proxyName;
        }

        public String getWhitelistName() {
            return whitelistName;
        }

        public void setWhitelistName(String whitelistName) {
            this.whitelistName = whitelistName;
        }

        @Override
        public String toString() {
            return "ServerMapping{" +
                    "displayName='" + displayName + '\'' +
                    ", proxyName='" + proxyName + '\'' +
                    ", whitelistName='" + whitelistName + '\'' +
                    '}';
        }
    }

}
