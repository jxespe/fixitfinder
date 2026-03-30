package com.example.fixitfinderapp.models;

import androidx.annotation.Nullable;

public class UserReviewItem {

    public final String bookingId;
    @Nullable
    public final String providerId;
    public final String providerName;
    @Nullable
    public final String providerAddress;
    @Nullable
    public final String serviceCategory;
    public final int rating;
    @Nullable
    public final String reviewText;
    @Nullable
    public final String logoUri;
    public final long sortTimestamp;

    public UserReviewItem(String bookingId,
                          @Nullable String providerId,
                          String providerName,
                          @Nullable String providerAddress,
                          @Nullable String serviceCategory,
                          int rating,
                          @Nullable String reviewText,
                          @Nullable String logoUri,
                          long sortTimestamp) {
        this.bookingId = bookingId;
        this.providerId = providerId;
        this.providerName = providerName;
        this.providerAddress = providerAddress;
        this.serviceCategory = serviceCategory;
        this.rating = rating;
        this.reviewText = reviewText;
        this.logoUri = logoUri;
        this.sortTimestamp = sortTimestamp;
    }
}
