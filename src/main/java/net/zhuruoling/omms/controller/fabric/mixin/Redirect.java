package net.zhuruoling.omms.controller.fabric.mixin;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.OpCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(OpCommand.class)
public class Redirect {
    @org.spongepowered.asm.mixin.injection.Redirect(method = "method_13470", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/command/ServerCommandSource;hasPermissionLevel(I)Z"))
    static boolean inj(ServerCommandSource instance, int level){
        return instance.hasPermissionLevel(2);
    }
}
