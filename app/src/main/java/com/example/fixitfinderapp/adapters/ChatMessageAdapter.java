package com.example.fixitfinderapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.text.TextUtils;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.ImageLoader;
import com.example.fixitfinderapp.BookingTimelineActivity;
import com.example.fixitfinderapp.models.ChatMessage;

import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.VH> {

    private static final int TYPE_RECEIVED = 0;
    private static final int TYPE_SENT = 1;

    private final List<ChatMessage> messages;
    private final String currentUserId;
    private final String currentRole;
    private String peerAvatarUrl = "";
    private String selfAvatarUrl = "";

    public ChatMessageAdapter(List<ChatMessage> messages, String currentUserId, String currentRole) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.currentRole = currentRole;
    }

    public void setPeerAvatarUrl(String url) {
        String next = url != null ? url : "";
        if (next.equals(peerAvatarUrl)) {
            return;
        }
        peerAvatarUrl = next;
        notifyDataSetChanged();
    }

    public void setSelfAvatarUrl(String url) {
        String next = url != null ? url : "";
        if (next.equals(selfAvatarUrl)) {
            return;
        }
        selfAvatarUrl = next;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = messages.get(position);
        if (msg.senderId != null && msg.senderId.equals(currentUserId)) {
            return TYPE_SENT;
        }
        return TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_SENT
                ? R.layout.item_chat_message_sent
                : R.layout.item_chat_message_received;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ChatMessage msg = messages.get(position);
        boolean hasText = !TextUtils.isEmpty(msg.text);
        boolean hasImage = !TextUtils.isEmpty(msg.imageUrl);
        boolean isRatePrompt = "rate_prompt".equalsIgnoreCase(msg.type);
        if (hasText) {
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.tvMessage.setText(msg.text);
        } else {
            holder.tvMessage.setVisibility(View.GONE);
        }
        if (hasImage) {
            holder.ivMessageImage.setVisibility(View.VISIBLE);
            ImageLoader.load(holder.ivMessageImage, msg.imageUrl, 0);
        } else {
            holder.ivMessageImage.setVisibility(View.GONE);
        }

        if (isRatePrompt && "user".equalsIgnoreCase(currentRole)
                && !TextUtils.isEmpty(msg.bookingId)) {
            holder.tvMessage.setText(msg.text != null ? msg.text : "Tap to rate your service");
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.tvMessage.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), BookingTimelineActivity.class);
                intent.putExtra("bookingId", msg.bookingId);
                v.getContext().startActivity(intent);
            });
        } else {
            holder.tvMessage.setOnClickListener(null);
        }

        boolean isSent = getItemViewType(position) == TYPE_SENT;
        boolean showFace = isLastInSenderRun(position, msg);
        if (showFace) {
            holder.ivAvatar.setVisibility(View.VISIBLE);
            String url = isSent ? selfAvatarUrl : peerAvatarUrl;
            ImageLoader.loadProfile(holder.ivAvatar, url, android.R.drawable.ic_menu_myplaces);
        } else {
            holder.ivAvatar.setVisibility(View.INVISIBLE);
        }
    }

    /** Avatar on the last row of each consecutive block from the same sender (Messenger-style). */
    private boolean isLastInSenderRun(int position, ChatMessage msg) {
        if (position >= messages.size() - 1) {
            return true;
        }
        ChatMessage next = messages.get(position + 1);
        String a = msg.senderId;
        String b = next.senderId;
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return true;
        }
        return !a.equals(b);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvMessage;
        final ImageView ivMessageImage;
        final ImageView ivAvatar;

        VH(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageText);
            ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
            ivAvatar = itemView.findViewById(R.id.ivMessageAvatar);
        }
    }
}
