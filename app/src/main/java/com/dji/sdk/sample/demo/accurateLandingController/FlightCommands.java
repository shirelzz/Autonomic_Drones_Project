package com.dji.sdk.sample.demo.accurateLandingController;

import dji.sdk.flightcontroller.FlightController;

public class FlightCommands {
    FlightController flightController;

//    DataFromDrone dataFromDrone;
    private float[] target; // [lat, long, alt]
    private float[] velocity;

    public FlightCommands() {}
    private void setTarget(float[] target) {
        this.target = target;
    }

    private void setVelocity(float[] velocity) {
        this.velocity = velocity;
    }

    /**
     * Sends commands to the virtual stick to get to a certain position
     * @param pos - GPS position [lat, long, alt]
     */
    private void goTo(float[] pos) {

    }

    /**
     * Calculates the distance between the drone and a certain position
     * @param pos - GPS position [lat, long, alt]
     * @param dataFromDrone DataFromDrone instance to receive the GPS data of tge drone
     * @return distance
     */
    private double[] calcDistFrom(double[] pos, DataFromDrone dataFromDrone) {
        double[] myPos = dataFromDrone.getGPS();
        return Cords.flatWorldDist(myPos, pos);
    }

    /**
     * Calculates the azimuth, distance and dz in degrees and meters
     * between the drone and a certain position
     * @param pos - GPS position [lat, long, alt]
     * @param dataFromDrone DataFromDrone instance to receive the GPS data of tge drone
     * @return [azm, dist, dz]
     */
    private double[] calcAzmDistFrom(double[] pos, DataFromDrone dataFromDrone) {
        double[] myPos = dataFromDrone.getGPS();
        return Cords.azmDist(myPos, pos);
    }

}

