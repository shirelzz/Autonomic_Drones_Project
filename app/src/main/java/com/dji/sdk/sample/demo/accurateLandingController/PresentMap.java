package com.dji.sdk.sample.demo.accurateLandingController;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.dji.sdk.sample.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * An activity that displays a map showing the place at the device's current location.
 */
public class PresentMap
//        implements OnMapReadyCallback
{

    private static final String TAG = PresentMap.class.getSimpleName();
//    private GoogleMap map;
//    private CameraPosition cameraPosition;

    // The entry point to the Places API.
//    private PlacesClient placesClient;

    // The entry point to the Fused Location Provider.
//    private FusedLocationProviderClient fusedLocationProviderClient;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
//    private final LatLng defaultLocation = new LatLng(-33.8523341, 151.2106085);
//    private static final int DEFAULT_ZOOM = 15;
//    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted = true;
//
    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.

    //    protected ImageView imgView;
    private Context context;
    private Location lastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    // Used for selecting the current place.
    private static final int M_MAX_ENTRIES = 5;
    private String[] likelyPlaceNames;
    private String[] likelyPlaceAddresses;
    private List[] likelyPlaceAttributions;
    private LatLng[] likelyPlaceLatLngs;
    private Activity activity;
    private MapView mapView;


    public PresentMap(Bundle savedInstanceState, Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
//        this.mapView = activity.findViewById(R.id.mapView);

        initializeMap(savedInstanceState);
////        this.placesClient = Places.createClient(context);
////        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
//        mapView.onCreate(savedInstanceState);
//        // Retrieve location and camera position from saved instance state.
//        if (savedInstanceState != null) {
//            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
//            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
//        }

//        // Construct a PlacesClient
//        Places.initialize(context, BuildConfig.PLACES_API_KEY);
//        placesClient = Places.createClient(context);
//
//        // Build the map.
//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
//
//        mapFragment.getMapAsync(this);
    }

    public void initializeMap(Bundle savedInstanceState) {
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                // Customize map and handle interactions here
                googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                googleMap.getUiSettings().setZoomControlsEnabled(true);
                setMapClickListener(googleMap);
            }
        });
    }

    public MapView getMapView() {
        return mapView;
    }

    private void setMapClickListener(GoogleMap googleMap) {
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                double latitude = latLng.latitude;
                double longitude = latLng.longitude;
                // Handle map click actions here
                Toast.makeText(activity, "Clicked location: " + latitude + ", " + longitude, Toast.LENGTH_SHORT).show();

            }
        });
    }


    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
//    @SuppressLint("PotentialBehaviorOverride")
//    @Override
//    public void onMapReady(GoogleMap map) {
//        this.map = map;
//        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
//        map.getUiSettings().setZoomControlsEnabled(true);
//
//        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
//            @Override
//            public void onMapClick(@NonNull LatLng latLng) {
//                double latitude = latLng.latitude;
//                double longitude = latLng.longitude;
//                Toast.makeText(activity, "Clicked location: " + latitude + ", " + longitude, Toast.LENGTH_SHORT).show();
//            }
//        });
    // Use a custom info window adapter to handle multiple lines of text in the
    // info window contents.
//        this.map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
//
//            @Override
//            // Return null here, so that getInfoContents() is called next.
//            public View getInfoWindow(Marker arg0) {
//                return null;
//            }
//
//            @Override
//            public View getInfoContents(Marker marker) {
//                // Inflate the layouts for the info window, title and snippet.
//                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
//                        (FrameLayout) findViewById(R.id.map), false);
//
//                TextView title = infoWindow.findViewById(R.id.title);
//                title.setText(marker.getTitle());
//
//                TextView snippet = infoWindow.findViewById(R.id.snippet);
//                snippet.setText(marker.getSnippet());
//
//                return infoWindow;
//            }
//        });

    // Turn on the My Location layer and the related control on the map.
//        updateLocationUI();

    // Get the current location of the device and set the position of the map.
//        getDeviceLocation();
//    }

//    @Override
//    public void onMapReady(@NonNull GoogleMap googleMap) {
//
//    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
//    private void getDeviceLocation() {
//        /*
//         * Get the best and most recent location of the device, which may be null in rare
//         * cases when a location is not available.
//         */
//        try {
//            if (locationPermissionGranted) {
////                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
////                locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
////                    @Override
////                    public void onComplete(@NonNull Task<Location> task) {
////                        if (task.isSuccessful()) {
////                            // Set the map's camera position to the current location of the device.
////                            lastKnownLocation = task.getResult();
////                            if (lastKnownLocation != null) {
////                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
////                                        new LatLng(lastKnownLocation.getLatitude(),
////                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
////                            }
////                        } else {
////                            Log.d(TAG, "Current location is null. Using defaults.");
////                            Log.e(TAG, "Exception: %s", task.getException());
////                            map.moveCamera(CameraUpdateFactory
////                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
////                            map.getUiSettings().setMyLocationButtonEnabled(false);
////                        }
////                    }
////                });
//            }
//        } catch (SecurityException e) {
//            Log.e("Exception: %s", e.getMessage(), e);
//        }
//    }


    public void onResume() {
        mapView.onResume();
    }

    public void onPause() {
        mapView.onPause();
    }

    public void onDestroy() {
        mapView.onDestroy();
    }


    /**
     * Prompts the user to select the current place from a list of likely places, and shows the
     * current place on the map - provided the user has granted location permission.
     */
