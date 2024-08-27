package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.controller.DJISampleApplication.getProductInstance;

import com.dji.sdk.sample.demo.kcgremotecontroller.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import dji.common.airlink.SignalQualityCallback;
import dji.common.battery.BatteryState;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.adsb.AirSenseSystemInformation;
import dji.common.gimbal.GimbalState;
import dji.sdk.airlink.AirLink;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class DataFromDrone {

    //    private final double[] GPS = new double[3]; //[latitude, longitude, altitude]
    private final GPSLocation GPS = new GPSLocation(); //[latitude, longitude, altitude]
    private final double[] velocity = new double[3];
    private double headDirection = 0.0;
    private double altitudeBelow = 0.0;
    private double yaw = 0.0;
    private double pitch = 0.0;
    private double roll = 0.0;
    private double gimbalPitch = 0.0;
    private double batRemainingTime = 0.0;
    private boolean isUltrasonicBeingUsed = false;
    private double batCharge = 0.0;
    private int satelliteCount = 0;
    private GPSSignalLevel gpsSignalLevel;
    private int signalQuality = 0;
    private FlightController flightController;

    public DataFromDrone() {
        initStateListeners();
        initFlightController();
    }

    public GPSLocation getGPS() {
        return GPS;
    }

    public double[] getVelocity() {
        return velocity;
    }

    public double getHeadDirection() {
        return headDirection;
    }

    public double getYaw() {
        return yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public double getRoll() {
        return roll;
    }

    public double getGimbalPitch() {
        return gimbalPitch;
    }

    public double getBatRemainingTime() {
        return batRemainingTime;
    }

    public double getBatCharge() {
        return batCharge;
    }

    public int getSatelliteCount() {
        return satelliteCount;
    }

    public GPSSignalLevel getGpsSignalLevel() {
        return gpsSignalLevel;
    }

    public int getSignalQuality() {
        return signalQuality;
    }

    public double getAltitudeBelow() {
        return altitudeBelow;
    }

    public boolean isUltrasonicBeingUsed() {
        return isUltrasonicBeingUsed;
    }

    // Initialize the flight controller
    private void initFlightController() {
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product != null && product instanceof Aircraft) {
            flightController = ((Aircraft) product).getFlightController();
        }
    }

    private void initStateListeners() {
        if (ModuleVerificationUtil.isFlightControllerAvailable()) {
            Objects.requireNonNull(DJISampleApplication.getAircraftInstance()).getFlightController().setStateCallback(flightControllerState -> {

                gpsSignalLevel = flightControllerState.getGPSSignalLevel();
                isUltrasonicBeingUsed = flightControllerState.isUltrasonicBeingUsed();
                if (isUltrasonicBeingUsed)
                    altitudeBelow = flightControllerState.getUltrasonicHeightInMeters();

                // Retrieve drone's GPS location
                LocationCoordinate3D aircraftLocation = flightControllerState.getAircraftLocation();

                //get drone location
                if (!Double.isNaN(aircraftLocation.getAltitude()))
                    GPS.setAltitude(aircraftLocation.getAltitude());
                if (!Double.isNaN(aircraftLocation.getLatitude()))
                    GPS.setLatitude(aircraftLocation.getLatitude());
                else {
                    GPS.setLatitude(32.085114);
                }
                if (!Double.isNaN(aircraftLocation.getLongitude()))
                    GPS.setLongitude(aircraftLocation.getLongitude());
                else {
                    GPS.setLongitude(34.852653);
                }
//                    GPS[0] = aircraftLocation.getLatitude();
//                    GPS[1] = aircraftLocation.getLongitude();
//                    GPS[2] = aircraftLocation.getAltitude();
//                    showToast(String.valueOf(aircraftLocation));
                headDirection = flightControllerState.getAircraftHeadDirection();

                //get drone velocity
                velocity[0] = flightControllerState.getVelocityX();
                velocity[1] = flightControllerState.getVelocityY();
                velocity[2] = flightControllerState.getVelocityZ();

                //get drone attitude
                yaw = flightControllerState.getAttitude().yaw;
                pitch = flightControllerState.getAttitude().pitch;
                roll = flightControllerState.getAttitude().roll;

                satelliteCount = flightControllerState.getSatelliteCount();


//                    controller.addTelemetryLog(droneTelemetry);

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
//
        // Assuming you're checking signal quality for the aircraft (drone)
        BaseComponent.ComponentListener componentListener = new BaseComponent.ComponentListener() {
            @Override
            public void onConnectivityChange(boolean isConnected) {
                // Handle the connectivity change
                if (isConnected) {
                    AirLink airLink = DJISDKManager.getInstance().getProduct().getAirLink();

                    // Set up a signal quality callback
                    airLink.setUplinkSignalQualityCallback(new SignalQualityCallback() {
                        @Override
                        public void onUpdate(int index) {
                            signalQuality = index;
                        }
                    });

                    // Optional: To remove the callback when no longer needed
                    // airLink.setSignalQualityUpdatedCallback(null);
                }
            }
        };
    }

    public double[] getCurrentPosition() {
        if (flightController != null) {
            LocationCoordinate3D location = flightController.getState().getAircraftLocation();
            return new double[]{location.getLatitude(), location.getLongitude(), location.getAltitude()};
        }
        return null;
    }

    public Map<String, Double> getAll() {
        Map<String, Double> droneTelemetry = new HashMap<>();
        droneTelemetry.put("lat", GPS.getLatitude());
        droneTelemetry.put("lon", GPS.getLongitude());
        droneTelemetry.put("alt", GPS.getAltitude());
        droneTelemetry.put("altitudeBelow", altitudeBelow);
        droneTelemetry.put("isUltrasonicBeingUsed", isUltrasonicBeingUsed ? 1.0 : 0.0);

        droneTelemetry.put("HeadDirection", headDirection);

        //get drone velocity
        droneTelemetry.put("velX", velocity[0]);
        droneTelemetry.put("velY", velocity[1]);
        droneTelemetry.put("velZ", velocity[2]);

        //get drone attitude
        droneTelemetry.put("yaw", yaw);
        droneTelemetry.put("pitch", pitch);
        droneTelemetry.put("roll", roll);

        //Get satellite Count
        droneTelemetry.put("satelliteCount", (double) satelliteCount);
        droneTelemetry.put("gpsSignalLevel", (double) gpsSignalLevel.value());

        //get gimbal pitch
        droneTelemetry.put("gimbalPitch", gimbalPitch);

        //battery data
        droneTelemetry.put("batRemainingTime", batRemainingTime);
        droneTelemetry.put("batCharge", batCharge);

        //Signal quality remote control
        droneTelemetry.put("signalQuality", (double) signalQuality);

        return droneTelemetry;
    }

}
