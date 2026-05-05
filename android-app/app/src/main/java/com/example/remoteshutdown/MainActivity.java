package com.example.remoteshutdown;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerview;
    private FloatingActionButton fab;
    private DeviceAdapter deviceadapter;
    private List<Device> listdevices;
    private OkHttpClient okhttpclient;
    private Handler handler;
    private Runnable runnable;
    private final String firebaseurl = "https://remote-desktop-control-73b66-default-rtdb.asia-southeast1.firebasedatabase.app/devices.json";

    @Override
    protected void onCreate(Bundle savedinstance) {
        super.onCreate(savedinstance);
        setContentView(R.layout.activity_main);

        recyclerview = findViewById(R.id.recyclerview_devices);
        fab = findViewById(R.id.fab_refresh);

        listdevices = new ArrayList<>();
        deviceadapter = new DeviceAdapter(listdevices);
        
        recyclerview.setLayoutManager(new LinearLayoutManager(this));
        recyclerview.setAdapter(deviceadapter);

        okhttpclient = new OkHttpClient();
        handler = new Handler(Looper.getMainLooper());

        runnable = new Runnable() {
            @Override
            public void run() {
                fetch_devices_from_firebase();
                handler.postDelayed(this, 10000);
            }
        };

        if (fab != null) {
            fab.setOnClickListener(view -> fetch_devices_from_firebase());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(runnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(runnable);
    }

    private void fetch_devices_from_firebase() {
        Request request = new Request.Builder()
                .url(firebaseurl)
                .build();

        okhttpclient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException exception) {
                Log.e("fetch_error", "failed to fetch data", exception);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsondata = response.body().string();
                        JSONObject jsonobject = new JSONObject(jsondata);
                        List<Device> newlist = new ArrayList<>();
                        
                        Iterator<String> keys = jsonobject.keys();
                        while (keys.hasNext()) {
                            String macaddress = keys.next();
                            JSONObject deviceobject = jsonobject.getJSONObject(macaddress);

                            String name = deviceobject.optString("name", "Unknown");
                            String ip = deviceobject.optString("ip", "0.0.0.0");
                            String status = deviceobject.optString("status", "OFFLINE");
                            long lastseen = deviceobject.optLong("last_seen", 0);

                            Device device = new Device(macaddress, name, ip, status, lastseen);
                            newlist.add(device);
                        }

                        runOnUiThread(() -> {
                            listdevices.clear();
                            listdevices.addAll(newlist);
                            deviceadapter.notifyDataSetChanged();
                        });

                    } catch (Exception exception) {
                        Log.e("parse_error", "json parsing error", exception);
                    }
                }
            }
        });
    }
}
