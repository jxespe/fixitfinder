package com.example.fixitfinderapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.ImageLoader;
import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.models.ServiceProviderProfile;

import java.util.List;

public class ServiceProviderAdapter extends RecyclerView.Adapter<ServiceProviderAdapter.VH> {

    private final List<ServiceProviderProfile> providers;

    public ServiceProviderAdapter(List<ServiceProviderProfile> providers) {
        this.providers = providers;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service_provider, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ServiceProviderProfile provider = providers.get(position);
        String name = provider.fullName != null && !provider.fullName.isEmpty()
                ? provider.fullName
                : "Service Provider";
        String category = provider.serviceCategory != null && !provider.serviceCategory.isEmpty()
                ? provider.serviceCategory
                : "Service";
        String address = provider.address != null && !provider.address.isEmpty()
                ? provider.address
                : "Location not set";

        holder.name.setText(name);
        holder.type.setText(category);
        holder.location.setText(address);
        holder.price.setText("Contact for pricing");
        holder.rating.setRating(0f);
        holder.rating.setIsIndicator(true);
        ImageLoader.load(holder.logo, provider.logoUri, android.R.drawable.ic_menu_myplaces);
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent =
                    new android.content.Intent(v.getContext(),
                            com.example.fixitfinderapp.BookingActivity.class);
            intent.putExtra("providerId", provider.providerId);
            intent.putExtra("providerName", name);
            intent.putExtra("serviceCategory", category);
            intent.putExtra("address", address);
            intent.putExtra("logoUri", provider.logoUri);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return providers.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView logo;
        final TextView name;
        final TextView type;
        final TextView location;
        final TextView price;
        final RatingBar rating;

        VH(@NonNull View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.ivProviderLogo);
            name = itemView.findViewById(R.id.tvProviderName);
            type = itemView.findViewById(R.id.tvProviderType);
            location = itemView.findViewById(R.id.tvLocation);
            price = itemView.findViewById(R.id.tvPrice);
            rating = itemView.findViewById(R.id.ratingBar);
        }
    }
}
