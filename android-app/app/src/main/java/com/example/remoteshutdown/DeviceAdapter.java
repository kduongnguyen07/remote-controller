package com.example.remoteshutdown;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<Device> listdevices;

    public DeviceAdapter(List<Device> listdevices) {
        this.listdevices = listdevices;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewtype) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        DeviceViewHolder ans = new DeviceViewHolder(view);
        return ans;
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = listdevices.get(position);
        holder.textviewname.setText(device.get_name());
        holder.textviewip.setText(device.get_ip());

        GradientDrawable res = new GradientDrawable();
        res.setShape(GradientDrawable.OVAL);
        
        if ("ONLINE".equals(device.get_status())) {
            res.setColor(Color.parseColor("#4CAF50")); // Xanh lÃ¡
        } else {
            res.setColor(Color.parseColor("#9E9E9E")); // XÃ¡m
        }
        holder.imageviewstatus.setImageDrawable(res);
    }

    @Override
    public int getItemCount() {
        int ans = listdevices.size();
        return ans;
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        public TextView textviewname;
        public TextView textviewip;
        public ImageView imageviewstatus;

        public DeviceViewHolder(@NonNull View itemview) {
            super(itemview);
            textviewname = itemview.findViewById(R.id.textview_name);
            textviewip = itemview.findViewById(R.id.textview_ip);
            imageviewstatus = itemview.findViewById(R.id.imageview_status);
        }
    }
}
