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
            InetAddress localhost = InetAddress.getLocalHost();
            ans = localhost.getHostAddress();
        } catch (UnknownHostException e) {
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
