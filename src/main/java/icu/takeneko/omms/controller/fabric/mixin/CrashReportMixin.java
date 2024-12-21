package icu.takeneko.omms.controller.fabric.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import icu.takeneko.omms.controller.fabric.config.SharedVariable;
import icu.takeneko.omms.controller.fabric.network.NetworkUtilKt;
import net.minecraft.CrashReport;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrashReport.class)
public abstract class CrashReportMixin {

    @Shadow @Final private static Logger LOGGER;

    @Inject(
        method = "getFriendlyReport",
        at = @At(value = "INVOKE", target = "Ljava/lang/StringBuilder;toString()Ljava/lang/String;")
    )
    void inj(
        CallbackInfoReturnable<String> cir,
        @Local StringBuilder sb
    ) {
        if (!SharedVariable.ready) return;
        LOGGER.info("Uploading crash report to OMMS Central Server.");
        try {
            NetworkUtilKt.uploadCrashReport(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
