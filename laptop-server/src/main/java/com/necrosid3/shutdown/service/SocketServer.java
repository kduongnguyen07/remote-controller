package com.necrosid3.shutdown.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer extends Thread {

    private final int port = 9999;
    private volatile boolean running = true;

    @Override
    public void run() {
        try (ServerSocket serversocket = new ServerSocket(port)) {
            System.out.println("socket server started on port " + port);

            while (running) {
                try {
                    Socket clientsocket = serversocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientsocket.getInputStream()));
                    String command = in.readLine();

                    if (command != null && command.trim().equalsIgnoreCase("OFF")) {
                        System.out.println("received shutdown command!");
                        Runtime.getRuntime().exec("shutdown -s -t 0");
                    }
                    clientsocket.close();
                } catch (IOException e) {
                    if (running) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        this.running = false;
    }
}
