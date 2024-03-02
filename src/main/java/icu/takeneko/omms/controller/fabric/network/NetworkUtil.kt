package icu.takeneko.omms.controller.fabric.network

import icu.takeneko.omms.controller.fabric.config.Config
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.LoggerFactory

private val httpClient by lazy {
    HttpClient(CIO) {
        engine {
            threadsCount = 4
            pipelining = true
        }
        install(Auth) {
            basic {
                credentials {
                    BasicAuthCredentials(
                        username = Config.getControllerName(),
                        password = Config.getControllerName().hashCode().toString(16)
                    )
                }
            }
        }
    }
}

private val logger = LoggerFactory.getLogger("Whitelist")

fun queryPlayerInAllWhitelist(playerName: String): List<String>{
    return runBlocking {
        val url = "http://${Config.getHttpQueryAddress()}:${Config.getHttpQueryPort()}/whitelist/queryAll/$playerName"
        val resp = httpClient.get(url)
        if (resp.status != HttpStatusCode.OK) {
            throw RuntimeException("Server returned non 200 status: ${resp.status.value}")
        } else {
            resp.body<List<String>>()
        }
    }
}

fun authPlayer(playerName: String): Text? {
    val url = "http://${Config.getHttpQueryAddress()}:${Config.getHttpQueryPort()}/whitelist/queryAll/$playerName"
    return runBlocking {
        try {
            val resp = httpClient.get(url)
            if (resp.status != HttpStatusCode.OK) {
                throw RuntimeException("Server returned non 200 status: ${resp.status.value}")
            } else {
                val list = resp.body<List<String>>()
                if (playerName !in list) {
                    Text.translatable("multiplayer.disconnect.not_whitelisted").copyContentOnly().setStyle(Style.EMPTY.withColor(Formatting.RED))
                } else {
                    logger.info("Successfully authed player $playerName")
                    null
                }
            }
        } catch (e: Exception) {
            logger.debug("Cannot auth with OMMS Central server.", e)
            Text.literal("Cannot auth with OMMS Central server.\nCaused By:")
                .append(Text.of(e.toString()).copyContentOnly().setStyle(Style.EMPTY.withColor(Formatting.RED)))

        }
    }
}

fun uploadCrashReport(content:String){
    runBlocking {
        val url = "http://${Config.getHttpQueryAddress()}:${Config.getHttpQueryPort()}/controller/crashReport/upload"
        httpClient.post(url){
            header("Controller-ID", Config.getControllerName())
            setBody(content)
        }
    }
}