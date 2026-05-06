package com.example.remoteshutdown;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
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

    private final androidx.activity.result.ActivityResultLauncher<com.journeyapps.barcodescanner.ScanOptions> ans = registerForActivityResult(new com.journeyapps.barcodescanner.ScanContract(), res -> {
        if(res.getContents() != null) {
            try {
                JSONObject obj = new JSONObject(res.getContents());
                String ip = obj.getString("ip");
                String name = obj.getString("name");
                String mac = obj.optString("mac", "");

                Intent intent = new Intent(MainActivity.this, ControlActivity.class);
                intent.putExtra("IP_ADDRESS", ip);
                intent.putExtra("DEVICE_NAME", name);
                intent.putExtra("MAC_ADDRESS", mac);
                startActivity(intent);
                Toast.makeText(this, "Tóm được máy: " + name, Toast.LENGTH_SHORT).show();
            } catch(Exception e) {
                Toast.makeText(this, "Mã QR đéo hợp lệ!", Toast.LENGTH_SHORT).show();
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        rvdevices = findViewById(R.id.rv_devices);
        layouthome = findViewById(R.id.layout_home);
        layoutsettings = findViewById(R.id.layout_settings);
        imgmainbg = findViewById(R.id.img_main_bg);

        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        String savedBg = prefs.getString("bg_uri", null);
        String savedFont = prefs.getString("font_type", "SANS_SERIF");

        if (savedBg != null) {
            try { imgmainbg.setImageURI(Uri.parse(savedBg)); } catch (SecurityException e) {}
        }

        handler.postDelayed(() -> applyFontToView(getWindow().getDecorView().getRootView(), getFontType(savedFont)), 100);

        listdevices = new ArrayList<>();
        client = new OkHttpClient();

        adapter = new DeviceAdapter(listdevices, device -> {
            Intent intent = new Intent(MainActivity.this, ControlActivity.class);
            intent.putExtra("IP_ADDRESS", device.getip());
            intent.putExtra("DEVICE_NAME", device.getname());
            // CHUYỀN MAC MƯỢT MÀ TỪ DANH SÁCH FIREBASE
            intent.putExtra("MAC_ADDRESS", device.getmac());
            startActivity(intent);
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

        View btnscanqr = findViewById(R.id.btn_scan_qr);
        if (btnscanqr != null) {
            btnscanqr.setOnClickListener(v -> {
                com.journeyapps.barcodescanner.ScanOptions res = new com.journeyapps.barcodescanner.ScanOptions();
                res.setPrompt("Chĩa Camera vào mã QR trên màn hình Laptop");
                res.setBeepEnabled(true);
                res.setOrientationLocked(true);
                res.setCaptureActivity(com.journeyapps.barcodescanner.CaptureActivity.class);
                ans.launch(res);
            });
        }

        findViewById(R.id.btn_change_bg).setOnClickListener(v -> {
            Intent res = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            res.addCategory(Intent.CATEGORY_OPENABLE);
            res.setType("image/*");
            startActivityForResult(res, 100);
        });

        findViewById(R.id.btn_change_font).setOnClickListener(v -> {
            String[] options = {"Mặc định (Sang trọng)", "Có chân (Cổ điển)", "Monospace (Dân code)", "Cursive (Mềm mại)"};
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Chọn phong cách chữ")
                    .setItems(options, (dialog, which) -> {
                        String selected = "SANS_SERIF";
                        if (which == 1) selected = "SERIF";
                        else if (which == 2) selected = "MONOSPACE";
                        else if (which == 3) selected = "CURSIVE";
                        getSharedPreferences("AppConfig", MODE_PRIVATE).edit().putString("font_type", selected).apply();
                        applyFontToView(getWindow().getDecorView().getRootView(), getFontType(selected));
                        Toast.makeText(this, "Đã đổi Font chữ!", Toast.LENGTH_SHORT).show();
                    }).show();
        });
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri res = data.getData();
            if (res != null) {
                getContentResolver().takePersistableUriPermission(res, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
                prefs.edit().putString("bg_uri", res.toString()).apply();
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
                        String savedFont = getSharedPreferences("AppConfig", MODE_PRIVATE).getString("font_type", "SANS_SERIF");
                        applyFontToView(rvdevices, getFontType(savedFont));
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

                    // MOI MAC TỪ FIREBASE. NẾU TRONG DEVOBJ ĐÉO CÓ THÌ LẤY LUÔN CÁI KEY!
                    device.setmac(devobj.optString("mac", key));

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