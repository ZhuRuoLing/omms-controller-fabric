package net.zhuruoling.omms.controller.fabric.mixin;

import kotlin.collections.CollectionsKt;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.zhuruoling.omms.controller.fabric.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerPlayerEntity.class)
public class SystemChatMixin {
    @ModifyVariable(method = "sendMessage(Lnet/minecraft/text/Text;)V", name = "message", index = 1, at = @At("HEAD"), argsOnly = true)
    private Text modifyText(Text message){
        System.out.println("Changing footer");
        return Texts.join(CollectionsKt.listOf(message, Config.INSTANCE.getCustomFooter()), Text.empty());
    }
}
