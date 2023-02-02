package net.zhuruoling.omms.controller.fabric.mixin;

import net.minecraft.util.crash.CrashReport;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Mixin(CrashReport.class)
public abstract class CrashReportMixin {

    String makeCrashReport(){
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

    @Shadow public abstract String asString();

    @Shadow
    private static String generateWittyComment() {
        return null;
    }

    @Shadow @Final private static DateTimeFormatter DATE_TIME_FORMATTER;

    @Shadow @Final private String message;

    @Shadow public abstract String getCauseAsString();

    @Shadow public abstract void addStackTrace(StringBuilder crashReportBuilder);

    @Inject(method = "asString", at=@At("RETURN"))
    void inj(CallbackInfoReturnable<String> cir){
        var content = makeCrashReport();
        Path path = Path.of("wdnmd.txt");
        try {
            Files.deleteIfExists(path);
            Files.createFile(path);
            Files.writeString(path, content);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
