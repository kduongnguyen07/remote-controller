package com.necrosid3.shutdown.util;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class MacUtil {

    public static String getMacAddress() {
        String ans = "";
        SystemInfo systeminfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systeminfo.getHardware();
        List<NetworkIF> networkifs = hardware.getNetworkIFs();
        
        for (NetworkIF net : networkifs) {
            String mac = net.getMacaddr();
            if (mac != null && !mac.isEmpty() && !mac.equals("00:00:00:00:00:00")) {
                ans = mac.replace(":", "").replace("-", "").toLowerCase();
                break;
            }
        }
        return ans;
    }

    public static String getIpAddress() {
        String ans = "";
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface networkInterface = interfaces.nextElement();

                // Bỏ qua các mạng đã tắt hoặc mạng ảo loopback
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                java.util.Enumeration<java.net.InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();

                    // Chỉ lấy chuẩn IPv4 và là mạng LAN cục bộ (như 192.168.x.x), tự động né IP của Radmin VPN
                    if (addr instanceof java.net.Inet4Address && addr.isSiteLocalAddress()) {
                        ans = addr.getHostAddress();
                        return ans; // Tìm thấy phát là chốt luôn
                    }
                }
            }

            // Nếu đen quá đéo tìm được thì mới xài cách cũ
            if (ans.isEmpty()) {
                ans = java.net.InetAddress.getLocalHost().getHostAddress();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ans;
    }

    public static String getDeviceName() {
        String ans = "";
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            ans = localhost.getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return ans;
    }
}
