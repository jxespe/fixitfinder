package com.example.fixitfinderapp.models;

public class ChatMessage {
    public final String messageId;
    public final String senderId;
    public final String senderRole;
    public final String text;
    public final long createdAt;

    public ChatMessage(String messageId, String senderId, String senderRole,
                       String text, long createdAt) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.text = text;
        this.createdAt = createdAt;
    }
}
