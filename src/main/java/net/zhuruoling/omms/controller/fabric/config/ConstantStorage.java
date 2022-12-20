package net.zhuruoling.omms.controller.fabric.config;

import net.zhuruoling.omms.controller.fabric.network.UdpBroadcastSender;
import net.zhuruoling.omms.controller.fabric.network.UdpReceiver;

import java.util.HashMap;

public class ConstantStorage {
    private static boolean enableWhitelist = false;
    private static boolean enableJoinmotd = false;
    private static boolean enableChatBridge = false;
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
        return "#OMMS config\n" +
                "enableWhitelist=false\n" +
                "enableJoinmotd=false\n" +
                "enableChatBridge=false\n" +
                "httpQueryAddr=localhost\n" +
                "httpQueryPort=50001\n" +
                "controllerName=omms-controller\n" +
                "usesWhitelist=my_whitelist\n" +
                "channel=GLOBAL\n" +
                "serverMappings";
    }

    public static void init() {
        SimpleConfig config = SimpleConfig.of("omms").provider(ConstantStorage::provider).request();
        setEnableWhitelist(config.getOrDefault("enableWhitelist", false));
        setEnableChatBridge(config.getOrDefault("enableChatBridge", false));
        setEnableJoinmotd(config.getOrDefault("enableJoinmotd", false));
        setChatChannel(config.getOrDefault("channel", "GLOBAL"));
        setControllerName(config.getOrDefault("controllerName", "omms-controller"));
        setHttpQueryAddress(config.getOrDefault("httpQueryAddr", "localhost"));
        setHttpQueryPort(config.getOrDefault("httpQueryPort", 50001));
        setWhitelistName(config.getOrDefault("usesWhitelist", "my_whitelist"));
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
                String displayName = config.getOrDefault(String.format("serverMapping.%s.displayName",name), "");
                String proxyName = config.getOrDefault(String.format("serverMapping.%s.proxyName",name), "");
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
            String displayName = config.getOrDefault(String.format("serverMapping.%s.displayName",serverMappingNames), "");
            String proxyName = config.getOrDefault(String.format("serverMapping.%s.proxyName",serverMappingNames), "");
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

    public static boolean isEnableJoinmotd() {
        return enableJoinmotd;
    }

    public static void setEnableJoinmotd(boolean enableJoinmotd) {
        ConstantStorage.enableJoinmotd = enableJoinmotd;
    }

    public static boolean isEnableChatBridge() {
        return enableChatBridge;
    }

    public static void setEnableChatBridge(boolean enableChatBridge) {
        ConstantStorage.enableChatBridge = enableChatBridge;
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
