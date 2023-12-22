package com.dji.sdk.sample.demo.accurateLandingController;

import androidx.annotation.Nullable;

public class GoToUsingVS {
    private GPSLocation startGpsLocation;
    private GPSLocation targetGpsLocation;
    private FlightControlMethods flightControlMethods;

    //    private static final double SOME_SCALING_FACTOR = 0.5;
    private static final double SCALE_FACTOR_DISTANCE = 0.5; // Adjust as needed
    private static final double SCALE_FACTOR_THROTTLE = 0.7; // Adjust as needed


    public GoToUsingVS(@Nullable GPSLocation startGpsLocation, @Nullable GPSLocation targetGpsLocation) {
        this.startGpsLocation = startGpsLocation;
        this.targetGpsLocation = targetGpsLocation;
        this.flightControlMethods = new FlightControlMethods();
    }

    public GoToUsingVS() {
        this(null, null);
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
        float roll = (float) (Math.cos(Math.toRadians(azimuth)) * distance);

        float normalizedYaw = (float) ((azimuth + 360) % 360);
//        if (normalizedYaw > 180) {
//            normalizedYaw -= 360;
//        }
        float yaw = normalizedYaw; // You might need to normalize or adjust this value based on your drone's requirements
        float throttle = (float) (distance * SCALE_FACTOR_THROTTLE * SCALE_FACTOR_DISTANCE); // Adjust SOME_SCALING_FACTOR based on your requirements
//        flightControlMethods.sendVirtualStickCommands(pitch, roll, yaw, throttle);
        return new float[]{pitch, roll, yaw, throttle};

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
