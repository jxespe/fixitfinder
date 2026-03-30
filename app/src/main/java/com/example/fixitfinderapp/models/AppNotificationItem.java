package com.example.fixitfinderapp.models;

public class AppNotificationItem {
    public final String title;
    public final String message;
    public final long timestamp;
    public final String source;
    public final String bookingId;
    public final String conversationId;

    public AppNotificationItem(String title, String message, long timestamp) {
        this(title, message, timestamp, "", "", "");
    }

    public AppNotificationItem(String title,
                               String message,
                               long timestamp,
                               String source,
                               String bookingId,
                               String conversationId) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.source = source != null ? source : "";
        this.bookingId = bookingId != null ? bookingId : "";
        this.conversationId = conversationId != null ? conversationId : "";
    }
}
