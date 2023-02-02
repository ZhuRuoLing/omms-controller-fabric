package net.zhuruoling.omms.controller.fabric.config

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import java.io.FileReader
import java.nio.file.Files
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

const val defaultConfig = """#OMMS config
enableWhitelist=false
enableChatBridge=false
enableJoinMotd=false
enableRemoteControl=false
httpQueryAddr=localhost
httpQueryPort=50001
httpServerPort=50010
controllerName=omms-controller
usesWhitelist=my_whitelist
channel=GLOBAL
allowedFakePlayerPrefix=bot_
allowedFakePlayerSuffix=_bot
customFooter
serverMappings"""


object Config {
    private var enableChatBridge = false
    private var enableWhitelist = false
    private var enableJoinMotd = false
    private var enableRemoteControl = false
    private var controllerName = ""
    private var httpQueryAddress = ""
    private var whitelistName = ""
    private var httpQueryPort = 0
    private var httpServerPort = 0
    private var chatChannel = ""
    private var customFooter = ""
    private var serverMappings = hashMapOf<String, ServerMapping>()


    fun load() {
        val configPath = FabricLoader.getInstance().configDir.resolve("omms.properties")
        if (!configPath.exists() || configPath.isDirectory()) {
            Files.createFile(configPath)
            Files.writeString(configPath, defaultConfig)
        }
        val reader = FileReader(configPath.toFile())
        val properties = Properties()
        properties.load(reader)
        reader.close()

        this.enableChatBridge = properties.get("enableChatBridge", "false").toBoolean()
        this.enableWhitelist = properties.get("enableWhitelist", "false").toBoolean()
        this.enableJoinMotd = properties.get("enableJoinMotd", "false").toBoolean()
        this.enableRemoteControl = properties.get("enableRemoteControl", "false").toBoolean()
        this.chatChannel = properties.get("channel", "GLOBAL")
        this.controllerName = properties.get("controllerName", "omms-controller")
        httpQueryAddress = properties.get("httpQueryAddr", "localhost")
        httpQueryPort = properties.get("httpQueryPort", "50001").toInt()
        whitelistName = properties.get("usesWhitelist", "my_whitelist")
        customFooter = properties.get("customFooter", "")

        val serverMappingNames: String = properties.get("serverMappings", "")
        if (serverMappingNames.contains(",")) {
            if (serverMappingNames.isBlank()) {
                this.serverMappings = hashMapOf()
                return
            }
            val map: HashMap<String, ServerMapping> = HashMap()
            for (name in serverMappingNames.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                if (name.isBlank()) {
                    continue
                }
                val mapping = ServerMapping()
                mapping.whitelistName = serverMappingNames
                val displayName: String = properties.get("serverMapping.$name.displayName", "")
                val proxyName: String = properties.get("serverMapping.$name.proxyName", "")
                if (displayName.isBlank() || proxyName.isBlank()) {
                    this.serverMappings = hashMapOf()
                    continue
                }
                mapping.displayName = displayName
                mapping.proxyName = proxyName
                map[name] = mapping
            }
            this.serverMappings = map
        } else {
            val mapping = ServerMapping()
            mapping.whitelistName = serverMappingNames
            val displayName: String =
                properties.get("serverMapping.$serverMappingNames.displayName", "")
            val proxyName: String = properties.get("serverMapping.$serverMappingNames.proxyName", "")
            if (displayName.isBlank() || proxyName.isBlank()) {
                this.serverMappings = hashMapOf()
                return
            }
            mapping.displayName = displayName
            mapping.proxyName = proxyName
            val hashMap: HashMap<String, ServerMapping> = HashMap()
            hashMap[serverMappingNames] = mapping
            this.serverMappings = hashMap
        }
    }

    fun getCustomFooter(): MutableText {
        return Text.literal(customFooter)
    }

    fun isEnableChatBridge(): Boolean {
        return this.enableChatBridge
    }

    fun isEnableJoinMotd(): Boolean {
        return this.enableJoinMotd
    }

    fun isEnableRemoteControl(): Boolean {
        return this.enableRemoteControl
    }

    fun isEnableWhitelist(): Boolean {
        return this.enableWhitelist
    }

    fun getHttpQueryAddress(): String {
        return this.httpQueryAddress
    }


    fun getHttpQueryPort(): Int {
        return this.httpQueryPort
    }


    fun getControllerName(): String {
        return this.controllerName
    }


    fun getWhitelistName(): String? {
        return this.whitelistName
    }


    fun getChatChannel(): String {
        return this.chatChannel
    }

    fun getServerMappings(): HashMap<String, ServerMapping> {
        return this.serverMappings
    }

    fun getHttpServerPort() = httpServerPort

}

fun <T> Properties.get(key: String, defaultValue: T): T {
    return this.getOrDefault(key, defaultValue) as T
}