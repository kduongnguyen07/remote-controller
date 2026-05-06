package com.example.remoteshutdown;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ControlActivity extends AppCompatActivity {

    private String targetip;
    private String targetmac;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_control);

        targetip = getIntent().getStringExtra("IP_ADDRESS");
        targetmac = getIntent().getStringExtra("MAC_ADDRESS"); // ĐÓN MAC TỪ BẤT CỨ ĐÂU TỚI
        String devicename = getIntent().getStringExtra("DEVICE_NAME");

        try {
            TextView tvName = findViewById(R.id.tv_device_name);
            if (tvName != null) tvName.setText(devicename);
        } catch (Exception e) {}

        try {
            TextView tvIp = findViewById(R.id.tv_device_ip);
            if (tvIp != null) tvIp.setText(targetip);
        } catch (Exception e) {}

        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        String savedBg = prefs.getString("bg_uri", null);
        String savedFont = prefs.getString("font_type", "SANS_SERIF");

        if (savedBg != null) {
            try {
                ImageView imgBg = findViewById(R.id.img_control_bg);
                if (imgBg != null) imgBg.setImageURI(Uri.parse(savedBg));
            } catch (Exception e) {}
        }

        try {
            findViewById(android.R.id.content).postDelayed(() -> applyFontToView(getWindow().getDecorView().getRootView(), getFontType(savedFont)), 100);
        } catch (Exception e) {}

        // --- CỤM ÂM LƯỢNG ---
        try {
            SeekBar sbVolume = findViewById(R.id.sb_volume);
            TextView tvVolPreview = findViewById(R.id.tv_vol_preview);

            if (sbVolume != null) {
                sbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (tvVolPreview != null) {
                            tvVolPreview.setText("ÂM LƯỢNG MÁY TÍNH: " + progress + "%");
                        }
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        int res = seekBar.getProgress();
                        send_socket_command("VOL_SET_" + res);
                    }
                });
            }
        } catch (Exception e) {}

        // --- GẮN SỰ KIỆN NÚT ---
        safeSetClickListener(R.id.btn_off, "OFF");
        safeSetClickListener(R.id.btn_lock, "LOCK");
        safeSetClickListener(R.id.btn_sleep, "SLEEP");
        safeSetClickListener(R.id.btn_abort, "ABORT");
        safeSetClickListener(R.id.btn_wake, "WAKE_UP"); // GỌI WOL

        try {
            View btnTimer = findViewById(R.id.btn_timer);
            if (btnTimer != null) btnTimer.setOnClickListener(v -> show_timer_dialog());
        } catch (Exception e) {}

        try {
            View btnMonitor = findViewById(R.id.btn_open_monitor);
            if (btnMonitor != null) {
                btnMonitor.setOnClickListener(v -> {
                    android.content.Intent ans = new android.content.Intent(ControlActivity.this, MonitorActivity.class);
                    ans.putExtra("IP_ADDRESS", targetip);
                    startActivity(ans);
                });
            }
        } catch (Exception e) {}

        try {
            View btnCam = findViewById(R.id.btn_stealth_webcam);
            if (btnCam != null) {
                btnCam.setOnClickListener(v -> {
                    Toast.makeText(this, "Đang gọi Cam, ráng đợi...", Toast.LENGTH_SHORT).show();
                    send_socket_command("WEBCAM");
                });
            }
        } catch (Exception e) {}
    }

    private void safeSetClickListener(int id, String command) {
        try {
            View btn = findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    if (command.equals("WAKE_UP")) {
                        if (targetmac != null && !targetmac.isEmpty() && !targetmac.equals("null")) {
                            send_wol_packet(targetmac);
                        } else {
                            Toast.makeText(this, "Chưa có MAC, quét QR lại 1 lần cho nó lưu đi m!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        send_socket_command(command);
                    }
                });
            }
        } catch (Exception e) {}
    }

    // THUẬT TOÁN WAKE ON LAN TỐI THƯỢNG (CHỐNG LỖI CRASH MAC)
    private void send_wol_packet(String macStr) {
        new Thread(() -> {
            try {
                // Tẩy uế chuỗi MAC: Xóa sạch mọi thứ không phải là số và chữ cái từ A-F
                String cleanMac = macStr.replaceAll("[^0-9A-Fa-f]", "");

                // Nếu độ dài sau khi tẩy uế không bằng đúng 12 (6 cặp Hexa), chứng tỏ MAC láo
                if (cleanMac.length() != 12) {
                    runOnUiThread(() -> Toast.makeText(ControlActivity.this, "MAC sai định dạng: " + macStr, Toast.LENGTH_SHORT).show());
                    return;
                }

                // Chẻ chuỗi 12 ký tự thành 6 byte
                byte[] macBytes = new byte[6];
                for (int i = 0; i < 6; i++) {
                    macBytes[i] = (byte) Integer.parseInt(cleanMac.substring(i * 2, i * 2 + 2), 16);
                }

                // Đúc gói tin Magic: 6 byte 0xFF + 16 lần cái MAC
                byte[] res = new byte[6 + 16 * macBytes.length];
                for (int i = 0; i < 6; i++) res[i] = (byte) 0xff;
                for (int i = 6; i < res.length; i += macBytes.length) {
                    System.arraycopy(macBytes, 0, res, i, macBytes.length);
                }

                // Cầu nguyện và Bắn Broadcast qua Router
                java.net.InetAddress address = java.net.InetAddress.getByName("255.255.255.255");
                java.net.DatagramPacket packet = new java.net.DatagramPacket(res, res.length, address, 9);
                java.net.DatagramSocket socket = new java.net.DatagramSocket();
                socket.setBroadcast(true);
                socket.send(packet);
                socket.close();

                runOnUiThread(() -> Toast.makeText(ControlActivity.this, "Đã ếm bùa Magic Packet, Laptop đang dậy!", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(ControlActivity.this, "Bùa xịt mẹ rồi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void show_timer_dialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Hẹn giờ (Phút)");
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String ans = input.getText().toString();
            if (!ans.isEmpty()) {
                int res = Integer.parseInt(ans) * 60;
                send_socket_command("TIMER_" + res);
            }
        });
        builder.show();
    }

    private void send_socket_command(String command) {
        new Thread(() -> {
            boolean isSuccess = false;
            String resData = null;
            try (Socket socket = new Socket(targetip, 9999);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println(command);

                if (command.equals("WEBCAM")) {
                    resData = in.readLine();
                }
                isSuccess = true;
            } catch (Exception e) {
                isSuccess = false;
            }

            boolean finalSuccess = isSuccess;
            String finalData = resData;
            runOnUiThread(() -> {
                if (finalSuccess) {
                    if (command.equals("WEBCAM") && finalData != null && finalData.startsWith("WEBCAM_DATA_")) {
                        show_stealth_photo(finalData.substring(12));
                    } else if (!command.equals("WEBCAM")) {
                        Toast.makeText(this, "Đã bắn lệnh: " + command, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Đéo kết nối được Laptop", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void show_stealth_photo(String base64Str) {
        try {
            byte[] res = Base64.decode(base64Str, Base64.DEFAULT);
            Bitmap ans = BitmapFactory.decodeByteArray(res, 0, res.length);

            ImageView imgView = new ImageView(this);
            imgView.setImageBitmap(ans);
            imgView.setPadding(20, 20, 20, 20);

            new android.app.AlertDialog.Builder(this)
                    .setTitle("📸 Mồi đã vào tròng!")
                    .setView(imgView)
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Exception e) {
            Toast.makeText(this, "Ảnh bị lỗi rồi", Toast.LENGTH_SHORT).show();
        }
    }

    private Typeface getFontType(String fontName) {
        switch (fontName) {
            case "SERIF": return Typeface.SERIF;
            case "MONOSPACE": return Typeface.MONOSPACE;
            case "CURSIVE": return Typeface.create("cursive", Typeface.NORMAL);
            default: return Typeface.SANS_SERIF;
        }
    }

    private void applyFontToView(View view, Typeface typeface) {
        if (view instanceof TextView) {
            ((TextView) view).setTypeface(typeface);
        } else if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyFontToView(vg.getChildAt(i), typeface);
            }
        }
    }
}