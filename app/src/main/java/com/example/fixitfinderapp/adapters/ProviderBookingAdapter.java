package com.example.fixitfinderapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.models.ProviderBookingItem;

import java.util.List;

public class ProviderBookingAdapter extends RecyclerView.Adapter<ProviderBookingAdapter.VH> {

    private final List<ProviderBookingItem> items;
    private final boolean showActions;
    private final BookingActionListener actionListener;

    public ProviderBookingAdapter(List<ProviderBookingItem> items,
                                  boolean showActions,
                                  BookingActionListener actionListener) {
        this.items = items;
        this.showActions = showActions;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_provider_booking, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ProviderBookingItem item = items.get(position);
        holder.tvBookingNumber.setText(item.bookingNumber);
        holder.tvBookedBy.setText(item.bookedBy);
        holder.tvBookedAt.setText(item.bookedAt);
        holder.tvRequiredAt.setText(item.requiredAt);
        holder.tvLocation.setText(item.location);
        holder.layoutActions.setVisibility(showActions ? View.VISIBLE : View.GONE);
        if (showActions) {
            holder.btnAccept.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onAccept(item);
                }
            });
            holder.btnDecline.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onDecline(item);
                }
            });
        } else {
            holder.btnAccept.setOnClickListener(null);
            holder.btnDecline.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvBookingNumber;
        final TextView tvBookedBy;
        final TextView tvBookedAt;
        final TextView tvRequiredAt;
        final TextView tvLocation;
        final View layoutActions;
        final android.widget.Button btnAccept;
        final android.widget.Button btnDecline;

        VH(@NonNull View itemView) {
            super(itemView);
            tvBookingNumber = itemView.findViewById(R.id.tvBookingNumber);
            tvBookedBy = itemView.findViewById(R.id.tvBookedBy);
            tvBookedAt = itemView.findViewById(R.id.tvBookedAt);
            tvRequiredAt = itemView.findViewById(R.id.tvRequiredAt);
            tvLocation = itemView.findViewById(R.id.tvBookingLocation);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }

    public interface BookingActionListener {
        void onAccept(ProviderBookingItem item);
        void onDecline(ProviderBookingItem item);
    }
}
