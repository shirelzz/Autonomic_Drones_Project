package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.controller.DJISampleApplication.getProductInstance;

import com.dji.sdk.sample.demo.kcgremotecontroller.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import dji.common.battery.BatteryState;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.adsb.AirSenseSystemInformation;
import dji.common.gimbal.GimbalState;
import dji.sdk.flightcontroller.FlightController;

public class DataFromDrone {

    private final double[] GPS = new double[3]; //[latitude, longitude, altitude]
    private final double[] velocity = new double[3];
    private double headDirection = 0.0;
    private double yaw = 0.0;
    private double pitch = 0.0;
    private double roll = 0.0;
    private double gimbalPitch = 0.0;
    private double batRemainingTime = 0.0;
    private double batCharge = 0.0;
    private final FlightController flightController = Objects.requireNonNull(DJISampleApplication.getAircraftInstance()).getFlightController();

    public DataFromDrone(AccuracyLog accuracyLog) {
        initStateListeners();
    }

    public double[] getGPS() {
        return GPS;
    }

    public void updateGPS() {
        LocationCoordinate3D locationCoordinate3D = flightController.getState().getAircraftLocation();
        this.GPS[0] = locationCoordinate3D.getLatitude();
        this.GPS[1] = locationCoordinate3D.getLongitude();
        this.GPS[2] = locationCoordinate3D.getAltitude();
    }

    public double[] getVelocity() {
        return velocity;
    }

    public void updateVelocity() {
        this.GPS[0] = flightController.getState().getVelocityX();
        this.GPS[1] = flightController.getState().getVelocityY();
        this.GPS[2] = flightController.getState().getVelocityZ();
    }

    public double getHeadDirection() {
        return headDirection;
    }

    public void setHeadDirection() {
        this.headDirection = flightController.getState().getAircraftHeadDirection();
    }

    public double getYaw() {
        return yaw;
    }

    public void setYaw(double yaw) {
        this.yaw = flightController.getState().getAttitude().yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch() {
        this.pitch = flightController.getState().getAttitude().pitch;
    }

    public double getRoll() {
        return roll;
    }

    public void setRoll() {
        this.roll = flightController.getState().getAttitude().roll;
    }

    public double getGimbalPitch() {
        return gimbalPitch;
    }

    public void setGimbalPitch(double gimbalPitch) {
        this.gimbalPitch = gimbalPitch;
    }

    public double getBatRemainingTime() {
        return batRemainingTime;
    }

    public void setBatRemainingTime(double batRemainingTime) {
        this.batRemainingTime = batRemainingTime;
    }

    public double getBatCharge() {
        return batCharge;
    }

    public void setBatCharge(double batCharge) {
        this.batCharge = batCharge;
    }

    private void initStateListeners() {
        if (ModuleVerificationUtil.isFlightControllerAvailable()) {

            Objects.requireNonNull(DJISampleApplication.getAircraftInstance()).getFlightController().setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(FlightControllerState flightControllerState) {

                    //get drone location
                    GPS[0] = flightControllerState.getAircraftLocation().getLatitude();
                    GPS[1] = flightControllerState.getAircraftLocation().getLongitude();
                    GPS[2] = flightControllerState.getAircraftLocation().getAltitude();
                    headDirection = flightControllerState.getAircraftHeadDirection();

                    //get drone velocity
                    velocity[0] = flightControllerState.getVelocityX();
                    velocity[1] = flightControllerState.getVelocityY();
                    velocity[2] = flightControllerState.getVelocityZ();

                    //get drone attitude
                    yaw = flightControllerState.getAttitude().yaw;
                    pitch = flightControllerState.getAttitude().pitch;
                    roll = flightControllerState.getAttitude().roll;


//                    controller.addTelemetryLog(droneTelemetry);

                }
            });
        }

        if (ModuleVerificationUtil.isGimbalModuleAvailable()) {
            getProductInstance().getGimbal().setStateCallback(new GimbalState.Callback() {
                @Override
                public void onUpdate(GimbalState gimbalState) {
                    gimbalPitch = -1 * gimbalState.getAttitudeInDegrees().getPitch();

                }
            });
        }

        Objects.requireNonNull(DJISampleApplication.getAircraftInstance()).getBattery().setStateCallback(new BatteryState.Callback() {
            @Override
            public void onUpdate(BatteryState batteryState) {
                batRemainingTime = batteryState.getLifetimeRemaining();
                batCharge = batteryState.getChargeRemainingInPercent();

            }
        });

        DJISampleApplication.getAircraftInstance().getFlightController().setASBInformationCallback(new AirSenseSystemInformation.Callback() {
            @Override
            public void onUpdate(AirSenseSystemInformation airSenseSystemInformation) {
//                controller.addPlanesLog(airSenseSystemInformation.getAirplaneStates());

            }
        });


    }

    private Map<String, Double> getAll() {
        Map<String, Double> droneTelemetry = new HashMap<>();
        droneTelemetry.put("lat", GPS[0]);
        droneTelemetry.put("lon", GPS[1]);
        droneTelemetry.put("alt", GPS[2]);

        droneTelemetry.put("HeadDirection", headDirection);

        //get drone velocity
        droneTelemetry.put("velX", velocity[0]);
        droneTelemetry.put("velY", velocity[1]);
        droneTelemetry.put("velZ", velocity[2]);

        //get drone attitude
        droneTelemetry.put("yaw", yaw);
        droneTelemetry.put("pitch", pitch);
        droneTelemetry.put("roll", roll);

        //get gimbal pitch
        droneTelemetry.put("gimbalPitch", gimbalPitch);

        //battery data
        droneTelemetry.put("batRemainingTime", batRemainingTime);
        droneTelemetry.put("batCharge", batCharge);

        return droneTelemetry;
    }
}
