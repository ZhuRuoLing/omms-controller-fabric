package net.zhuruoling.omms.controller.fabric.config;

import net.zhuruoling.omms.controller.fabric.network.UdpBroadcastSender;
import net.zhuruoling.omms.controller.fabric.network.UdpReceiver;

import java.util.HashMap;

public class ConstantStorage {
    private static boolean enable = false;
    private static String httpQueryAddress;
    private static int httpQueryPort;
    private static String controllerName;
    private static String whitelistName;
    private static String unixSocketFilePath;
    private static String chatChannel;
    private static HashMap<String,ServerMapping> serverMappings;

    private static UdpBroadcastSender sender;

    private static UdpReceiver chatReceiver;
    private static UdpReceiver instructionReceiver;

    public static UdpReceiver getInstructionReceiver() {
        return instructionReceiver;
    }

    public static void setInstructionReceiver(UdpReceiver instructionReceiver) {
        ConstantStorage.instructionReceiver = instructionReceiver;
    }

    public static UdpBroadcastSender getSender() {
        return sender;
    }

    public static UdpReceiver getChatReceiver() {
        return chatReceiver;
    }

    public static void setChatReceiver(UdpReceiver receiver) {
        ConstantStorage.chatReceiver = receiver;
    }

    public static void setSender(UdpBroadcastSender sender) {
        ConstantStorage.sender = sender;
    }

    private static String provider(String filename ) {
        return """
                #OMMS config
                enable=false
                httpQueryAddr=localhost
                httpQueryPort=50001
                controllerName=omms-controller
                usesWhitelist=my_whitelist
                unixSocketFilePath
                channel=GLOBAL
                serverMappings""";
    }
    public static void init(){
        SimpleConfig config = SimpleConfig.of("omms").provider(ConstantStorage::provider).request();
        setEnable(config.getOrDefault("enable",false));
        setChatChannel(config.getOrDefault("channel","GLOBAL"));
        setControllerName(config.getOrDefault("controllerName", "omms-controller"));
        setHttpQueryAddress(config.getOrDefault("httpQueryAddr", "localhost"));
        setHttpQueryPort(config.getOrDefault("httpQueryPort", 50001));
        setWhitelistName(config.getOrDefault("usesWhitelist","my_whitelist"));
        String serverMappingNames = config.getOrDefault("serverMappings","");
        setUnixSocketFilePath(config.getOrDefault("unixSocketFilePath", ""));
        if (serverMappingNames.contains(",")){
            if (serverMappingNames.isBlank()){
                setServerMappings(null);
                return;
            }
            HashMap<String,ServerMapping> map = new HashMap<>();
            for (String name : serverMappingNames.split(",")) {
                if (name.isBlank()){
                    continue;
                }
                ServerMapping mapping = new ServerMapping();
                mapping.setWhitelistName(serverMappingNames);
                String displayName = config.getOrDefault("serverMapping.%s.displayName".formatted(name),"");
                String proxyName = config.getOrDefault("serverMapping.%s.proxyName".formatted(name),"");
                if (displayName.isBlank() || proxyName.isBlank()){
                    setServerMappings(null);
                    continue;
                }
                mapping.setDisplayName(displayName);
                mapping.setProxyName(proxyName);
                map.put(name,mapping);
            }
            setServerMappings(map);
        }
        else {
            ServerMapping mapping = new ServerMapping();
            mapping.setWhitelistName(serverMappingNames);
            String displayName = config.getOrDefault("serverMapping.%s.displayName".formatted(serverMappingNames),"");
            String proxyName = config.getOrDefault("serverMapping.%s.proxyName".formatted(serverMappingNames),"");
            if (displayName.isBlank() || proxyName.isBlank()){
                setServerMappings(null);
                return;
            }
            mapping.setDisplayName(displayName);
            mapping.setProxyName(proxyName);
            HashMap<String,ServerMapping> hashMap = new HashMap<>();
            hashMap.put(serverMappingNames,mapping);
            setServerMappings(hashMap);
        }
    }

    public static boolean isEnable() {
        return enable;
    }

    public static void setEnable(boolean enable) {
        ConstantStorage.enable = enable;
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

    public static String getUnixSocketFilePath() {
        return unixSocketFilePath;
    }

    public static void setUnixSocketFilePath(String unixSocketFilePath) {
        ConstantStorage.unixSocketFilePath = unixSocketFilePath;
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

    public static class ServerMapping{
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
