package com.necrosid3.shutdown.service;

import com.necrosid3.shutdown.util.Config;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;

public class FirebaseService {

    private final OkHttpClient client;
    private final MediaType jsonmediatype;

    public FirebaseService() {
        this.client = new OkHttpClient();
        this.jsonmediatype = MediaType.parse("application/json; charset=utf-8");
    }

    public boolean updateDeviceInfo(String macaddress, String name, String ip, String status) {
        boolean ans = false;
        long currenttime = System.currentTimeMillis();

        JSONObject jsondata = new JSONObject();
        jsondata.put("name", name);
        jsondata.put("ip", ip);
        jsondata.put("status", status);
        jsondata.put("last_seen", currenttime);

        String url = Config.FIREBASE_URL + "/devices/" + macaddress + ".json";
        
        RequestBody body = RequestBody.create(jsondata.toString(), jsonmediatype);
        Request request = new Request.Builder()
                .url(url)
                .put(body) // Use PUT to write/overwrite data at this exact location
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ans = true;
            } else {
                System.err.println("failed to update firebase. code: " + response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return ans;
    }
}
