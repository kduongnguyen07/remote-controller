package com.example.remoteshutdown; // Chỉnh lại theo package của mày

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ControlActivity extends AppCompatActivity {

    private String targetip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ẩn thanh trạng thái (Pin, Giờ) cực mượt
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_control);

        targetip = getIntent().getStringExtra("IP_ADDRESS");
        String devicename = getIntent().getStringExtra("DEVICE_NAME");

        android.widget.TextView tvname = findViewById(R.id.tv_device_name);
        android.widget.TextView tvip = findViewById(R.id.tv_device_ip);
        tvname.setText(devicename);
        tvip.setText(targetip);

        // Bắt sự kiện mở 2 cái Popup
        findViewById(R.id.btn_pop_volume).setOnClickListener(v -> show_volume_popup());
        findViewById(R.id.btn_pop_power).setOnClickListener(v -> show_power_popup());

        // NỐI DÂY CHO NÚT BẬT TASK MANAGER ĐÂY MÀY
        findViewById(R.id.btn_open_monitor).setOnClickListener(v -> {
            android.content.Intent ans = new android.content.Intent(ControlActivity.this, MonitorActivity.class);
            ans.putExtra("IP_ADDRESS", targetip);
            startActivity(ans);
        });
    }

    // Hàm tạo Popup chứa danh sách Âm lượng
    private void show_volume_popup() {
        String[] res = {"Tăng âm lượng (+)", "Giảm âm lượng (-)", "Tắt tiếng (Mute)"};

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("ĐIỀU KHIỂN ÂM LƯỢNG");
        builder.setItems(res, (dialog, which) -> {
            if (which == 0) send_socket_command("VOL_UP");
            else if (which == 1) send_socket_command("VOL_DOWN");
            else if (which == 2) send_socket_command("VOL_MUTE");
        });
        builder.show();
    }

    // Hàm tạo Popup chứa danh sách Nguồn
    private void show_power_popup() {
        String[] ans = {"Khóa màn hình", "Ngủ (Sleep)", "Ngủ đông", "Hẹn giờ tắt máy", "Tắt máy NGAY!", "Hủy lệnh tắt"};

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("NGUỒN & HỆ THỐNG");
        builder.setItems(ans, (dialog, which) -> {
            if (which == 0) send_socket_command("LOCK");
            else if (which == 1) send_socket_command("SLEEP");
            else if (which == 2) send_socket_command("HIBERNATE");
            else if (which == 3) show_timer_dialog(); // Riêng thằng này lại mở thêm 1 Popup nhập số phút
            else if (which == 4) send_socket_command("OFF");
            else if (which == 5) send_socket_command("ABORT");
        });
        builder.show();
    }

    // Hộp thoại nhập số phút
    private void show_timer_dialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Hẹn giờ tắt máy (Phút)");
        builder.setMessage("Mày muốn tắt máy sau bao nhiêu phút nữa?");

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
                    android.widget.Toast.makeText(this, "Đã gửi lệnh: " + command, android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    android.widget.Toast.makeText(this, "Lỗi kết nối tới " + targetip, android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}