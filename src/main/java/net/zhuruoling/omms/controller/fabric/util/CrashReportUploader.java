package net.zhuruoling.omms.controller.fabric.util;

import net.zhuruoling.omms.controller.fabric.config.Config;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
@SuppressWarnings("all")
public class CrashReportUploader {
    public static void upload(String content){
        String url = "http://%s:%d/controller/crashReport/upload".formatted(Config.INSTANCE.getHttpQueryAddress(), Config.INSTANCE.getHttpQueryPort());        HttpClient client = HttpClient.newHttpClient();
       try {
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(content))
                    .header("Content-Type", "text/plain")
                    .header("Controller-ID", Config.INSTANCE.getControllerName())
                    .uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(Charset.defaultCharset()));
        }catch (Exception e){
           e.printStackTrace();
       }
    }
}
