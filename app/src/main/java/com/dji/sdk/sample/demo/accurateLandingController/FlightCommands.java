package com.dji.sdk.sample.demo.accurateLandingController;

import dji.sdk.flightcontroller.FlightController;

public class FlightCommands {
    FlightController flightController;
    private float[] target;
    private float[] velocity;
    private void setTarget(float[] target) {
        this.target = target;
    }

    private void setVelocity(float[] velocity) {
        this.velocity = velocity;
    }

    private void goTo(float[] pos) {
    }


}
