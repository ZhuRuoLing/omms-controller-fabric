package net.zhuruoling.omms.controller.fabric.util.logging;

import net.zhuruoling.omms.controller.fabric.config.SharedVariable;
import net.zhuruoling.omms.controller.fabric.network.http.HttpServerMainKt;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.filter.LevelRangeFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;

public class MemoryAppender extends AbstractAppender {

    public MemoryAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }

    @Override
    public void append(LogEvent event) {
        String s = getLayout().toSerializable(event).toString();
        s = s.substring(0, s.length()-2);
        //Util.sendChatBroadcast(s, "logger");
        synchronized (SharedVariable.logCache){
            if (SharedVariable.logCache.size() > 500){
                SharedVariable.logCache.remove(0);
                SharedVariable.logCache.remove(0);
            }
            SharedVariable.logCache.add(s);
        }
        String finalS = s;
        if (!(SharedVariable.getExecutorService().isTerminated() || SharedVariable.getExecutorService().isShutdown())){
            SharedVariable.getExecutorService().submit(() -> HttpServerMainKt.sendToAllConnection(finalS));
        }
    }

    public static MemoryAppender newAppender(String name){
        return new MemoryAppender(name,
                LevelRangeFilter.createFilter(Level.FATAL, Level.INFO, Filter.Result.ACCEPT, Filter.Result.DENY),
                PatternLayout.newBuilder().withPattern("[%d{HH:mm:ss}] [%t/%level]: %msg{nolookups}%n").build(),
                false,
                null
                );
    }
}
