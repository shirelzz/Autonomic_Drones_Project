package com.dji.sdk.sample.demo.accurateLandingController;

import android.view.View;

import androidx.annotation.NonNull;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.demo.GlobalData;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.util.Objects;

public class PresentMap implements OnMapReadyCallback {
    private static final int ZOOM = 18;
    private final DataFromDrone dataFromDrone;
    private GoogleMap map;

    private final GoToUsingVS goToUsingVS;
    private SupportMapFragment mapFragment;

    public PresentMap(DataFromDrone dataFromDrone, GoToUsingVS goToUsingVS) {
        this.dataFromDrone = dataFromDrone;
        this.goToUsingVS = goToUsingVS;
        mapFragment = (SupportMapFragment) GlobalData.getSupportFragmentManager().findFragmentById(R.id.map_view);
        mapFragment.getMapAsync(this);
        MapVisibility(false);
    }

    public void MapVisibility(boolean isVisible) {
        Objects.requireNonNull(mapFragment.getView()).setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
//        ToastUtils.showToast("In MapVisibility");
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;
        try {
            double latitude = 32.085114, longitude = 34.852653;
            if (dataFromDrone.getGpsSignalLevel().value() >= 2) {
                latitude = dataFromDrone.getGPS().getLatitude();
                longitude = dataFromDrone.getGPS().getLongitude();
            }
            LatLng currentLocation = new LatLng(latitude, longitude);

            map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, ZOOM));
            this.clickOnMap();
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.showToast(e.getMessage());
        }
    }

    public void clickOnMap() {

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                double latitude = latLng.latitude;
                double longitude = latLng.longitude;
                ToastUtils.showToast("Lat : " + latitude + " , "
                        + "Long : " + longitude);
                try{
                    goToUsingVS.setTargetGpsLocation(new double[]{latitude, longitude, dataFromDrone.getGPS().getAltitude()});

                } catch (Exception e) {
                    e.printStackTrace();
                    ToastUtils.showToast(e.getMessage());
                }
            }
        });

    }
}

