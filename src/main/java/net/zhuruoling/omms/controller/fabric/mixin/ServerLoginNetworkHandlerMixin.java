package net.zhuruoling.omms.controller.fabric.mixin;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginQueryResponseC2SPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginNetworkHandler.class)
public class ServerLoginNetworkHandlerMixin {

    @Shadow @Final
    ClientConnection connection;

    @Inject(method = "onKey", at = @At(value = "INVOKE", shift = At.Shift.BEFORE, target = "Lnet/minecraft/network/ClientConnection;setupEncryption(Ljavax/crypto/Cipher;Ljavax/crypto/Cipher;)V"))
    void sendRequest(LoginKeyC2SPacket packet, CallbackInfo ci){
//        var p = new LoginQueryRequestS2CPacket(Util.PACKET_ID,Util.AUTH_PACKET_CHANNEL, PacketByteBufs.create());
//        this.connection.send(p);
    }


    @Inject(method = "onQueryResponse", at = @At("HEAD"), cancellable = true)
    void inj(LoginQueryResponseC2SPacket packet, CallbackInfo ci){
        ci.cancel();
    }
}
