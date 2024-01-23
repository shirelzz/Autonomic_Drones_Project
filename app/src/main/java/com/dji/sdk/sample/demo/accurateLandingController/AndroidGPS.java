package com.dji.sdk.sample.demo.accurateLandingController;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

public class AndroidGPS {

    private Context context;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private double latitude;
    private double longitude;
    private double altitude;
    private static final long LOCATION_UPDATE_INTERVAL = 10 * 1000; // 10 seconds
    private Handler locationUpdateHandler;


    public AndroidGPS(Context context) {
        this.context = context;
        initializeLocationManager();
//        startLocationUpdates();
    }

    private void initializeLocationManager() {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateLocationData(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // Handle status changes if needed
            }

            @Override
            public void onProviderEnabled(String provider) {
                // Handle provider enabled
            }

            @Override
            public void onProviderDisabled(String provider) {
                // Handle provider disabled
            }
        };

        locationUpdateHandler = new Handler(Looper.getMainLooper());
        startPeriodicLocationUpdates();
    }

//    private void startLocationUpdates() {
//        // Check location permission
//        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
//                == PackageManager.PERMISSION_GRANTED) {
//            // Request location updates
//            locationManager.requestLocationUpdates(
//                    LocationManager.GPS_PROVIDER,
//                    1000,  // Update every 1000 milliseconds (1 second)
//                    1,     // Update every 1 meter
//                    locationListener);
//        }
//    }

    private void startPeriodicLocationUpdates() {
        locationUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                requestLocationUpdates();
                locationUpdateHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
            }
        }, LOCATION_UPDATE_INTERVAL);
    }

    private void requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Request location updates
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0,
                    locationListener);
        }
    }

    private void updateLocationData(Location location) {
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            altitude = location.getAltitude();
        }
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void stopLocationUpdates() {
        locationManager.removeUpdates(locationListener);
        locationUpdateHandler.removeCallbacksAndMessages(null);
    }}
