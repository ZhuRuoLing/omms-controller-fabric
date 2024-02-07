package icu.takeneko.omms.controller.fabric.network;


import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;


public class UdpReceiver extends Thread {
    private static final Logger logger = LoggerFactory.getLogger("UdpBroadcastReceiver");
    private final MinecraftServer server;
    BiConsumer<MinecraftServer, String> function = null;
    UdpBroadcastSender.Target target = null;

    public UdpReceiver(MinecraftServer server, UdpBroadcastSender.Target target, BiConsumer<MinecraftServer, String> function) {
        this.setName("UdpBroadcastReceiver#" + getId());
        this.server = server;
        this.function = function;
        this.target = target;
    }

    @Override
    public void run() {
        try {
            int port = target.port();
            String address = target.address(); // 224.114.51.4:10086
            MulticastSocket socket;
            InetAddress inetAddress;
            inetAddress = InetAddress.getByName(address);
            socket = new MulticastSocket(port);
            logger.info("Started Broadcast Receiver at " + address + ":" + port);
            socket.joinGroup(new InetSocketAddress(inetAddress, port), null);
            for (; ; ) {
                try {
                    DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
                    socket.receive(packet);
                    String msg = new String(packet.getData(), packet.getOffset(),
                            packet.getLength(), StandardCharsets.UTF_8);
                    function.accept(this.server, msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
