package com.example.fixitfinderapp.models;

public class ServiceProviderProfile {
    public final String providerId;
    public final String fullName;
    public final String serviceCategory;
    public final String address;
    public final String logoUri;
    public Double lat;
    public Double lng;

    public ServiceProviderProfile(String providerId, String fullName, String serviceCategory,
                                  String address, String logoUri, Double lat, Double lng) {
        this.providerId = providerId;
        this.fullName = fullName;
        this.serviceCategory = serviceCategory;
        this.address = address;
        this.logoUri = logoUri;
        this.lat = lat;
        this.lng = lng;
    }

    public void setLatLng(Double lat, Double lng) {
        this.lat = lat;
        this.lng = lng;
    }
}
