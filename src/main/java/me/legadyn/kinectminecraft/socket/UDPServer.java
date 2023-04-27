package me.legadyn.kinectminecraft.socket;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.ActionResult;

import javax.xml.crypto.Data;
import java.beans.EventHandler;
import java.io.IOException;
import java.net.*;


public class UDPServer {
    private DatagramSocket udpSocket;
    private boolean running = true;
    private Thread listenThread;

    public UDPServer() throws SocketException {
        onInitialize();
    }

    public void initializeServer() throws SocketException {
        this.udpSocket = new DatagramSocket(62034);
    }


    public void listen() throws Exception {

        if(listenThread == null) {
            listenThread = new Thread(() -> {
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        udpSocket.receive(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String msg = new String(packet.getData()).trim();
                    // Llamamos al evento personalizado SocketReceivedPacket
                    SocketReceivedPacket.EVENT.invoker().onMyCustomEvent(msg);
                    /*FabricLoader.getInstance().getEntrypointContainers("socketReceivedPacket", SocketReceivedPacket.class).forEach((container) -> {
                        container.getEntrypoint().onPacketReceived(event);
                    });*/
                }
            });
            listenThread.start();
        }
        /*while (running) {
            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            udpSocket.receive(packet);
            String msg = new String(packet.getData()).trim();
            // Llamamos al evento personalizado SocketReceivedPacket
            SocketReceivedPacket.EVENT.invoker().onMyCustomEvent("Hola, soy un argumento personalizado para el evento");
            /*FabricLoader.getInstance().getEntrypointContainers("socketReceivedPacket", SocketReceivedPacket.class).forEach((container) -> {
                container.getEntrypoint().onPacketReceived(event);
            });*/


        }


    public void stop() {
        if(listenThread != null) {
            listenThread.interrupt();
            listenThread = null;
        }
        udpSocket.close();
    }

    public void onInitialize() {
        // Inicializamos el servidor cuando se inicie Fabric
        try {
            initializeServer();
            listen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

