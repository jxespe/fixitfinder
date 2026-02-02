package com.example.fixitfinderapp.models;

public class ConversationItem {
    public final String conversationId;
    public final String name;
    public final String preview;
    public final String avatarUri;
    public final long lastMessageAt;
    public final int unreadCount;

    public ConversationItem(String conversationId, String name, String preview,
                            String avatarUri, long lastMessageAt, int unreadCount) {
        this.conversationId = conversationId;
        this.name = name;
        this.preview = preview;
        this.avatarUri = avatarUri;
        this.lastMessageAt = lastMessageAt;
        this.unreadCount = unreadCount;
    }
}
