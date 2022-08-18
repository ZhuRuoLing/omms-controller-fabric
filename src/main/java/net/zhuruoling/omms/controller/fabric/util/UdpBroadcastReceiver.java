package net.zhuruoling.omms.controller.fabric.util;


import com.google.gson.Gson;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.zhuruoling.omms.controller.fabric.config.ConstantStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public class UdpBroadcastReceiver extends Thread{
    private static final Logger logger = LoggerFactory.getLogger("UdpBroadcastReceiver");
    private final MinecraftServer server;
    public UdpBroadcastReceiver(MinecraftServer server){
        this.setName("UdpBroadcastReceiver#" + getId());
        this.server = server;
    }

    @Override
    public void run() {
        try {
            int port = 10086;
            String address = "224.114.51.4"; // 224.114.51.4:10086
            MulticastSocket socket = null;
            InetAddress inetAddress = null;
            inetAddress = InetAddress.getByName(address);
            socket = new MulticastSocket(port);
            logger.info("Started Broadcast Receiver at " + address + ":" + port);
            socket.joinGroup(new InetSocketAddress(inetAddress,port), NetworkInterface.getByInetAddress(inetAddress));

            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            for (;;) {
                try {
                    socket.receive(packet);
                    String msg = new String(packet.getData(), packet.getOffset(),
                            packet.getLength(), StandardCharsets.UTF_8);
                    var broadcast = new Gson().fromJson(msg, Broadcast.class);
                    logger.info(String.format("%s <%s[%s]> %s", Objects.requireNonNull(broadcast).getChannel(), broadcast.getPlayer(), broadcast.getServer(), broadcast.getContent()));
                    if (!Objects.equals(broadcast.getServer(), ConstantStorage.getControllerName())){
                        server.getPlayerManager().broadcast(Util.fromBroadcast(broadcast), MessageType.SYSTEM, UUID.randomUUID());
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
