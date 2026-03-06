package com.example.fixitfinderapp.models;

public class ProviderBookingItem {
    public final String bookingId;
    public final String bookingNumber;
    public final String bookedBy;
    public final String bookedAt;
    public final String requiredAt;
    public final String location;
    public final String status;
    public final String jobDescription;
    public final String priceText;

    public ProviderBookingItem(String bookingId, String bookingNumber, String bookedBy,
                               String bookedAt, String requiredAt, String location, String status) {
        this(bookingId, bookingNumber, bookedBy, bookedAt, requiredAt, location, status,
                "Job: N/A", "Price: N/A");
    }

    public ProviderBookingItem(String bookingId, String bookingNumber, String bookedBy,
                               String bookedAt, String requiredAt, String location, String status,
                               String jobDescription, String priceText) {
        this.bookingId = bookingId;
        this.bookingNumber = bookingNumber;
        this.bookedBy = bookedBy;
        this.bookedAt = bookedAt;
        this.requiredAt = requiredAt;
        this.location = location;
        this.status = status;
        this.jobDescription = jobDescription;
        this.priceText = priceText;
    }
}
