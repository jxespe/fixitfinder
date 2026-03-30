package com.example.fixitfinderapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.adapters.BookingHistoryAdapter;
import com.example.fixitfinderapp.models.BookingHistoryItem;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProviderHistoryFragment extends Fragment {

    private final List<BookingHistoryItem> items = new ArrayList<>();
    private BookingHistoryAdapter adapter;
    private TextView tvEmpty;
    private FirebaseUser user;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        user = FirebaseAuth.getInstance().getCurrentUser();

        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setVisibility(View.GONE);
        }
        TextView tvHeaderTitle = view.findViewById(R.id.tvHeaderTitle);
        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText("Bookings Received");
        }

        View tabBar = view.findViewById(R.id.layoutHistoryReviewTabs);
        if (tabBar != null) {
            tabBar.setVisibility(View.GONE);
        }

        RecyclerView recycler = view.findViewById(R.id.recyclerViewHistory);
        tvEmpty = view.findViewById(R.id.tvEmptyHistory);
        adapter = new BookingHistoryAdapter(items);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        loadHistory();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity() instanceof AppCompatActivity) {
            NavigationHelper.ensureLoggedIn((AppCompatActivity) getActivity());
        }
    }

    private void loadHistory() {
        if (user == null) {
            Toast.makeText(requireContext(), "Please log in again.", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("providerId", user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    items.clear();
                    snapshot.getDocuments().forEach(doc -> {
                        String bookingId = doc.getId();
                        String customerName = doc.getString("customerName");
                        String serviceName = doc.getString("serviceName");
                        String serviceDescription = doc.getString("serviceDescription");
                        if (TextUtils.isEmpty(serviceDescription)) {
                            serviceDescription = serviceName;
                        }
                        String priceText = formatPrice(doc.get("servicePrice"));
                        String title = !TextUtils.isEmpty(customerName) ? customerName :
                                (!TextUtils.isEmpty(serviceName) ? serviceName : "Customer Booking");

                        String status = displayStatus(doc.getString("status"));
                        String payment = displayPayment(doc.getString("paymentStatus"));
                        String logoUri = pickLogoUri(doc.getString("customerLogoUri"),
                                doc.getString("logoUri"));
                        String dateText = formatDate(doc.get("scheduledAt"), doc.get("createdAt"));
                        long sortTimestamp = pickSortTimestamp(doc.get("scheduledAt"), doc.get("createdAt"));
                        items.add(new BookingHistoryItem(
                                bookingId,
                                title,
                                dateText,
                                status,
                                payment,
                                logoUri,
                                "Job: " + valueOrUnknown(serviceDescription),
                                "Price: " + valueOrUnknown(priceText),
                                sortTimestamp
                        ));
                    });
                    items.sort((a, b) -> Long.compare(b.sortTimestamp, a.sortTimestamp));
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    tvEmpty.setText("Unable to load history: " + e.getMessage());
                    tvEmpty.setVisibility(View.VISIBLE);
                });
    }

    private String displayStatus(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return "Pending";
        }
        String normalized = raw.replace("_", " ").trim();
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private String displayPayment(String raw) {
        return TextUtils.isEmpty(raw) ? "On-hold" : raw;
    }

    private String pickLogoUri(String primary, String fallback) {
        if (!TextUtils.isEmpty(primary)) {
            return primary;
        }
        return !TextUtils.isEmpty(fallback) ? fallback : null;
    }

    private long pickSortTimestamp(Object scheduledAt, Object createdAt) {
        Date date = toDate(scheduledAt);
        if (date == null) {
            date = toDate(createdAt);
        }
        return date != null ? date.getTime() : 0L;
    }

    private String formatPrice(Object priceObj) {
        if (priceObj instanceof Number) {
            double price = ((Number) priceObj).doubleValue();
            if (price > 0) {
                return String.format(Locale.US, "\u20b1%.2f", price);
            }
        }
        return null;
    }

    private String valueOrUnknown(String value) {
        return TextUtils.isEmpty(value) ? "N/A" : value;
    }

    private String formatDate(Object scheduledAt, Object createdAt) {
        Date date = toDate(scheduledAt);
        if (date == null) {
            date = toDate(createdAt);
        }
        if (date == null) {
            return "Unknown date";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.US);
        return sdf.format(date);
    }

    private Date toDate(Object value) {
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate();
        }
        if (value instanceof Long) {
            return new Date((Long) value);
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        return null;
    }
}
