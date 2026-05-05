package com.example.remoteshutdown; // Nhớ kiểm tra lại xem có trùng với tên package trên máy mày không nhé

public class Device {
    private String name;
    private String ip;
    private String status;
    private long lastseen;

    public Device() {
    }

    public Device(String name, String ip, String status, long lastseen) {
        this.name = name;
        this.ip = ip;
        this.status = status;
        this.lastseen = lastseen;
    }

    public String getname() {
        return name;
    }

    public void setname(String name) {
        this.name = name;
    }

    public String getip() {
        return ip;
    }

    public void setip(String ip) {
        this.ip = ip;
    }

    public String getstatus() {
        return status;
    }

    public void setstatus(String status) {
        this.status = status;
    }

    public long getlastseen() {
        return lastseen;
    }

    public void setlastseen(long lastseen) {
        this.lastseen = lastseen;
    }
}