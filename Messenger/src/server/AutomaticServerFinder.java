package service;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class AutomaticServerFinder {
    private static final int DISCOVERY_PORT = 8888;
    private static final String DISCOVERY_MESSAGE = "MESSENGER_SERVER_DISCOVERY";
    private static final String RESPONSE_PREFIX = "MESSENGER_SERVER_AT:";

    public static String findServerInNetwork(int timeoutMs) {
        List<String> foundServers = new ArrayList<>();

        try {
            // Wysyłanie broadcast wiadomości
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.setSoTimeout(timeoutMs);

            byte[] sendData = DISCOVERY_MESSAGE.getBytes();

            // Pobierz wszystkie interfejsy sieciowe
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            // Przekształć Enumeration na List dla łatwiejszej obsługi
            List<NetworkInterface> interfaceList = Collections.list(interfaces);

            for (NetworkInterface networkInterface : interfaceList) {
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
                for (InterfaceAddress interfaceAddress : interfaceAddresses) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }

                    try {
                        DatagramPacket sendPacket = new DatagramPacket(
                                sendData, sendData.length, broadcast, DISCOVERY_PORT
                        );
                        socket.send(sendPacket);
                        System.out.println("Broadcast wysłany do: " + broadcast.getHostAddress());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // Nasłuchiwanie odpowiedzi
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    socket.receive(receivePacket);
                    String response = new String(receivePacket.getData(), 0, receivePacket.getLength());

                    if (response.startsWith(RESPONSE_PREFIX)) {
                        String serverIP = response.substring(RESPONSE_PREFIX.length());
                        foundServers.add(serverIP);
                        System.out.println("Znaleziono serwer: " + serverIP);
                    }
                } catch (SocketTimeoutException e) {
                    // Kontynuuj czekanie
                }
            }

            socket.close();

        } catch (Exception e) {
            System.err.println("Błąd podczas szukania serwera: " + e.getMessage());
        }

        // Zwróć pierwszy znaleziony serwer
        return foundServers.isEmpty() ? null : foundServers.get(0);
    }

    public static void startDiscoveryServer() {
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT);
                socket.setBroadcast(true);

                System.out.println("Serwer discovery nasłuchuje na porcie " + DISCOVERY_PORT);

                byte[] buffer = new byte[1024];

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength());

                    if (DISCOVERY_MESSAGE.equals(message)) {
                        String localIP = InetAddress.getLocalHost().getHostAddress();
                        String response = RESPONSE_PREFIX + localIP;

                        byte[] responseData = response.getBytes();
                        DatagramPacket responsePacket = new DatagramPacket(
                                responseData, responseData.length,
                                packet.getAddress(), packet.getPort()
                        );

                        socket.send(responsePacket);
                        System.out.println("Odpowiedź wysłana do: " + packet.getAddress());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}