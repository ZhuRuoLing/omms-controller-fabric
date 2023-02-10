package net.zhuruoling.omms.controller.fabric.util.logging;

import com.mojang.logging.LogQueues;

import java.util.ArrayList;

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
                logCache.add(string.substring(0, string.length() - 2));
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ArrayList<String> getLogCache() {
        return logCache;
    }
}
