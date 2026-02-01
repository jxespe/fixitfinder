package com.example.fixitfinderapp.models;

public class ProviderEarningItem {
    public final String bookingNumber;
    public final String paidBy;
    public final String paidAt;
    public final String jobDone;
    public final String paymentMethod;

    public ProviderEarningItem(String bookingNumber, String paidBy,
                               String paidAt, String jobDone, String paymentMethod) {
        this.bookingNumber = bookingNumber;
        this.paidBy = paidBy;
        this.paidAt = paidAt;
        this.jobDone = jobDone;
        this.paymentMethod = paymentMethod;
    }
}
