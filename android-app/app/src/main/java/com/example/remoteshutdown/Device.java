package com.example.remoteshutdown;

public class Device {
    private String macaddress;
    private String name;
    private String ip;
    private String status;
    private long lastseen;

    public Device(String macaddress, String name, String ip, String status, long lastseen) {
        this.macaddress = macaddress;
        this.name = name;
        this.ip = ip;
        this.status = status;
        this.lastseen = lastseen;
    }

    public String get_macaddress() {
        String ans = this.macaddress;
        return ans;
    }

    public void set_macaddress(String macaddress) {
        this.macaddress = macaddress;
    }

    public String get_name() {
        String ans = this.name;
        return ans;
    }

    public void set_name(String name) {
        this.name = name;
    }

    public String get_ip() {
        String ans = this.ip;
        return ans;
    }

    public void set_ip(String ip) {
        this.ip = ip;
    }

    public String get_status() {
        String ans = this.status;
        return ans;
    }

    public void set_status(String status) {
        this.status = status;
    }

    public long get_lastseen() {
        long ans = this.lastseen;
        return ans;
    }

    public void set_lastseen(long lastseen) {
        this.lastseen = lastseen;
    }
}
