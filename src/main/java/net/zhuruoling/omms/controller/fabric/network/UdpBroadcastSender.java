package net.zhuruoling.omms.controller.fabric.network;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class UdpBroadcastSender extends Thread {

    private final Logger logger = LoggerFactory.getLogger("UdpBroadcastSender");
    boolean stopped = false;
    private ConcurrentHashMap<Target, byte[]> queue = new ConcurrentHashMap<>();
    private HashMap<Target, MulticastSocket> multicastSocketCache = new HashMap<>();

    public UdpBroadcastSender() {
        this.setName("UdpBroadcastSender#" + this.getId());
    }

    private static MulticastSocket createMulticastSocket(String addr, int port) throws IOException {
        MulticastSocket socket;
        InetAddress inetAddress;
        inetAddress = InetAddress.getByName(addr);
        socket = new MulticastSocket(port);
        socket.joinGroup(new InetSocketAddress(inetAddress, port), NetworkInterface.getByInetAddress(inetAddress));
        return socket;

    }

    @Override
    public void run() {
        logger.info("Starting UdpBroadcastSender.");
        while (!stopped) {
            try {
                if (!queue.isEmpty()) {
                    queue.forEach(this::send);
                }
                sleep(10);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        logger.info("Stopped!");
    }

    public boolean isStopped() {
        return stopped;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    public void addToQueue(Target target, String content) {
        queue.put(target, content.getBytes(StandardCharsets.UTF_8));
    }


    private void send(Target target, byte[] content) {
        queue.remove(target, content);
        MulticastSocket socket;

        try {
            if (multicastSocketCache.containsKey(target)) {
                socket = multicastSocketCache.get(target);
            } else {
                socket = createMulticastSocket(target.address, target.port);
                multicastSocketCache.put(target, socket);
            }
            DatagramPacket packet = new DatagramPacket(content, content.length, new InetSocketAddress(target.address, target.port).getAddress(), target.port);

            socket.send(packet);
        } catch (Exception e) {
            logger.error("Cannot send UDP Broadcast.\n\tTarget=%s\n\tContent=%s"
                            .formatted(target.toString(), Arrays.toString(content))
                    , e);
        }
    }

    public void createMulticastSocketCache(Target target) {
        try {
            multicastSocketCache.put(target, createMulticastSocket(target.address, target.port));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Target {

        String address;
        int port;

        public Target(String s, int i) {
            this.address = s;
            this.port = i;
        }

        @Override
        public int hashCode() {
            return (address + port).hashCode();
        }

        @Override
        public String toString() {
            return "Target{" +
                    "address='" + address + '\'' +
                    ", port=" + port +
                    '}';
        }

        public String address() {
            return address;
        }

        public int port() {
            return port;
        }
    }

}
