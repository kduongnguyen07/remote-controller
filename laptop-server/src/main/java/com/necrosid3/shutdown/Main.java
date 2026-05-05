package com.necrosid3.shutdown;

import com.necrosid3.shutdown.service.FirebaseService;
import com.necrosid3.shutdown.service.SocketServer;
import com.necrosid3.shutdown.util.MacUtil;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Timer;
import java.util.TimerTask;

public class Main {
    private static SocketServer socketserver;
    private static Timer heartbeattimer;
    private static FirebaseService firebaseservice;

    public static void main(String[] args) {
        firebaseservice = new FirebaseService();
        String mac = MacUtil.getMacAddress();
        String ip = MacUtil.getIpAddress();
        String name = MacUtil.getDeviceName();

        // Shutdown Hook: Báo Offline trước khi JVM bị kill[cite: 25]
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            firebaseservice.updateDeviceInfo(mac, name, ip, "OFFLINE");
        }));

        setupSystemTray();
        firebaseservice.updateDeviceInfo(mac, name, ip, "ONLINE");

        heartbeattimer = new Timer();
        heartbeattimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                firebaseservice.updateDeviceInfo(mac, name, ip, "ONLINE");
            }
        }, 60000, 60000);

        socketserver = new SocketServer();
        socketserver.start();
    }

    private static void setupSystemTray() {
        if (!SystemTray.isSupported()) return;
        SystemTray tray = SystemTray.getSystemTray();
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);
        g.fillOval(0, 0, 16, 16);
        g.dispose();

        PopupMenu popup = new PopupMenu();
        MenuItem exititem = new MenuItem("Exit");
        exititem.addActionListener(e -> System.exit(0));
        popup.add(exititem);

        try {
            tray.add(new TrayIcon(image, "Remote Shutdown", popup));
        } catch (Exception e) { e.printStackTrace(); }
    }
}