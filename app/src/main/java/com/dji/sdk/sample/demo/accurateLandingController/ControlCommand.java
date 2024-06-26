package com.dji.sdk.sample.demo.accurateLandingController;

/*
and object to describe controll comand to drone
 */

import dji.common.flightcontroller.virtualstick.VerticalControlMode;

public class ControlCommand {

    //additional info data
    double xError, yError, zError;
    double confidence;
    double p_Throttle, i_Throttle, d_Throttle, maxI;
    double p_pitch, i_pitch, d_pitch;
    double p_roll, i_roll, d_roll;
    double imageDistance;
    //data
    private float pitch; // The movement of the drawn forward and backward
    //    private float yaw;
    private float roll; // The velocity rotation on the X axis
    private float verticalThrottle = 0;
    private float gimbalPitch = 0;
    private VerticalControlMode controllMode;
    //constructor

    public ControlCommand(float pitch, float roll, float verticalThrottle) {
        this.pitch = pitch;
        this.roll = roll;

        this.verticalThrottle = verticalThrottle;
    }


    //functions

    public void setErr(double confidence, double xErr, double yErr, double zErr) {
        this.confidence = confidence;
        xError = xErr;
        yError = yErr;
        zError = zErr;

    }

    public void setPID(double p_Throttle, double i_Throttle, double d_Throttle, double p_pitch, double i_pitch, double d_pitch, double p_roll, double i_roll, double d_roll, double maxI) {
        this.p_roll = p_roll;
        this.i_roll = i_roll;
        this.d_roll = d_roll;
        this.p_pitch = p_pitch;
        this.i_pitch = i_pitch;
        this.d_pitch = d_pitch;
        this.p_Throttle = p_Throttle;
        this.i_Throttle = i_Throttle;
        this.d_Throttle = d_Throttle;
        this.maxI = maxI;
    }

    public void setImageDistance(double imageDistance) {
        this.imageDistance = imageDistance;
    }


    public float getRoll() {
        return roll;
    }

    public VerticalControlMode getControllMode() {
        return controllMode;
    }

    public float getPitch() {
        return pitch;
    }


    public float getVerticalThrottle() {
        return verticalThrottle;
    }

    public float getGimbalPitch() {
        return gimbalPitch;
    }

}
