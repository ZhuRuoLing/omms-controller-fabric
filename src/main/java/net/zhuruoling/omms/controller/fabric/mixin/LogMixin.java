package net.zhuruoling.omms.controller.fabric.mixin;

import com.mojang.logging.plugins.QueueLogAppender;
import net.zhuruoling.omms.controller.fabric.config.SharedVariable;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = QueueLogAppender.class, remap = false)
public abstract class LogMixin {

    private static final PatternLayout layout = PatternLayout.newBuilder().withPattern("[%d{HH:mm:ss}] [%t/%level]: %msg{nolookups}%n").build();

    @Inject(method = "append", at = @At("HEAD"), remap = false)
    void inj(LogEvent event, CallbackInfo ci){
        String s = layout.toSerializable(event);
        SharedVariable.logCache.add(s);
    }
}
