package net.zhuruoling.omms.controller.fabric.util.logging;

import net.zhuruoling.omms.controller.fabric.config.SharedVariable;
import net.zhuruoling.omms.controller.fabric.util.Util;
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
        //Util.sendChatBroadcast(s, "logger");
        SharedVariable.logCache.add(s);
    }

    public static MemoryAppender newAppender(){
        return new MemoryAppender("MemAppender",
                LevelRangeFilter.createFilter(Level.INFO, Level.FATAL, Filter.Result.ACCEPT, Filter.Result.DENY),
                PatternLayout.newBuilder().withPattern("[%d{HH:mm:ss}] [%t/%level]: %msg{nolookups}%n").build(),
                false,
                null
                );
    }
}
