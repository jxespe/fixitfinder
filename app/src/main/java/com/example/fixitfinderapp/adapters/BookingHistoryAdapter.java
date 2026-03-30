package com.example.fixitfinderapp.adapters;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.ImageLoader;
import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.models.BookingHistoryItem;

import java.util.List;

public class BookingHistoryAdapter extends RecyclerView.Adapter<BookingHistoryAdapter.VH> {

    private final List<BookingHistoryItem> items;

    public BookingHistoryAdapter(List<BookingHistoryItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking_history, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        BookingHistoryItem item = items.get(position);
        holder.title.setText(item.title);
        holder.date.setText(item.dateText);
        holder.status.setText(item.status);
        holder.payment.setText(item.paymentStatus);
        holder.description.setText(item.description);
        holder.price.setText(item.priceText);

        holder.status.setTextColor(statusColor(item.status));
        holder.payment.setTextColor(statusColor(item.status));

        ImageLoader.loadProfile(holder.logo, item.logoUri, android.R.drawable.ic_menu_myplaces);
        holder.itemView.setOnClickListener(v -> {
            if (item.bookingId == null || item.bookingId.trim().isEmpty()) {
                return;
            }
            android.content.Intent intent = new android.content.Intent();
            intent.setClassName(v.getContext(), "com.example.fixitfinderapp.BookingTimelineActivity");
            intent.putExtra("bookingId", item.bookingId);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private int statusColor(String status) {
        if (status == null) {
            return Color.parseColor("#757575");
        }
        String normalized = status.toLowerCase();
        if (normalized.contains("pending")) {
            return Color.parseColor("#FF9800");
        }
        if (normalized.contains("process") || normalized.contains("ongoing")) {
            return Color.parseColor("#2196F3");
        }
        if (normalized.contains("cancel")) {
            return Color.parseColor("#F44336");
        }
        if (normalized.contains("finish") || normalized.contains("done")) {
            return Color.parseColor("#4CAF50");
        }
        return Color.parseColor("#757575");
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView logo;
        final TextView title;
        final TextView date;
        final TextView status;
        final TextView payment;
        final TextView description;
        final TextView price;

        VH(@NonNull View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.ivProviderLogo);
            title = itemView.findViewById(R.id.tvProviderName);
            date = itemView.findViewById(R.id.tvBookingDate);
            status = itemView.findViewById(R.id.tvStatus);
            payment = itemView.findViewById(R.id.tvPayment);
            description = itemView.findViewById(R.id.tvServiceDescription);
            price = itemView.findViewById(R.id.tvServicePrice);
        }
    }
}
