package com.dji.sdk.sample.demo.accurateLandingController;

import android.widget.TextView;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.followme.FollowMeHeading;
import dji.common.mission.followme.FollowMeMission;
import dji.common.mission.followme.FollowMeMissionState;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.followme.FollowMeMissionOperator;
import dji.sdk.sdkmanager.DJISDKManager;


/*
 https://github.com/ZenZenDaMie/DJAutocontrol/blob/master/app/src/main/java/com/mrbluyee/djautocontrol/application/RemoteControlApplication.java
 https://stackoverflow.com/questions/43291644/custom-coordinates-on-follow-me-mission-dji-mobile-sdk-for-android
 https://stackoverflow.com/questions/54539410/how-to-implement-follow-me-function-into-android-dji-app/54612216#54612216
 https://stackoverflow.com/questions/48404526/custom-follow-me-mission-dji-android-sdk
 https://stackoverflow.com/questions/54539410/how-to-implement-follow-me-function-into-android-dji-app
 */
public class MissionControlWrapper {
    FlightControllerState flightController;
    TextView missionStateTextView;
    private FollowMeMission fmm;
    private FollowMeMissionOperator fmmo;
    private LocationCoordinate2D targetLocation;
    private float altitude;
    private DataFromDrone dataFromDrone;
    private String lastState = "";


    public MissionControlWrapper(LocationCoordinate2D targetLocation, float altitude, FlightController flightController, DataFromDrone dataFromDrone, TextView missionStateTextView) {
        this.flightController = flightController.getState();
        this.targetLocation = targetLocation;
        this.altitude = altitude;
        this.dataFromDrone = dataFromDrone;
        this.missionStateTextView = missionStateTextView;
    }

    public MissionControlWrapper(FlightController flightController, DataFromDrone dataFromDrone, TextView missionStateTextView) {
        this.flightController = flightController.getState();
        this.dataFromDrone = dataFromDrone;
        this.missionStateTextView = missionStateTextView;
    }

    public void setTargetLocation(LocationCoordinate2D targetLocation) {
        this.targetLocation = targetLocation;
    }

    public void setAltitude(float altitude) {
        this.altitude = altitude;
    }
    public String getLastState() {
        return lastState;
    }

    private void setLastState(String state) {
        lastState = state;
        missionStateTextView.setText(state);
    }

    private boolean isGpsSignalStrongEnough() {
//        flightController.getGPSSignalLevel();
//        GPSSignalLevel gpsSignalLevel = flightController.getGPSSignalLevel();

        return dataFromDrone.getGpsSignalLevel().value() >= 2;
        // level 2: The GPS signal is weak. At this level, the aircraft's go home functionality will still work.
        // level 3: The GPS signal is good. At this level, the aircraft can hover in the air.
        // level 4: The GPS signal is very good. At this level, the aircraft can record the home point.
    }

    private FollowMeMissionOperator getFollowMeMissionOperator() {
        DJISDKManager djiSdkManager = DJISDKManager.getInstance();
        if (djiSdkManager != null) {
            return djiSdkManager.getMissionControl().getFollowMeMissionOperator();
        }
        return null;
    }

    private FollowMeMission getFollowMeMission(double latitude, double longitude, double altitude) {
        // TOWARD_FOLLOW_POSITION - Aircraft's heading remains toward the coordinate it is following.
        FollowMeMission followMeMission = new FollowMeMission(FollowMeHeading.TOWARD_FOLLOW_POSITION,
                latitude,
                longitude,
                (float) altitude); // check this!
        this.fmm = followMeMission;
        return followMeMission;
    }

    public void stopGoToMission() {
        FollowMeMissionOperator fmmo = getFollowMeMissionOperator();
        assert fmmo != null;
        fmmo.stopMission(djiError -> {
            if (djiError == null) {
                setLastState("Mission Stop: Successfully");
            } else {
                setLastState(djiError.getDescription());
            }
        });
    }

    public void startGoToMission() {

        if (!isGpsSignalStrongEnough()) {
            setLastState("GPS signal is not strong enough");
            return;
        }

        FollowMeMissionOperator fmmo = getFollowMeMissionOperator();

        if (fmmo != null && fmmo.getCurrentState() == FollowMeMissionState.READY_TO_EXECUTE) {
            /*
            Invoked when the asynchronous operation completes.
            If the operation completes successfully, error will be null.
            Override to handle in your own code.
            */
            fmmo.startMission(getFollowMeMission(targetLocation.getLatitude(), targetLocation.getLongitude(), altitude),
                    djiError -> {
                        if (djiError == null) {
                            setLastState("Mission Start: Successfully");
                        } else {
                            setLastState(djiError.getDescription());
                        }
                    });
        }
    }

    public void updateGoToMission(double latitude, double longitude) { // , float altitude?

        if (!isGpsSignalStrongEnough()) {
            setLastState("GPS signal is not strong enough");
            return;
        }

        LocationCoordinate2D updatedTarget = new LocationCoordinate2D(latitude, longitude);
        FollowMeMissionOperator fmmo = getFollowMeMissionOperator();

        // READY_TO_EXECUTE or EXECUTING ?
        if (fmmo != null && fmmo.getCurrentState() == FollowMeMissionState.EXECUTING) {
            fmmo.updateFollowingTarget(updatedTarget,
                    new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError == null) {
                                setLastState("Mission Start: Successfully");
                            } else {
                                setLastState(djiError.getDescription());
                            }
                        }
                    });
        } else {
            setLastState("FollowMeMissionOperator is not ready");
        }
    }

}


