package com.example.fixitfinderapp.models;

public class ChatMessage {
    public final String messageId;
    public final String senderId;
    public final String senderRole;
    public final String text;
    public final String imageUrl;
    public final String type;
    public final String bookingId;
    public final long createdAt;

    public ChatMessage(String messageId, String senderId, String senderRole,
                       String text, String imageUrl, String type, String bookingId, long createdAt) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.text = text;
        this.imageUrl = imageUrl;
        this.type = type;
        this.bookingId = bookingId;
        this.createdAt = createdAt;
    }
}
