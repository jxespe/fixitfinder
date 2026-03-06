package com.example.fixitfinderapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.models.AppNotificationItem;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    private final List<AppNotificationItem> items;
    private final SimpleDateFormat formatter =
            new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

    public NotificationAdapter(List<AppNotificationItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        AppNotificationItem item = items.get(position);
        holder.title.setText(item.title);
        holder.message.setText(item.message);
        if (item.timestamp > 0) {
            holder.time.setText(formatter.format(new java.util.Date(item.timestamp)));
        } else {
            holder.time.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView message;
        final TextView time;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvNotificationTitle);
            message = itemView.findViewById(R.id.tvNotificationMessage);
            time = itemView.findViewById(R.id.tvNotificationTime);
        }
    }
}
