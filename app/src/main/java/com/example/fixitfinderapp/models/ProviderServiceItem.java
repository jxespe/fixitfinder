package com.example.fixitfinderapp.models;

public class ProviderServiceItem {
    public final String id;
    public final String name;
    public final double price;
    public final String imageUri;
    public final String description;

    public ProviderServiceItem(String id, String name, double price, String imageUri, String description) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageUri = imageUri;
        this.description = description;
    }
}
