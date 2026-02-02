package com.example.fixitfinderapp.models;

public class ProviderBookingItem {
    public final String bookingId;
    public final String bookingNumber;
    public final String bookedBy;
    public final String bookedAt;
    public final String requiredAt;
    public final String location;
    public final String status;

    public ProviderBookingItem(String bookingId, String bookingNumber, String bookedBy,
                               String bookedAt, String requiredAt, String location, String status) {
        this.bookingId = bookingId;
        this.bookingNumber = bookingNumber;
        this.bookedBy = bookedBy;
        this.bookedAt = bookedAt;
        this.requiredAt = requiredAt;
        this.location = location;
        this.status = status;
    }
}