//    private void showCurrentPlace() {
//        if (map == null) {
//            return;
//        }
//
//        if (locationPermissionGranted) {
//            // Use fields to define the data types to return.
//            List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS,
//                    Place.Field.LAT_LNG);
//
//            // Use the builder to create a FindCurrentPlaceRequest.
//            FindCurrentPlaceRequest request =
//                    FindCurrentPlaceRequest.newInstance(placeFields);
//
//            // Get the likely places - that is, the businesses and other points of interest that
//            // are the best match for the device's current location.
////            @SuppressWarnings("MissingPermission") final Task<FindCurrentPlaceResponse> placeResult =
////                    placesClient.findCurrentPlace(request);
////            placeResult.addOnCompleteListener(new OnCompleteListener<FindCurrentPlaceResponse>() {
////                @Override
////                public void onComplete(@NonNull Task<FindCurrentPlaceResponse> task) {
////                    if (task.isSuccessful() && task.getResult() != null) {
////                        FindCurrentPlaceResponse likelyPlaces = task.getResult();
////
////                        // Set the count, handling cases where less than 5 entries are returned.
////                        int count;
////                        if (likelyPlaces.getPlaceLikelihoods().size() < M_MAX_ENTRIES) {
////                            count = likelyPlaces.getPlaceLikelihoods().size();
////                        } else {
////                            count = M_MAX_ENTRIES;
////                        }
////
////                        int i = 0;
////                        likelyPlaceNames = new String[count];
////                        likelyPlaceAddresses = new String[count];
////                        likelyPlaceAttributions = new List[count];
////                        likelyPlaceLatLngs = new LatLng[count];
////
////                        for (PlaceLikelihood placeLikelihood : likelyPlaces.getPlaceLikelihoods()) {
////                            // Build a list of likely places to show the user.
////                            likelyPlaceNames[i] = placeLikelihood.getPlace().getName();
////                            likelyPlaceAddresses[i] = placeLikelihood.getPlace().getAddress();
////                            likelyPlaceAttributions[i] = placeLikelihood.getPlace()
////                                    .getAttributions();
////                            likelyPlaceLatLngs[i] = placeLikelihood.getPlace().getLatLng();
////
////                            i++;
////                            if (i > (count - 1)) {
////                                break;
////                            }
////                        }
////
////                        // Show a dialog offering the user the list of likely places, and add a
////                        // marker at the selected place.
////                        openPlacesDialog();
////                    } else {
////                        Log.e(TAG, "Exception: %s", task.getException());
////                    }
////                }
////            });
//        } else {
//            // The user has not granted permission.
//            Log.i(TAG, "The user did not grant location permission.");
//
//            // Add a default marker, because the user hasn't selected a place.
////            map.addMarker(new MarkerOptions()
////                    .title(getString(R.string.default_info_title))
////                    .position(defaultLocation)
////                    .snippet(getString(R.string.default_info_snippet)));
//        }
//    }

    /**
     * Displays a form allowing the user to select a place from a list of likely places.
     */
//    private void openPlacesDialog() {
//        // Ask the user to choose the place where they are now.
//        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                // The "which" argument contains the position of the selected item.
//                LatLng markerLatLng = likelyPlaceLatLngs[which];
//                String markerSnippet = likelyPlaceAddresses[which];
//                if (likelyPlaceAttributions[which] != null) {
//                    markerSnippet = markerSnippet + "\n" + likelyPlaceAttributions[which];
//                }
//
//                // Add a marker for the selected place, with an info window
//                // showing information about that place.
//                map.addMarker(new MarkerOptions()
//                        .title(likelyPlaceNames[which])
//                        .position(markerLatLng)
//                        .snippet(markerSnippet));
//
//                // Position the map's camera at the location of the marker.
//                map.moveCamera(CameraUpdateFactory.newLatLngZoom(markerLatLng,
//                        DEFAULT_ZOOM));
//            }
//        };
//
////        // Display the dialog.
////        AlertDialog dialog = new AlertDialog.Builder(context)
////                .setTitle(R.string.pick_place)
////                .setItems(likelyPlaceNames, listener)
////                .show();
//    }
//
//    /**
//     * Updates the map's UI settings based on whether the user has granted location permission.
//     */
//    private void updateLocationUI() {
//        if (map == null) {
//            return;
//        }
//        try {
//            if (locationPermissionGranted) {
//                map.setMyLocationEnabled(true);
//                map.getUiSettings().setMyLocationButtonEnabled(true);
//            } else {
//                map.setMyLocationEnabled(false);
//                map.getUiSettings().setMyLocationButtonEnabled(false);
//                lastKnownLocation = null;
//            }
//        } catch (SecurityException e) {
//            Log.e("Exception: %s", e.getMessage());
//        }
//    }
}

