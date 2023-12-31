package com.dji.sdk.sample.demo.accurateLandingController;

public class GPSLocation {
    private double altitude;
    private double longitude;
    private double latitude;

    public GPSLocation() {
        altitude = 0.0;
        longitude = 0.0;
        latitude = 0.0;
    }

    public GPSLocation(double latitude, double longitude, double altitude) {
        this.altitude = altitude;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public GPSLocation(double longitude, double latitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public GPSLocation(double[] position) {
        this.latitude = position[0];
        this.longitude = position[1];
        this.altitude = position[2];
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double[] getAll() {
        return new double[]{this.getLatitude(), this.getLongitude(), this.getAltitude()};
    }
}
