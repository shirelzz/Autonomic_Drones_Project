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
    private DataFromDrone dataFromDrone;
    private GoogleMap map;

    private SupportMapFragment mapFragment;

    private static final int ZOOM = 16;

    public PresentMap(DataFromDrone dataFromDrone) {
        this.dataFromDrone = dataFromDrone;

        mapFragment = (SupportMapFragment) GlobalData.getSupportFragmentManager().findFragmentById(R.id.map_view);
        mapFragment.getMapAsync(this);
        MapVisibility(false);
    }

    public void MapVisibility(boolean isVisible) {
        Objects.requireNonNull(mapFragment.getView()).setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
        ToastUtils.showToast("In MapVisibility");
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;
        try {
//            LatLng currentLocation = new LatLng(dataFromDrone.getGPS().getLatitude(), dataFromDrone.getGPS().getLongitude());
            LatLng currentLocation = new LatLng(32.0841854, 34.8487116);
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

            }
        });

    }
}

