package com.dji.sdk.sample.demo.speechToText;
import android.util.Log;

import com.dji.sdk.sample.demo.kcgremotecontroller.KcgLog;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;

import java.util.Timer;
import java.util.TimerTask;

import dji.common.battery.BatteryState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

public class FlightCommandsAPI {

    private FlightController flightController;
    private KcgLog log;
    private Aircraft aircraft;
    private FlightControlData flightcontroldata;
    private volatile float pitch, roll, yaw, throttle, gimbal_pitch;
    private volatile String command_name;
    private static final String TAG = FlightCommandsAPI.class.getName();

    public FlightCommandsAPI(KcgLog main_log){
        initFlightController();
        initListeners();
        // init log variables
        log = main_log;

        roll = 0;
        pitch = 0;
        yaw = 0;
        throttle = 0;
        command_name = "Empty";
    }

    public void takeoff(){
        flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
//                log.setDebug("Takeoff successfully!");
            }
        });
        this.setCommandTimerTask();
    }

    public void land(){
        flightController.getFlightAssistant().setLandingProtectionEnabled(false, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
//                if (djiError != null){
//                    log.setDebug(djiError.toString());
//                }
//                else
//                    log.setDebug("Disabled protection mode");
            }
        });
        flightController.startLanding(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
//                if (djiError != null){
//                    log.setDebug(djiError.toString());
//                }
//                else
//                    log.setDebug("Started Landing!");

            }
        });
    }

    public void stayOnPlace(){
        roll = 0;
        pitch = 0;
        yaw = 0;
        throttle = 0;
        gimbal_pitch = 0;
        command_name = "stay on place";
    }
    public void set_pitch(float new_pitch, String command){
        roll = 0;
        pitch = new_pitch;
        yaw = 0;
        throttle = 0;
        gimbal_pitch = 0;
        command_name = command;
    }

    public void set_yaw(float new_yaw, String command){
        flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
        roll = 0;
        pitch = 0;
        yaw = new_yaw;
        throttle = 0;
        gimbal_pitch = 0;
        command_name = command;
    }

    public void set_roll(float new_roll, String command){
        roll = new_roll;
        pitch = 0;
        yaw = 0;
        throttle = 0;
        gimbal_pitch = 0;
        command_name = command;
    }

    public void set_throttle(float new_throttle, String command){
        roll = 0;
        pitch = 0;
        yaw = 0;
        throttle = new_throttle;
        gimbal_pitch = 0;
        command_name = command;
    }

    private void setCommandTimerTask(){
        TimerTask task = new TimerTask() {
            @Override
            public void run(){
                setControlCommand();
            }
        };
        Timer timer = new Timer();
        // 5 Hz rate
        timer.schedule(task, 0, 100);
//        log.setDebug("Command Timer Task set successfully!");
    }

    public void setControlCommand(){
        /*
            This method receives flight params and sends a command to the drone
         */
        // Pitch & Roll are opposite, why?
        flightcontroldata.setPitch(roll);
        flightcontroldata.setRoll(pitch);
        flightcontroldata.setYaw(yaw);
        flightcontroldata.setVerticalThrottle(throttle);
//        log.setDebug((Boolean.toString(flightController.isVirtualStickControlModeAvailable())));
        if (flightController.isVirtualStickControlModeAvailable()) {
            flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            flightController.sendVirtualStickFlightControlData(flightcontroldata, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
//                    if (djiError != null){
//                        log.setDebug(command_name + " " + djiError.toString());
//                    }
//                    else
//                        log.setDebug("Command " + command_name + " sent successfully");
                }
            });
        }
    }

    private void initListeners(){
        aircraft.getBattery().setStateCallback(new BatteryState.Callback() {
            @Override
            public void onUpdate(BatteryState batteryState) {
//                bat_status.setText(Integer.toString(batteryState.getLifetimeRemaining()));
            }
        });
    }

    private void enableVirtualStick() {
        flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
//                if (djiError != null){
//                    log.setDebug(djiError.toString());
//                }
//                else
//                    log.setDebug("Set Virtual Stick Mode to True");
            }
        });
    }

    private void disableVirtualStick() {
        flightController.setVirtualStickAdvancedModeEnabled(false);
    }




    private void initFlightController() {
        try {
            flightcontroldata = new FlightControlData(0, 0, 0, 0);
            aircraft = DJISampleApplication.getAircraftInstance();
            if (aircraft == null || !aircraft.isConnected()) {
                //showToast("Disconnected");
                flightController = null;
            } else {
                // re-init flight controller only if needed
                if (flightController == null) {
                    flightController = aircraft.getFlightController();
                    flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                    flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                    flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
                    flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
                }
            }
            this.enableVirtualStick();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }


}
