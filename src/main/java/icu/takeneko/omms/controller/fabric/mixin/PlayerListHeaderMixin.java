package icu.takeneko.omms.controller.fabric.mixin;


import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import icu.takeneko.omms.controller.fabric.config.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ClientboundTabListPacket.class)
public class PlayerListHeaderMixin {
    @Final
    @Mutable
    @Shadow
    private Component footer;

    @WrapOperation(
        method = "<init>(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/network/protocol/game/ClientboundTabListPacket;footer:Lnet/minecraft/network/chat/Component;"
        )
    )
    private void modifyFooter(ClientboundTabListPacket instance, Component value, Operation<Void> original) {
        Component result;
        if (value.getString().isEmpty()) {
            result = Config.INSTANCE.getCustomFooter();
        } else {
            result = ComponentUtils.formatList(
                List.of(
                    value,
                    Config.INSTANCE.getCustomFooter()
                ),
                Component.literal("\n")
            );
        }
        original.call(this, result);
    }

}
