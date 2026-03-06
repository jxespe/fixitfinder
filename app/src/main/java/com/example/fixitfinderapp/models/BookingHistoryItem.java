package com.example.fixitfinderapp.models;

public class BookingHistoryItem {
    public final String bookingId;
    public final String title;
    public final String dateText;
    public final String status;
    public final String paymentStatus;
    public final String logoUri;
    public final String description;
    public final String priceText;
    public final long sortTimestamp;

    public BookingHistoryItem(String bookingId, String title, String dateText, String status,
                              String paymentStatus, String logoUri,
                              String description, String priceText) {
        this(bookingId, title, dateText, status, paymentStatus, logoUri,
                description, priceText, 0L);
    }

    public BookingHistoryItem(String bookingId, String title, String dateText, String status,
                              String paymentStatus, String logoUri,
                              String description, String priceText, long sortTimestamp) {
        this.bookingId = bookingId;
        this.title = title;
        this.dateText = dateText;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.logoUri = logoUri;
        this.description = description;
        this.priceText = priceText;
        this.sortTimestamp = sortTimestamp;
    }
}
