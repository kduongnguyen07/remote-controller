package com.necrosid3.shutdown.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import org.json.JSONObject;

public class SocketServer extends Thread {
    private final int port = 9999;
    private volatile boolean running = true;

    @Override
    public void run() {
        try (ServerSocket serversocket = new ServerSocket(port)) {
            System.out.println("Socket server started on port " + port);
            while (running) {
                try (Socket clientsocket = serversocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientsocket.getInputStream()));
                     PrintWriter out = new PrintWriter(clientsocket.getOutputStream(), true)) {

                    String command = in.readLine();
                    if (command != null) {
                        command = command.trim();
                        System.out.println("Nhan lenh: " + command);

                        if (command.equals("GET_STATS")) {
                            try {
                                JSONObject ans = new JSONObject();

                                // CPU Load
                                com.sun.management.OperatingSystemMXBean osbean = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
                                double cpuload = osbean.getCpuLoad() * 100;
                                if (cpuload < 0) cpuload = 0.0;
                                ans.put("cpu_util", cpuload);

                                // RAM (GB)
                                long totalram = osbean.getTotalMemorySize() / (1024 * 1024 * 1024);
                                long freeram = osbean.getFreeMemorySize() / (1024 * 1024 * 1024);
                                ans.put("ram_used", totalram - freeram);
                                ans.put("ram_total", totalram);

                                // Lấy thông số Task Manager bằng OSHI
                                oshi.SystemInfo si = new oshi.SystemInfo();
                                oshi.hardware.HardwareAbstractionLayer hal = si.getHardware();
                                oshi.software.os.OperatingSystem os = si.getOperatingSystem();

                                ans.put("cpu_name", hal.getProcessor().getProcessorIdentifier().getName());
                                ans.put("processes", os.getProcessCount());
                                ans.put("threads", os.getThreadCount());
                                long upsec = os.getSystemUptime();
                                ans.put("uptime", String.format(java.util.Locale.US, "%02d:%02d:%02d", upsec / 3600, (upsec % 3600) / 60, upsec % 60));

                                // GPU RTX
                                int gpuutil = 0, gputemp = 0;
                                try {
                                    Process process = Runtime.getRuntime().exec("nvidia-smi --query-gpu=utilization.gpu,temperature.gpu --format=csv,noheader,nounits");
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                                    String res = reader.readLine();
                                    if (res != null) {
                                        String[] parts = res.split(",");
                                        gpuutil = Integer.parseInt(parts[0].trim());
                                        gputemp = Integer.parseInt(parts[1].trim());
                                    }
                                } catch (Exception e) {}
                                ans.put("gpu_util", gpuutil);
                                ans.put("gpu_temp", gputemp);

                                out.println(ans.toString());
                            } catch (Exception e) {
                                out.println("{\"error\": true}");
                            }
                        }
                        // Các lệnh hệ thống
                        else if (command.equals("OFF")) {
                            Runtime.getRuntime().exec("cmd.exe /c shutdown -s -t 0");
                        } else if (command.equals("LOCK")) {
                            Runtime.getRuntime().exec("cmd.exe /c rundll32.exe user32.dll,LockWorkStation");
                        } else if (command.equals("SLEEP")) {
                            Runtime.getRuntime().exec("cmd.exe /c rundll32.exe powrprof.dll,SetSuspendState 0,1,0");
                        } else if (command.equals("HIBERNATE")) {
                            Runtime.getRuntime().exec("cmd.exe /c shutdown -h");
                        } else if (command.startsWith("TIMER_")) {
                            String time = command.split("_")[1];
                            Runtime.getRuntime().exec("cmd.exe /c shutdown -s -t " + time);
                        } else if (command.startsWith("SLEEPTIMER_")) {
                            String time = command.split("_")[1];
                            String res = "cmd.exe /c start /min cmd.exe /c \"timeout /t " + time + " /nobreak & rundll32.exe powrprof.dll,SetSuspendState 0,1,0\"";
                            Runtime.getRuntime().exec(res);
                        } else if (command.equals("ABORT")) {
                            Runtime.getRuntime().exec("cmd.exe /c shutdown -a");
                        } else if (command.equals("VOL_UP")) {
                            Runtime.getRuntime().exec("powershell.exe -Command \"(new-object -com wscript.shell).SendKeys([char]175)\"");
                        } else if (command.equals("VOL_DOWN")) {
                            Runtime.getRuntime().exec("powershell.exe -Command \"(new-object -com wscript.shell).SendKeys([char]174)\"");
                        } else if (command.equals("VOL_MUTE")) {
                            Runtime.getRuntime().exec("powershell.exe -Command \"(new-object -com wscript.shell).SendKeys([char]173)\"");
                        }
                    }
                } catch (IOException e) {
                    if (running) System.err.println("Socket error: " + e.getMessage());
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void stopserver() { this.running = false; }
}