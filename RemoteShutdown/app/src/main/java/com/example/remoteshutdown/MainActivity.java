package com.example.remoteshutdown;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONObject;
import java.io.IOException;
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

    private final String firebaseurl = "https://remote-desktop-control-73b66-default-rtdb.asia-southeast1.firebasedatabase.app/devices.json";

    private LinearLayout layouthome, layoutsettings;
    private ImageView imgmainbg;
    private boolean ismonospace = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        rvdevices = findViewById(R.id.rv_devices);
        layouthome = findViewById(R.id.layout_home);
        layoutsettings = findViewById(R.id.layout_settings);
        imgmainbg = findViewById(R.id.img_main_bg);

        // --- KHÚC NÀY TỰ ĐỘNG LOAD LẠI ẢNH NỀN KHI MỞ APP ---
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        String savedBg = prefs.getString("bg_uri", null);
        if (savedBg != null) {
            try {
                imgmainbg.setImageURI(Uri.parse(savedBg));
            } catch (SecurityException e) {
                // Lỡ ảnh bị xóa khỏi máy thì bỏ qua
            }
        }

        listdevices = new ArrayList<>();
        client = new OkHttpClient();

        adapter = new DeviceAdapter(listdevices, device -> {
            Intent ans = new Intent(MainActivity.this, ControlActivity.class);
            ans.putExtra("IP_ADDRESS", device.getip());
            ans.putExtra("DEVICE_NAME", device.getname());
            startActivity(ans);
        });

        rvdevices.setLayoutManager(new LinearLayoutManager(this));
        rvdevices.setAdapter(adapter);

        fetchrunnable = new Runnable() {
            @Override
            public void run() {
                fetch_devices_from_firebase();
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(fetchrunnable);

        com.google.android.material.bottomnavigation.BottomNavigationView bottomnav = findViewById(R.id.bottom_nav);
        bottomnav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                layouthome.setVisibility(View.VISIBLE);
                layoutsettings.setVisibility(View.GONE);
                return true;
            } else if (id == R.id.nav_settings) {
                layouthome.setVisibility(View.GONE);
                layoutsettings.setVisibility(View.VISIBLE);
                return true;
            }
            return false;
        });

        // --- NÚT ĐỔI NỀN ĐÃ ĐƯỢC NÂNG CẤP LẤY QUYỀN ---
        findViewById(R.id.btn_change_bg).setOnClickListener(v -> {
            Intent res = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            res.addCategory(Intent.CATEGORY_OPENABLE);
            res.setType("image/*");
            startActivityForResult(res, 100);
        });

        findViewById(R.id.btn_change_font).setOnClickListener(v -> {
            ismonospace = !ismonospace;
            android.graphics.Typeface newfont = ismonospace ? android.graphics.Typeface.MONOSPACE : android.graphics.Typeface.DEFAULT_BOLD;
            ((TextView) findViewById(R.id.tv_title_home)).setTypeface(newfont);
            ((TextView) findViewById(R.id.tv_title_settings)).setTypeface(newfont);
            Toast.makeText(this, "Đã đổi Font chữ!", Toast.LENGTH_SHORT).show();
        });
    }

    // --- XỬ LÝ LƯU ẢNH NỀN VĨNH VIỄN VÀO BỘ NHỚ ---
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri res = data.getData();
            if (res != null) {
                // Xin Android cấp quyền truy cập cái ảnh này mãi mãi
                getContentResolver().takePersistableUriPermission(res, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // Lưu đường dẫn ảnh vào sổ tay SharedPreferences
                SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
                prefs.edit().putString("bg_uri", res.toString()).apply();

                // Set ảnh lên màn hình luôn
                imgmainbg.setImageURI(res);
            }
        }
    }

    private void fetch_devices_from_firebase() {
        Request request = new Request.Builder().url(firebaseurl).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

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
        } catch (Exception e) {}
        return ans;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(fetchrunnable);
    }
}