package com.dji.sdk.sample.demo.accurateLandingController;

import android.view.View;

import androidx.annotation.NonNull;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.demo.GlobalData;
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
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;
//        LatLng currentLocation = new LatLng(dataFromDrone.getGPS().getLatitude(), dataFromDrone.getGPS().getLongitude());
//        map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, ZOOM));

    }
}

