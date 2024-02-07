package icu.takeneko.omms.controller.fabric.mixin;

import icu.takeneko.omms.controller.fabric.config.SharedVariable;
import icu.takeneko.omms.controller.fabric.util.Util;
import net.minecraft.util.crash.CrashReport;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Mixin(CrashReport.class)
public abstract class CrashReportMixin {

    @Shadow
    @Final
    private static DateTimeFormatter DATE_TIME_FORMATTER;
    @Shadow
    @Final
    private String message;

    @Shadow
    private static String generateWittyComment() {
        return null;
    }

    @Unique
    String makeCrashReport() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("---- Minecraft Crash Report ----\n");
        stringBuilder.append("// ");
        stringBuilder.append(generateWittyComment());
        stringBuilder.append("\n\n");
        stringBuilder.append("Time: ");
        stringBuilder.append(DATE_TIME_FORMATTER.format(ZonedDateTime.now()));
        stringBuilder.append("\n");
        stringBuilder.append("Description: ");
        stringBuilder.append(this.message);
        stringBuilder.append("\n\n");
        stringBuilder.append(this.getCauseAsString());
        stringBuilder.append("\n\nA detailed walkthrough of the error, its code path and all known details is as follows:\n");

        stringBuilder.append("-".repeat(87));

        stringBuilder.append("\n\n");
        this.addStackTrace(stringBuilder);
        return stringBuilder.toString();
    }

    @Shadow
    public abstract String getCauseAsString();

    @Shadow
    public abstract void addStackTrace(StringBuilder crashReportBuilder);

    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "asString", at = @At("RETURN"))
    void inj(CallbackInfoReturnable<String> cir) {
        if (!SharedVariable.ready) return;
        LOGGER.info("Uploading crash report to OMMS Central Server.");
        var content = makeCrashReport();
        try {
            Util.submitCrashReport(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
