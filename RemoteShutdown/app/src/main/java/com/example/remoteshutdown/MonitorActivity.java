package com.example.remoteshutdown;

import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import org.json.JSONObject;

public class MonitorActivity extends AppCompatActivity {

    private String targetip;
    private boolean isrunning = true;
    private com.github.mikephil.charting.charts.LineChart chartcpu;
    private ArrayList<Entry> cpuentries = new ArrayList<>();
    private int timeindex = 0;

    // Các text view thông số
    private android.widget.TextView tvcpuname, tvutil, tvprocs, tvram, tvthreads, tvuptime, tvgpu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_monitor);

        targetip = getIntent().getStringExtra("IP_ADDRESS");

        tvcpuname = findViewById(R.id.tv_cpu_name);
        tvutil = findViewById(R.id.tv_util);
        tvprocs = findViewById(R.id.tv_procs);
        tvram = findViewById(R.id.tv_ram);
        tvthreads = findViewById(R.id.tv_threads);
        tvuptime = findViewById(R.id.tv_uptime);
        tvgpu = findViewById(R.id.tv_gpu);
        chartcpu = findViewById(R.id.chart_cpu);

        setup_task_manager_chart(chartcpu);
        start_monitoring();
    }

    private void setup_task_manager_chart(com.github.mikephil.charting.charts.LineChart chart) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);

        // Cấu hình trục X (Lưới dọc)
        chart.getXAxis().setDrawLabels(false);
        chart.getXAxis().setDrawGridLines(true);
        chart.getXAxis().setGridColor(Color.parseColor("#333333"));
        chart.getXAxis().setGridLineWidth(1f);

        // Cấu hình trục Y (Lưới ngang)
        chart.getAxisLeft().setTextColor(Color.parseColor("#888888"));
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisLeft().setGridColor(Color.parseColor("#333333"));
        chart.getAxisLeft().setGridLineWidth(1f);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setAxisMaximum(100f);

        // Vẽ đường sóng màu xanh Task Manager
        LineDataSet dataset = new LineDataSet(new ArrayList<>(), "");
        dataset.setColor(Color.parseColor("#00A4EF")); // Xanh lơ Win 11
        dataset.setDrawCircles(false);
        dataset.setLineWidth(1.5f);
        dataset.setDrawFilled(true);
        dataset.setFillColor(Color.parseColor("#00A4EF"));
        dataset.setFillAlpha(50);
        dataset.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        chart.setData(new LineData(dataset));
    }

    private void start_monitoring() {
        new Thread(() -> {
            while (isrunning) {
                try (Socket socket = new Socket(targetip, 9999);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    out.println("GET_STATS");
                    String res = in.readLine();

                    if (res != null && !res.contains("error")) {
                        JSONObject ans = new JSONObject(res);

                        String cpuname = ans.optString("cpu_name", "Unknown CPU");
                        float cpuload = (float) ans.optDouble("cpu_util", 0.0);
                        long ramused = ans.optLong("ram_used", 0);
                        long ramtotal = ans.optLong("ram_total", 0);
                        int procs = ans.optInt("processes", 0);
                        int threads = ans.optInt("threads", 0);
                        String uptime = ans.optString("uptime", "0:00:00");
                        int gpuutil = ans.optInt("gpu_util", 0);
                        int gputemp = ans.optInt("gpu_temp", 0);

                        runOnUiThread(() -> {
                            tvcpuname.setText(cpuname);
                            tvutil.setText(String.format("%.0f%%", cpuload));
                            tvram.setText(ramused + " / " + ramtotal + " GB");
                            tvprocs.setText(String.valueOf(procs));
                            tvthreads.setText(String.valueOf(threads));
                            tvuptime.setText(uptime);
                            tvgpu.setText(gpuutil + "% - " + gputemp + "°C");

                            // Trượt đồ thị
                            cpuentries.add(new Entry(timeindex++, cpuload));
                            if (cpuentries.size() > 60) cpuentries.remove(0); // 60 giây trượt

                            LineDataSet dataset = (LineDataSet) chartcpu.getData().getDataSetByIndex(0);
                            dataset.setValues(cpuentries);
                            chartcpu.getData().notifyDataChanged();
                            chartcpu.notifyDataSetChanged();
                            chartcpu.invalidate();
                        });
                    }
                } catch (Exception e) {}
                try { Thread.sleep(1000); } catch (Exception e) {}
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isrunning = false;
    }
}