package icu.takeneko.omms.controller.fabric.mixin;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import icu.takeneko.omms.controller.fabric.config.Config;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerListHeaderS2CPacket.class)
public class PlayerListHeaderMixin {
    @Final
    @Mutable
    @Shadow
    private Text footer;

    @ModifyVariable(method = "<init>(Lnet/minecraft/text/Text;Lnet/minecraft/text/Text;)V", at = @At("HEAD"), index = 2, name = "footer", argsOnly = true)
    private static Text modify(Text footer) {
        if (footer.getString().isEmpty()) {
            return Config.INSTANCE.getCustomFooter();
        }
        return Texts.join(List.of(footer, Config.INSTANCE.getCustomFooter()), Text.literal("\n"));
    }

    @Inject(method = "<init>(Lnet/minecraft/network/PacketByteBuf;)V", at = @At("RETURN"))
    void modifyByBuf(PacketByteBuf buf, CallbackInfo ci) {
        var footerText = footer.copy();
        if (footerText.getString().isEmpty()) {
            this.footer = Config.INSTANCE.getCustomFooter();
            return;
        }
        this.footer = Texts.join(List.of(footerText, Config.INSTANCE.getCustomFooter()), Text.literal("\n"));
    }
}
