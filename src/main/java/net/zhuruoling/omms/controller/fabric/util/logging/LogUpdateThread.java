package net.zhuruoling.omms.controller.fabric.util.logging;

import com.mojang.logging.LogQueues;
import net.zhuruoling.omms.controller.fabric.network.http.HttpServerMainKt;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;

public class LogUpdateThread extends Thread {
    private final ArrayList<String> logCache = new ArrayList<>();

    public LogUpdateThread() {
        super("LogUpdate");
    }

    @Override
    public void run() {
        String string;
        while ((string = LogQueues.getNextLogEvent("ServerGuiConsole")) != null) {
            try {
                var line = string.substring(0, string.length() - 2);
                synchronized (logCache){
                    logCache.add(line);
                }
                HttpServerMainKt.sendToAllConnection(line);
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            } catch (Exception e) {
                    e.printStackTrace();
            }
        }
    }

    public ArrayList<String> getLogCache() {
        return logCache;
    }
}
