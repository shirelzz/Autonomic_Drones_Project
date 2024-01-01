package com.dji.sdk.sample.demo.accurateLandingController;

import android.widget.TextView;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.mission.followme.FollowMeHeading;
import dji.common.mission.followme.FollowMeMissionState;
import dji.common.util.CommonCallbacks;
import dji.sdk.mission.followme.FollowMeMissionOperator;
//import dji.waypointv2.common.waypointv1.LocationCoordinate2D;
import dji.common.model.LocationCoordinate2D;
import dji.common.mission.followme.FollowMeMission;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.flightcontroller.FlightController;


/*
 https://github.com/ZenZenDaMie/DJAutocontrol/blob/master/app/src/main/java/com/mrbluyee/djautocontrol/application/RemoteControlApplication.java
 https://stackoverflow.com/questions/43291644/custom-coordinates-on-follow-me-mission-dji-mobile-sdk-for-android
 https://stackoverflow.com/questions/54539410/how-to-implement-follow-me-function-into-android-dji-app/54612216#54612216
 https://stackoverflow.com/questions/48404526/custom-follow-me-mission-dji-android-sdk
 */
public class MissionControlWrapper {
    FlightControllerState flightController;

    private FollowMeMissionOperator fmmo;
    //    private FollowMeMission fmm;

    private LocationCoordinate2D targetLocation;
    private float altitude;
    TextView missionStateTextView;
    private String lastState = "";



    public  MissionControlWrapper(LocationCoordinate2D targetLocation, float altitude, FlightController fc) {
        this.flightController = flightController;
        this.targetLocation = targetLocation;
        this.altitude = altitude;
    }

//    public void startSimpleFollowMe() {
//        if (fmmo == null) {
//            fmmo = getFollowMeMissionOperator();
//        }
//
//        final FollowMeMissionOperator followMeMissionOperator = fmmo;
//        if (followMeMissionOperator.getCurrentState().equals(FollowMeMissionState.READY_TO_EXECUTE)) {
//            followMeMissionOperator.startMission(getFollowMeMission(), new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onResult(DJIError djiError) {
//                    if (djiError != null) {
//                        setLastState(djiError.getDescription());
//                    } else {
//                        setLastState("Mission Start: Successfully");
//                    }
//                }
//            });
//        }
//    }



//    public void updateSimpleFollowMe() {
//        if (fmmo == null) {
//            fmmo = getFollowMeMissionOperator();
//        }
//
//        final FollowMeMissionOperator followMeMissionOperator = fmmo;
//        if (followMeMissionOperator.getCurrentState().equals(FollowMeMissionState.EXECUTING)) {
//            followMeMissionOperator.updateFollowingTarget(
//                    new LocationCoordinate2D(targetLocation.getLatitude(), targetLocation.getLongitude()),
//                    new CommonCallbacks.CompletionCallback() {
//                        @Override
//                        public void onResult(DJIError error) {
//                            if (error != null) {
//                                setLastState(followMeMissionOperator.getCurrentState().getName().toString() + " " + error.getDescription());
//                            } else {
//                                setLastState("Mission Update Successfully");
//                            }
//                        }
//                    });
//        }
//    }
    private void setLastState(String state) {
        lastState = state;
        missionStateTextView.setText(state);
    }

    public String getLastState() {
        return lastState;
    }

    private boolean isGpsSignalStrongEnough() {
        flightController.getGPSSignalLevel();

        GPSSignalLevel gpsSignalLevel = flightController.getGPSSignalLevel();
        return gpsSignalLevel.value() >= 2;
        // level 2: The GPS signal is weak. At this level, the aircraft's go home functionality will still work.
        // level 3: The GPS signal is good. At this level, the aircraft can hover in the air.
        // level 4: The GPS signal is very good. At this level, the aircraft can record the home point.


    }

    private FollowMeMissionOperator getFollowMeMissionOperator() {
        // Example using a hypothetical SDKManager:
        DJISDKManager djiSdkManager = DJISDKManager.getInstance();
        if (djiSdkManager != null) {
            return djiSdkManager.getMissionControl().getFollowMeMissionOperator();
        }
        // Handle the case where FollowMeMissionOperator is not available
        return null;
    }

    private FollowMeMission getFollowMeMission(double longitude, double latitude, double altitude) {
        // Provide appropriate values for heading, v, v1, and v2 based on your mission requirements and SDK documentation
        FollowMeMission followMeMission = new FollowMeMission(FollowMeHeading.TOWARD_FOLLOW_POSITION,
                                                                longitude,
                                                                latitude,
                                                                (float) altitude); // check this!

        // Configure other mission parameters as needed, referring to the SDK documentation

        return followMeMission;
    }

    // Implement other methods for mission control and error handling, as needed

    public void startGoToMission() {

        if (!isGpsSignalStrongEnough()) {
            setLastState("GPS signal is not strong enough");
            return;
        }

        FollowMeMissionOperator fmmo = getFollowMeMissionOperator();
        if (fmmo != null && fmmo.getCurrentState() == FollowMeMissionState.READY_TO_EXECUTE) {
            fmmo.startMission(getFollowMeMission(targetLocation.getLongitude(), targetLocation.getLatitude(), altitude),
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

        }
    }

    public void updateGoToMission(double longitude, double latitude) { // altitude?

        if (!isGpsSignalStrongEnough()) {
            setLastState("GPS signal is not strong enough");
            return;
        }

        LocationCoordinate2D updatedTarget = new LocationCoordinate2D(longitude, latitude);

        FollowMeMissionOperator fmmo = getFollowMeMissionOperator();
        if (fmmo != null && fmmo.getCurrentState() == FollowMeMissionState.READY_TO_EXECUTE) {
            fmmo.updateFollowingTarget(updatedTarget ,
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

        }
    }


    // Use this method to update the target location during the mission if needed
//    public void updateTargetLocation(double longitude, double latitude) {
//        FollowMeMissionOperator fmmo = getFollowMeMissionOperator();
//        if (fmmo != null && fmmo.getCurrentState() == FollowMeMissionState.EXECUTING) {
//            fmmo.updateFollowingTarget(new LocationCoordinate2D(latitude, longitude), new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onResult(DJIError error) {
//                    if (error == null) {
//                        // Target location updated successfully
//                    } else {
//                        // Handle target update error
//                    }
//                }
//            });
//        }
//    }

}


