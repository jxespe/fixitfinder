package com.example.fixitfinderapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.models.TimelineEntry;

import java.util.List;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.VH> {

    private final List<TimelineEntry> items;

    public TimelineAdapter(List<TimelineEntry> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeline_entry, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TimelineEntry entry = items.get(position);
        holder.label.setText(entry.label);
        holder.time.setText(entry.time);
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView label;
        final TextView time;

        VH(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.tvTimelineLabel);
            time = itemView.findViewById(R.id.tvTimelineTime);
        }
    }
}
