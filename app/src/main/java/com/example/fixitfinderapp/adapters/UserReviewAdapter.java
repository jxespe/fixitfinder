package com.example.fixitfinderapp.adapters;

import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.BookingActivity;
import com.example.fixitfinderapp.ImageLoader;
import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.models.UserReviewItem;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserReviewAdapter extends RecyclerView.Adapter<UserReviewAdapter.VH> {

    private final List<UserReviewItem> items;

    public UserReviewAdapter(List<UserReviewItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_review, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        UserReviewItem item = items.get(position);
        holder.providerName.setText(item.providerName);
        holder.ratingStars.setText(stars(item.rating));
        String body = item.reviewText;
        if (TextUtils.isEmpty(body)) {
            body = "You rated this booking — add a short comment next time when you leave a review.";
        }
        holder.reviewText.setText(body);
        ImageLoader.loadProfile(holder.logo, item.logoUri, android.R.drawable.ic_menu_myplaces);

        boolean canBook = !TextUtils.isEmpty(item.providerId);
        holder.btnBookAgain.setEnabled(canBook);
        holder.btnAddToFavorites.setEnabled(canBook);

        holder.btnBookAgain.setOnClickListener(v -> {
            if (TextUtils.isEmpty(item.providerId)) {
                Toast.makeText(v.getContext(), "Provider unavailable.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(v.getContext(), BookingActivity.class);
            intent.putExtra("providerId", item.providerId);
            intent.putExtra("providerName", item.providerName);
            intent.putExtra("serviceCategory",
                    !TextUtils.isEmpty(item.serviceCategory) ? item.serviceCategory : "Service");
            intent.putExtra("address",
                    !TextUtils.isEmpty(item.providerAddress) ? item.providerAddress : "");
            intent.putExtra("logoUri", item.logoUri != null ? item.logoUri : "");
            v.getContext().startActivity(intent);
        });

        holder.btnAddToFavorites.setOnClickListener(v -> {
            if (TextUtils.isEmpty(item.providerId)) {
                Toast.makeText(v.getContext(), "Provider unavailable.", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(v.getContext(), "Please log in again.", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> updates = new HashMap<>();
            updates.put("favoriteProviderIds", FieldValue.arrayUnion(item.providerId));
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .set(updates, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(unused ->
                            Toast.makeText(v.getContext(), "Added to favorites.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(v.getContext(), "Could not update favorites.", Toast.LENGTH_SHORT).show());
        });
    }

    private static String stars(int rating) {
        int n = Math.max(0, Math.min(5, rating));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append('\u2605');
        }
        for (int i = n; i < 5; i++) {
            sb.append('\u2606');
        }
        return sb.toString();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView logo;
        final TextView providerName;
        final TextView ratingStars;
        final TextView reviewText;
        final MaterialButton btnBookAgain;
        final MaterialButton btnAddToFavorites;

        VH(@NonNull View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.ivProviderLogo);
            providerName = itemView.findViewById(R.id.tvProviderName);
            ratingStars = itemView.findViewById(R.id.tvRatingStars);
            reviewText = itemView.findViewById(R.id.tvReviewText);
            btnBookAgain = itemView.findViewById(R.id.btnBookAgain);
            btnAddToFavorites = itemView.findViewById(R.id.btnAddToFavorites);
            int primary = ContextCompat.getColor(itemView.getContext(), R.color.color_primary);
            btnBookAgain.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary));
            btnAddToFavorites.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary));
        }
    }
}
