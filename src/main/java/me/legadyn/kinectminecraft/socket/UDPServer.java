package me.legadyn.kinectminecraft.socket;

import java.io.IOException;
import java.net.*;


public class UDPServer {

    private DatagramSocket udpSocket;

    private Thread listenThread;

    public UDPServer() throws SocketException {
        onInitialize();
    }

    public void initializeServer() throws SocketException {
        this.udpSocket = new DatagramSocket(62034);
    }


    public void listen() {

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
                }
            });
            listenThread.start();
        }
    }


    public void stop() {
        if(listenThread != null) {
            listenThread.interrupt();
            listenThread = null;
        }
        udpSocket.close();
    }

    public void onInitialize() {
        // Inicializamos el servidor
        try {
            initializeServer();
            listen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

