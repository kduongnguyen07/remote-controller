package com.example.remoteshutdown; // Đổi nếu package mày khác nhé

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import org.json.JSONObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvdevices;
    private DeviceAdapter adapter;
    private List<Device> listdevices;
    private OkHttpClient client;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable fetchrunnable;

    // Link Firebase chuẩn của mày cộng thêm /devices.json để lấy full list
    private final String firebaseurl = "https://remote-desktop-control-73b66-default-rtdb.asia-southeast1.firebasedatabase.app/devices.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        rvdevices = findViewById(R.id.rv_devices);
        listdevices = new ArrayList<>();
        client = new OkHttpClient();

        adapter = new DeviceAdapter(listdevices, new DeviceAdapter.ondeviceclicklistener() {
            @Override
            public void ondeviceclick(Device device) {
                // Trang 1 đẩy data sang Trang 2
                android.content.Intent res = new android.content.Intent(MainActivity.this, ControlActivity.class);
                res.putExtra("IP_ADDRESS", device.getip());
                res.putExtra("DEVICE_NAME", device.getname());
                startActivity(res);
            }
        });

        rvdevices.setLayoutManager(new LinearLayoutManager(this));
        rvdevices.setAdapter(adapter);

        // Vòng lặp 5 giây quét Firebase 1 lần
        fetchrunnable = new Runnable() {
            @Override
            public void run() {
                fetch_devices_from_firebase();
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(fetchrunnable);
    }

    private void fetch_devices_from_firebase() {
        Request request = new Request.Builder().url(firebaseurl).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Không log gì ra UI để đỡ phiền nếu mạng lag
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responsedata = response.body().string();
                    List<Device> newlist = parse_json_to_list(responsedata);
                    runOnUiThread(() -> {
                        listdevices.clear();
                        listdevices.addAll(newlist);
                        adapter.notifyDataSetChanged();
                    });
                }
            }
        });
    }

    private List<Device> parse_json_to_list(String json) {
        List<Device> ans = new ArrayList<>();
        try {
            if (json != null && !json.equals("null")) {
                JSONObject jsonobject = new JSONObject(json);
                Iterator<String> keys = jsonobject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject devobj = jsonobject.getJSONObject(key);
                    Device device = new Device();
                    device.setname(devobj.optString("name", "Unknown"));
                    device.setip(devobj.optString("ip", ""));
                    device.setstatus(devobj.optString("status", "OFFLINE"));
                    device.setlastseen(devobj.optLong("last_seen", 0));
                    ans.add(device);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ans;
    }

    // Hiển thị Bottom Sheet hiện đại
    // Hiển thị Bottom Sheet hiện đại nhiều nút
    private void show_control_panel(Device device) {
        com.google.android.material.bottomsheet.BottomSheetDialog res = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.layout_bottom_sheet, null);

        android.widget.TextView tvname = view.findViewById(R.id.tv_sheet_name);
        android.widget.TextView tvip = view.findViewById(R.id.tv_sheet_ip);
        tvname.setText(device.getname());
        tvip.setText(device.getip());

        // Khai báo các nút Cụm Âm Lượng
        android.widget.Button btnvoldown = view.findViewById(R.id.btn_vol_down);
        android.widget.Button btnvolmute = view.findViewById(R.id.btn_vol_mute);
        android.widget.Button btnvolup = view.findViewById(R.id.btn_vol_up);

        // Khai báo các nút Cụm Nguồn
        android.widget.Button btnlock = view.findViewById(R.id.btn_lock);
        android.widget.Button btnsleep = view.findViewById(R.id.btn_sleep);
        android.widget.Button btnhibernate = view.findViewById(R.id.btn_hibernate);
        android.widget.Button btnshutdown = view.findViewById(R.id.btn_shutdown);
        android.widget.Button btnabort = view.findViewById(R.id.btn_abort);

        // Khai báo Cụm Thông báo
        android.widget.EditText etmessage = view.findViewById(R.id.et_message);
        android.widget.Button btnsendmsg = view.findViewById(R.id.btn_send_msg);

        // --- Bắt sự kiện bắn Socket ---
        btnvoldown.setOnClickListener(v -> send_socket_command(device.getip(), "VOL_DOWN"));
        btnvolmute.setOnClickListener(v -> send_socket_command(device.getip(), "VOL_MUTE"));
        btnvolup.setOnClickListener(v -> send_socket_command(device.getip(), "VOL_UP"));

        btnlock.setOnClickListener(v -> { send_socket_command(device.getip(), "LOCK"); res.dismiss(); });
        btnsleep.setOnClickListener(v -> { send_socket_command(device.getip(), "SLEEP"); res.dismiss(); });
        btnhibernate.setOnClickListener(v -> { send_socket_command(device.getip(), "HIBERNATE"); res.dismiss(); });
        btnshutdown.setOnClickListener(v -> { send_socket_command(device.getip(), "OFF"); res.dismiss(); });
        btnabort.setOnClickListener(v -> send_socket_command(device.getip(), "ABORT"));

        btnsendmsg.setOnClickListener(v -> {
            String msg = etmessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                send_socket_command(device.getip(), "MSG_" + msg);
                etmessage.setText("");
            } else {
                android.widget.Toast.makeText(this, "Viết gì đi chứ!", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        res.setContentView(view);
        res.show();
    }

    // Đâm Socket gửi mật mã tuỳ chỉnh
    private void send_socket_command(String ipaddress, String command) {
        new Thread(() -> {
            boolean ans = false;
            try (Socket socket = new Socket(ipaddress, 9999);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                out.println(command); // Bắn lệnh đi
                ans = true;
            } catch (IOException e) {
                ans = false;
            }

            boolean finalans = ans;
            runOnUiThread(() -> {
                if (finalans) {
                    Toast.makeText(this, "Đã gửi lệnh: " + command, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Lỗi kết nối! Kiểm tra lại tường lửa Laptop.", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // Đâm Socket chọt thẳng vào Server Laptop
    private void send_shutdown_command(String ipaddress) {
        new Thread(() -> {
            boolean ans = false;
            try (Socket socket = new Socket(ipaddress, 9999);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                out.println("OFF"); // Câu thần chú tắt nguồn
                ans = true;
            } catch (IOException e) {
                ans = false;
            }

            boolean finalans = ans;
            runOnUiThread(() -> {
                if (finalans) {
                    Toast.makeText(this, "Đã gửi lệnh tắt máy thành công!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Lỗi kết nối! Kiểm tra lại tường lửa Laptop.", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(fetchrunnable); // Xóa thread chống rò rỉ RAM
    }
}