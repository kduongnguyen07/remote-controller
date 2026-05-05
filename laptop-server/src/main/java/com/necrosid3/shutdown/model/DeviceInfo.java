package com.necrosid3.shutdown.model;

public class DeviceInfo {
    private String name;
    private String ip;
    private String status;
    private long lastseen;

    public DeviceInfo() {
    }

    public DeviceInfo(String name, String ip, String status, long lastseen) {
        this.name = name;
        this.ip = ip;
        this.status = status;
        this.lastseen = lastseen;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getLastseen() {
        return lastseen;
    }

    public void setLastseen(long lastseen) {
        this.lastseen = lastseen;
    }
}
