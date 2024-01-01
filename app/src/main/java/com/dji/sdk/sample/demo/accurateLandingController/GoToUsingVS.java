package com.dji.sdk.sample.demo.accurateLandingController;

import android.annotation.SuppressLint;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.Arrays;

public class  GoToUsingVS {
    private GPSLocation startGpsLocation;
    private GPSLocation targetGpsLocation;
    private FlightControlMethods flightControlMethods;

    private DataFromDrone dataFromDrone;
    // Adjust these scaling factors based on flight testing
    private static final double SCALE_FACTOR_DISTANCE = 0.5;
    private static final double SCALE_FACTOR_THROTTLE = 0.7;


    public GoToUsingVS(@Nullable GPSLocation startGpsLocation, @Nullable GPSLocation targetGpsLocation, DataFromDrone dataFromDrone) {
        this.startGpsLocation = startGpsLocation;
        this.targetGpsLocation = targetGpsLocation;
        this.dataFromDrone = dataFromDrone;
        this.flightControlMethods = new FlightControlMethods();
    }

    public GoToUsingVS(DataFromDrone dataFromDrone) {
        this(null, null, dataFromDrone);
    }

    public float[] calculateMovement() {
        if (startGpsLocation == null || targetGpsLocation == null) {
            return new float[]{0, 0, 0, 0}; // Return default commands if locations are null
        }

        // Calculate angle and distance between start and target coordinates using Cords class
        double[] azmDist = Cords.azmDist(startGpsLocation.getAll(), targetGpsLocation.getAll());
        double azimuth = azmDist[0]; // Angle
        double distance = azmDist[1]; // Distance in meters

        // Convert azimuth and distance to control commands for the drone
        float pitch = (float) (Math.sin(Math.toRadians(azimuth)) * distance);
        float roll = (float) calculateAdjustedRoll(azimuth, distance); // Use adjusted roll calculation
        float yaw = (float) azimuth; // Assuming yaw is already in degrees and within expected range
        float throttle = (float) (distance * SCALE_FACTOR_THROTTLE * SCALE_FACTOR_DISTANCE);

        // Send virtual stick commands to the drone
        flightControlMethods.sendVirtualStickCommands(pitch, roll, yaw, throttle);

        return new float[]{pitch, roll, yaw, throttle};
    }

    private double calculateAdjustedRoll(double azimuth, double distance){
        // Obtain drone's current yaw (replace with actual mechanism to get yaw)
        double currentYaw = dataFromDrone.getYaw(); // Assuming this method exists in your SDK version

        // Calculate the difference between current yaw and desired azimuth
        double yawDifference = azimuth - currentYaw;

        // Adjust roll based on yaw difference (example implementation)
        float rollAdjustment = (float) (Math.cos(Math.toRadians(yawDifference)) * distance);

        // Return the adjusted roll value
        return rollAdjustment;
    }

    @SuppressLint("SetTextI18n")
    public void startGoTo(TextView dist, FlightCommands flightCommands){
        // need to provide relevant values
        GPSLocation gpsLocation = this.getDestGpsLocation();
        double[] pos;
        if (gpsLocation == null) {
            double lat = dataFromDrone.getGPS().getLatitude() + 0.001;
            double lon = dataFromDrone.getGPS().getLongitude() + 0.000001;
            double alt = dataFromDrone.getGPS().getAltitude();


            pos = new double[]{lat, lon, alt};
            this.setTargetGpsLocation(pos);
        } else {
            pos = gpsLocation.getAll();
        }
        this.setCurrentGpsLocation(dataFromDrone.getGPS());

        dist.setText(Arrays.toString(flightCommands.calcDistFrom(pos, dataFromDrone)) + " [" + Arrays.toString(this.calculateMovement()));

    }

    public GPSLocation getCurrentGpsLocation() {
        return startGpsLocation;
    }

    public void setCurrentGpsLocation(GPSLocation startGpsLocation) {
        this.startGpsLocation = startGpsLocation;
    }

    public GPSLocation getDestGpsLocation() {
        return targetGpsLocation;
    }

    public void setTargetGpsLocation(GPSLocation targetGpsLocation) {
        this.targetGpsLocation = targetGpsLocation;
    }

    public void setTargetGpsLocation(double[] targetGpsLocation) {
        this.targetGpsLocation = new GPSLocation(targetGpsLocation);
    }
}
