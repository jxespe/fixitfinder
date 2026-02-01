package com.example.fixitfinderapp.models;

public class BookingHistoryItem {
    public final String title;
    public final String dateText;
    public final String status;
    public final String paymentStatus;
    public final String logoUri;

    public BookingHistoryItem(String title, String dateText, String status,
                              String paymentStatus, String logoUri) {
        this.title = title;
        this.dateText = dateText;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.logoUri = logoUri;
    }
}
