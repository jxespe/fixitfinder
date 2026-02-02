package com.example.fixitfinderapp.adapters;

import android.net.Uri;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.models.ConversationItem;

import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.VH> {

    public interface ConversationClickListener {
        void onConversationClick(ConversationItem item);
    }

    private final List<ConversationItem> items;
    private final ConversationClickListener listener;

    public ConversationAdapter(List<ConversationItem> items, ConversationClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ConversationItem item = items.get(position);
        holder.tvName.setText(item.name);
        holder.tvPreview.setText(item.preview);
        boolean unread = item.unreadCount > 0;
        holder.tvName.setTypeface(null, unread ? Typeface.BOLD : Typeface.NORMAL);
        holder.tvPreview.setTypeface(null, unread ? Typeface.BOLD : Typeface.NORMAL);
        if (unread) {
            holder.tvBadge.setVisibility(View.VISIBLE);
            holder.tvBadge.setText(item.unreadCount > 99 ? "99+" : String.valueOf(item.unreadCount));
        } else {
            holder.tvBadge.setVisibility(View.GONE);
        }
        if (item.avatarUri != null && !item.avatarUri.isEmpty()) {
            holder.ivAvatar.setImageURI(Uri.parse(item.avatarUri));
        } else {
            holder.ivAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView ivAvatar;
        final TextView tvName;
        final TextView tvPreview;
        final TextView tvBadge;

        VH(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvName = itemView.findViewById(R.id.tvUserName);
            tvPreview = itemView.findViewById(R.id.tvMessagePreview);
            tvBadge = itemView.findViewById(R.id.tvUnreadBadge);
        }
    }
}
