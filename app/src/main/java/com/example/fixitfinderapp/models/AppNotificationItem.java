package com.example.fixitfinderapp.models;

public class AppNotificationItem {
    public final String title;
    public final String message;
    public final long timestamp;

    public AppNotificationItem(String title, String message, long timestamp) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
    }
}
