package com.necrosid3.shutdown;

import com.necrosid3.shutdown.service.FirebaseService;
import com.necrosid3.shutdown.service.SocketServer;
import com.necrosid3.shutdown.util.MacUtil;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.util.Timer;
import java.util.TimerTask;

public class Main {

    private static SocketServer socketserver;
    private static Timer heartbeattimer;

    public static void main(String[] args) {
        System.out.println("starting laptop server...");

        setupSystemTray();

        FirebaseService firebaseservice = new FirebaseService();
        String macaddress = MacUtil.getMacAddress();
        String ipaddress = MacUtil.getIpAddress();
        String devicename = MacUtil.getDeviceName();

        // report initial status
        firebaseservice.updateDeviceInfo(macaddress, devicename, ipaddress, "ONLINE");

        // heartbeat loop every 60 seconds
        heartbeattimer = new Timer();
        heartbeattimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                firebaseservice.updateDeviceInfo(macaddress, devicename, ipaddress, "ONLINE");
                System.out.println("heartbeat sent to firebase.");
            }
        }, 60000, 60000);

        // start socket server
        socketserver = new SocketServer();
        socketserver.start();
    }

    private static void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            System.err.println("system tray is not supported on this platform!");
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();
        
        int iconsize = 16;
        BufferedImage image = new BufferedImage(iconsize, iconsize, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = image.createGraphics();
        g.setColor(java.awt.Color.RED);
        g.fillOval(0, 0, iconsize, iconsize);
        g.dispose();

        PopupMenu popup = new PopupMenu();
        MenuItem exititem = new MenuItem("Exit");
        
        exititem.addActionListener(e -> {
            System.out.println("exiting app...");
            if (socketserver != null) {
                socketserver.stopServer();
            }
            if (heartbeattimer != null) {
                heartbeattimer.cancel();
            }
            
            // update status to offline before exiting
            FirebaseService firebaseservice = new FirebaseService();
            firebaseservice.updateDeviceInfo(MacUtil.getMacAddress(), MacUtil.getDeviceName(), MacUtil.getIpAddress(), "OFFLINE");
            
            System.exit(0);
        });

        popup.add(exititem);

        TrayIcon trayicon = new TrayIcon(image, "Laptop Remote Shutdown", popup);
        trayicon.setImageAutoSize(true);

        try {
            tray.add(trayicon);
        } catch (AWTException e) {
            System.err.println("tray icon could not be added.");
        }
    }
}
