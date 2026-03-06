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
    private final String mode;
    private final BookingActionListener actionListener;

    public ProviderBookingAdapter(List<ProviderBookingItem> items,
                                  String mode,
                                  BookingActionListener actionListener) {
        this.items = items;
        this.mode = mode;
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
        holder.tvJobDescription.setText(item.jobDescription);
        holder.tvJobPrice.setText(item.priceText);
        holder.tvLocation.setText(item.location);
        holder.tvStatus.setText("Status: " + formatStatus(item.status));

        boolean isPending = "pending".equalsIgnoreCase(mode);
        boolean isUpcoming = "upcoming".equalsIgnoreCase(mode);
        holder.layoutActions.setVisibility(isPending ? View.VISIBLE : View.GONE);
        holder.layoutProgressActions.setVisibility(isUpcoming ? View.VISIBLE : View.GONE);
        holder.layoutFinalActions.setVisibility(isUpcoming ? View.VISIBLE : View.GONE);

        if (isPending) {
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

        if (isUpcoming) {
            holder.btnProcess.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onProcess(item);
                }
            });
            holder.btnFinish.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onFinish(item);
                }
            });
            holder.btnCancel.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onCancel(item);
                }
            });
            holder.btnReschedule.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onReschedule(item);
                }
            });
            applyUpcomingActionVisibility(holder, item.status);
        } else {
            holder.btnProcess.setOnClickListener(null);
            holder.btnFinish.setOnClickListener(null);
            holder.btnCancel.setOnClickListener(null);
            holder.btnReschedule.setOnClickListener(null);
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
        final TextView tvJobDescription;
        final TextView tvJobPrice;
        final TextView tvLocation;
        final TextView tvStatus;
        final View layoutActions;
        final View layoutProgressActions;
        final View layoutFinalActions;
        final android.widget.Button btnAccept;
        final android.widget.Button btnDecline;
        final android.widget.Button btnProcess;
        final android.widget.Button btnFinish;
        final android.widget.Button btnCancel;
        final android.widget.Button btnReschedule;

        VH(@NonNull View itemView) {
            super(itemView);
            tvBookingNumber = itemView.findViewById(R.id.tvBookingNumber);
            tvBookedBy = itemView.findViewById(R.id.tvBookedBy);
            tvBookedAt = itemView.findViewById(R.id.tvBookedAt);
            tvRequiredAt = itemView.findViewById(R.id.tvRequiredAt);
            tvJobDescription = itemView.findViewById(R.id.tvJobDescription);
            tvJobPrice = itemView.findViewById(R.id.tvJobPrice);
            tvLocation = itemView.findViewById(R.id.tvBookingLocation);
            tvStatus = itemView.findViewById(R.id.tvBookingStatus);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            layoutProgressActions = itemView.findViewById(R.id.layoutProgressActions);
            layoutFinalActions = itemView.findViewById(R.id.layoutFinalActions);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
            btnProcess = itemView.findViewById(R.id.btnProcess);
            btnFinish = itemView.findViewById(R.id.btnFinish);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnReschedule = itemView.findViewById(R.id.btnReschedule);
        }
    }

    public interface BookingActionListener {
        void onAccept(ProviderBookingItem item);
        void onDecline(ProviderBookingItem item);
        void onProcess(ProviderBookingItem item);
        void onFinish(ProviderBookingItem item);
        void onCancel(ProviderBookingItem item);
        void onReschedule(ProviderBookingItem item);
    }

    private String formatStatus(String status) {
        if (status == null) {
            return "Pending";
        }
        String trimmed = status.trim();
        if (trimmed.equalsIgnoreCase("on-process")) {
            return "On Process";
        }
        if (trimmed.equalsIgnoreCase("ongoing")) {
            return "On Process";
        }
        if (trimmed.equalsIgnoreCase("accepted")) {
            return "Accepted";
        }
        if (trimmed.equalsIgnoreCase("declined")) {
            return "Declined";
        }
        if (trimmed.equalsIgnoreCase("cancelled")) {
            return "Cancelled";
        }
        if (trimmed.equalsIgnoreCase("rescheduled")) {
            return "Rescheduled";
        }
        if (trimmed.equalsIgnoreCase("finished")) {
            return "Finished";
        }
        if (trimmed.equalsIgnoreCase("pending")) {
            return "Pending";
        }
        return trimmed;
    }

    private boolean isInProcess(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toLowerCase(java.util.Locale.US);
        return normalized.equals("on process")
                || normalized.equals("on-process")
                || normalized.equals("ongoing");
    }

    private boolean isAccepted(String status) {
        if (status == null) {
            return false;
        }
        return status.trim().equalsIgnoreCase("accepted");
    }

    private void applyUpcomingActionVisibility(VH holder, String status) {
        boolean accepted = isAccepted(status);
        boolean inProcess = isInProcess(status);

        holder.btnProcess.setVisibility(accepted ? View.VISIBLE : View.GONE);
        holder.btnReschedule.setVisibility(accepted ? View.VISIBLE : View.GONE);
        holder.btnFinish.setVisibility(inProcess ? View.VISIBLE : View.GONE);
        holder.btnCancel.setVisibility(View.GONE);

        boolean showProgressRow = accepted || inProcess;
        boolean showFinalRow = accepted;
        holder.layoutProgressActions.setVisibility(showProgressRow ? View.VISIBLE : View.GONE);
        holder.layoutFinalActions.setVisibility(showFinalRow ? View.VISIBLE : View.GONE);
    }
}
