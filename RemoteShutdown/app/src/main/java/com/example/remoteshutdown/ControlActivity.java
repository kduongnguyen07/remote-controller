package com.example.remoteshutdown;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ControlActivity extends AppCompatActivity {

    private String targetip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_control);

        targetip = getIntent().getStringExtra("IP_ADDRESS");
        String devicename = getIntent().getStringExtra("DEVICE_NAME");

        ((TextView) findViewById(R.id.tv_device_name)).setText(devicename);
        ((TextView) findViewById(R.id.tv_device_ip)).setText(targetip);

        // --- ĐỒNG BỘ ẢNH NỀN VÀ FONT TỪ TRANG CHỦ ---
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        String savedBg = prefs.getString("bg_uri", null);
        String savedFont = prefs.getString("font_type", "SANS_SERIF");

        if (savedBg != null) {
            try {
                ((ImageView) findViewById(R.id.img_control_bg)).setImageURI(Uri.parse(savedBg));
            } catch (Exception e) {}
        }

        findViewById(android.R.id.content).postDelayed(() -> applyFontToView(getWindow().getDecorView().getRootView(), getFontType(savedFont)), 100);

        // --- XỬ LÝ THANH TRƯỢT VOLUME (JOG DIAL) ---
        SeekBar sbVolume = findViewById(R.id.sb_volume);
        sbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                int diff = progress - 50; // Tính toán xem lệch bao nhiêu so với tâm

                if (diff > 0) {
                    int steps = diff / 2; // Giả sử mỗi nấc kéo là 2% âm lượng
                    for (int i = 0; i < steps; i++) send_socket_command("VOL_UP");
                } else if (diff < 0) {
                    int steps = Math.abs(diff) / 2;
                    for (int i = 0; i < steps; i++) send_socket_command("VOL_DOWN");
                }

                // Kéo xong nảy lò xo về giữa
                seekBar.setProgress(50);
            }
        });

        // Nút Mute
        findViewById(R.id.btn_mute).setOnClickListener(v -> send_socket_command("VOL_MUTE"));

        // --- CÁC NÚT NGUỒN ---
        findViewById(R.id.btn_off).setOnClickListener(v -> send_socket_command("OFF"));
        findViewById(R.id.btn_lock).setOnClickListener(v -> send_socket_command("LOCK"));
        findViewById(R.id.btn_sleep).setOnClickListener(v -> send_socket_command("SLEEP"));
        findViewById(R.id.btn_abort).setOnClickListener(v -> send_socket_command("ABORT"));
        findViewById(R.id.btn_timer).setOnClickListener(v -> show_timer_dialog());

        // Bật Task Manager
        findViewById(R.id.btn_open_monitor).setOnClickListener(v -> {
            android.content.Intent ans = new android.content.Intent(ControlActivity.this, MonitorActivity.class);
            ans.putExtra("IP_ADDRESS", targetip);
            startActivity(ans);
        });
    }

    private void show_timer_dialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Hẹn giờ tắt máy (Phút)");
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("HẸN GIỜ", (dialog, which) -> {
            String str = input.getText().toString().trim();
            if (!str.isEmpty()) {
                int ans = Integer.parseInt(str) * 60;
                send_socket_command("TIMER_" + ans);
            }
        });
        builder.setNegativeButton("HỦY", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void send_socket_command(String command) {
        new Thread(() -> {
            boolean ans = false;
            try (Socket socket = new Socket(targetip, 9999);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                out.println(command);
                ans = true;
            } catch (IOException e) {
                ans = false;
            }
            boolean finalans = ans;
            runOnUiThread(() -> {
                if (finalans) {
                    Toast.makeText(this, "Đã gửi lệnh thành công", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Lỗi kết nối tới " + targetip, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // Các hàm ép Font giống hệt Trang chủ
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