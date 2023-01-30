package net.zhuruoling.omms.controller.fabric.config;

public class ServerMapping {
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
