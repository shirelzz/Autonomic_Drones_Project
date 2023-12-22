package com.dji.sdk.sample.demo.accurateLandingController;

import java.util.Objects;

import dji.common.flightcontroller.flightassistant.FillLightMode;
import dji.sdk.flightcontroller.FlightController;

public class DroneFeatures {
    FlightControlMethods flightControlMethods;

    public DroneFeatures(FlightControlMethods flightControlMethods) {
        this.flightControlMethods = flightControlMethods;
    }

    /*
     * Sets the downward fill light mode. It is supported by Mavic 2 series and Matrice 300 RTK.
     */
    public void setDownwardLight(FillLightMode data) {
        Objects.requireNonNull(flightControlMethods.getFlightController().getFlightAssistant()).setDownwardFillLightMode(data, null);
    }
}
