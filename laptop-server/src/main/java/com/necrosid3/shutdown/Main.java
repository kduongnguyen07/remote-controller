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

        // Shutdown Hook: Báo Offline trước khi JVM bị kill
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

        // Gọi hàm hiện QR ở đây (đã mang ra ngoài)
        show_qr(ip, name, mac);

        socketserver = new SocketServer();
        socketserver.start();
    }

    // HÀM HIỆN QR ĐÃ ĐƯỢC LÔI RA KHỎI MAIN
    public static void show_qr(String ip, String name, String mac) {
        try {
            // Nét căng: Nhét cả MAC vào JSON để lát bật máy
            String data = "{\"ip\":\"" + ip + "\", \"name\":\"" + name + "\", \"mac\":\"" + mac + "\"}";

            com.google.zxing.common.BitMatrix matrix = new com.google.zxing.MultiFormatWriter().encode(data, com.google.zxing.BarcodeFormat.QR_CODE, 250, 250);
            java.awt.image.BufferedImage img = com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage(matrix);

            javax.swing.JFrame frame = new javax.swing.JFrame("Quét QR để điều khiển");
            frame.add(new javax.swing.JLabel(new javax.swing.ImageIcon(img)));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
            frame.setVisible(true);
        } catch (Exception e) {}
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