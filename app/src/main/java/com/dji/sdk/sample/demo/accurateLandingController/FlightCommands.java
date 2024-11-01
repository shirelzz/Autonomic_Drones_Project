package com.dji.sdk.sample.demo.accurateLandingController;

import android.util.Log;

import dji.common.error.DJIError;
import dji.common.mission.followme.FollowMeMissionState;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.followme.FollowMeMissionOperator;
import dji.waypointv2.common.waypointv1.LocationCoordinate2D;

public class FlightCommands {
    private FlightControlMethods flightControlMethods;
    private FlightController flightController;

    //    DataFromDrone dataFromDrone;
    private float[] target; // [lat, long, alt]
    private float[] velocity;

    public FlightCommands() {
        flightControlMethods = new FlightControlMethods();
        flightController = flightControlMethods.getFlightController();
    }

    public FlightController getFlightController() {
        try {
            if (flightController != null) {
                return flightController;
            }
        } catch (Exception e) {
            Log.d("Null", "FlightCommands: flightController is null");
            throw new RuntimeException(e);
        }
        return null;
    }

    public void setTarget(float[] target) {
        this.target = target;
    }

    public void setVelocity(float[] velocity) {
        this.velocity = velocity;
    }

    /**
     * Sends commands to the virtual stick to get to a certain position
     *
     * @param pos - GPS position [lat, long, alt]
     */
    public void goTo(float[] pos) {

        /* GoToAction(LocationCoordinate2D coordinate, float altitude)
         Go to the specified coordinate and altitude (in meters) from the current aircraft position.
         The actionType of this object is set to GoToActionType.COORDINATE_AND_ALTITUDE.
         */

    }





    /**
     * Calculates the distance between the drone and a certain position
     *
     * @param pos           - GPS position [lat, long, alt]
     * @param dataFromDrone DataFromDrone instance to receive the GPS data of tge drone
     * @return distance
     */
    public double[] calcDistFrom(double[] pos, DataFromDrone dataFromDrone) {
        double[] myPos = dataFromDrone.getGPS().getAll();
        return Cords.flatWorldDist(myPos, pos);
    }

    /**
     * Calculates the azimuth, distance and dz in degrees and meters
     * between the drone and a certain position
     *
     * @param pos           - GPS position [lat, long, alt]
     * @param dataFromDrone DataFromDrone instance to receive the GPS data of tge drone
     * @return [azm, dist, dz]
     */
    public double[] calcAzmDistFrom(double[] pos, DataFromDrone dataFromDrone) {
        double[] myPos = dataFromDrone.getGPS().getAll();
        return Cords.azmDist(myPos, pos);
    }



}

