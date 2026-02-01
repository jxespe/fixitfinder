package com.example.fixitfinderapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.models.ProviderEarningItem;

import java.util.List;

public class ProviderEarningsAdapter extends RecyclerView.Adapter<ProviderEarningsAdapter.VH> {

    private final List<ProviderEarningItem> items;

    public ProviderEarningsAdapter(List<ProviderEarningItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_provider_earning, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ProviderEarningItem item = items.get(position);
        holder.tvBookingNumber.setText(item.bookingNumber);
        holder.tvPaidBy.setText(item.paidBy);
        holder.tvPaidAt.setText(item.paidAt);
        holder.tvJobDone.setText(item.jobDone);
        holder.tvPaymentMethod.setText(item.paymentMethod);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvBookingNumber;
        final TextView tvPaidBy;
        final TextView tvPaidAt;
        final TextView tvJobDone;
        final TextView tvPaymentMethod;

        VH(@NonNull View itemView) {
            super(itemView);
            tvBookingNumber = itemView.findViewById(R.id.tvBookingNumber);
            tvPaidBy = itemView.findViewById(R.id.tvPaidBy);
            tvPaidAt = itemView.findViewById(R.id.tvPaidAt);
            tvJobDone = itemView.findViewById(R.id.tvJobDone);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
        }
    }
}
