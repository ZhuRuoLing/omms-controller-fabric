package net.zhuruoling.omms.controller.fabric.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.zhuruoling.omms.controller.fabric.config.SharedVariable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class ServerMixin {
    @Shadow private PlayerManager playerManager;

    @Inject(method = "tick", at = @At("HEAD"))
    void tickInject(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        if(SharedVariable.shouldCrash){
            throw new Error("I`m Crashing!");
        }
    }
}
