package com.example.fixitfinderapp.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.ImageLoader;
import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.models.ProviderServiceItem;

import java.util.List;
import java.util.Locale;

public class ProviderServiceAdapter extends RecyclerView.Adapter<ProviderServiceAdapter.VH> {

    public interface OnServiceClickListener {
        void onServiceClick(ProviderServiceItem item);
    }

    private final List<ProviderServiceItem> services;
    private final boolean editable;
    private final boolean selectable;
    private final OnServiceClickListener listener;
    private String selectedId;

    public ProviderServiceAdapter(List<ProviderServiceItem> services,
                                  boolean editable,
                                  boolean selectable,
                                  @Nullable OnServiceClickListener listener) {
        this.services = services;
        this.editable = editable;
        this.selectable = selectable;
        this.listener = listener;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_provider_service, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        ProviderServiceItem item = services.get(position);
        holder.name.setText(!TextUtils.isEmpty(item.name) ? item.name : "Service");
        holder.price.setText(formatPrice(item.price));
        ImageLoader.load(holder.image, item.imageUri, android.R.drawable.ic_menu_gallery);
        if (selectable) {
            boolean selected = item.id != null && item.id.equals(selectedId);
            holder.itemView.setBackgroundResource(selected
                    ? R.drawable.bg_service_card_selected
                    : R.drawable.bg_service_card);
        }
        if (listener != null) {
            holder.itemView.setOnClickListener(v -> listener.onServiceClick(item));
        } else {
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return services == null ? 0 : services.size();
    }

    public void setSelectedId(@Nullable String selectedId) {
        this.selectedId = selectedId;
        notifyDataSetChanged();
    }

    private String formatPrice(double price) {
        if (price <= 0) {
            return "Price not set";
        }
        return String.format(Locale.US, "From \u20b1%.2f", price);
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name;
        final TextView price;

        VH(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.ivServiceImage);
            name = itemView.findViewById(R.id.tvServiceName);
            price = itemView.findViewById(R.id.tvServicePrice);
        }
    }
}
