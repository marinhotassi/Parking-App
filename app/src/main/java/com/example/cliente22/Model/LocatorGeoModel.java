package com.example.cliente22.Model;

import com.firebase.geofire.GeoLocation;

public class LocatorGeoModel {
    private String key;
    private GeoLocation geoLocation;
    private LocatorInfoModel locatorInfoModel;

    public LocatorGeoModel() {
    }

    public LocatorGeoModel(String key, GeoLocation geoLocation) {
        this.key = key;
        this.geoLocation = geoLocation;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public GeoLocation getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(GeoLocation geoLocation) {
        this.geoLocation = geoLocation;
    }

    public LocatorInfoModel getLocatorInfoModel() {
        return locatorInfoModel;
    }

    public void setLocatorInfoModel(LocatorInfoModel locatorInfoModel) {
        this.locatorInfoModel = locatorInfoModel;
    }
}
