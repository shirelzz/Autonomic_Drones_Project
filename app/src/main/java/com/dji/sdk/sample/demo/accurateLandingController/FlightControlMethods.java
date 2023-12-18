package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.utils.ToastUtils.setResultToToast;

import com.dji.sdk.sample.demo.flightcontroller.VirtualStickView;
import com.dji.sdk.sample.demo.kcgremotecontroller.gimbelListener;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.CallbackHandlers;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.ToastUtils;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.error.DJIError;
import dji.common.flightcontroller.flightassistant.FillLightMode;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.gimbal.CapabilityKey;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.util.CommonCallbacks;
import dji.common.util.DJIParamCapability;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.mission.followme.FollowMeHeading;
import dji.common.mission.followme.FollowMeMission;
import dji.common.model.LocationCoordinate2D;
import dji.common.error.DJIError;
import dji.common.flightcontroller.simulator.InitializationData;
import dji.common.flightcontroller.simulator.SimulatorState;
import dji.common.flightcontroller.virtualstick.FlightControlData;


import dji.common.error.DJIFlightControllerError;
import dji.common.util.CommonCallbacks;

import java.util.Timer;
import java.util.TimerTask;

/**
 * FlightControlMethods class provides methods to control the drone's movement using virtual sticks.
 * It interfaces with the DJI SDK's FlightController to send virtual stick commands.
 */
public class FlightControlMethods {

    private float pitch;
    private float roll;
    private float yaw;
    private float throttle;
    private FlightController flightController;
    private boolean virtualSticksEnabled;

    /**
     * Default constructor initializes the FlightControlMethods class
     * and sets up the FlightController.
     */
    public FlightControlMethods() {
        flightController = getFlightController();
        configureFlightController();
    }

    /**
     * Documentation (including tables) of parameters here:
     * <a href="https://developer.dji.com/mobile-sdk/documentation/introduction/component-guide-flightController.html#virtual-sticks">...</a>
     */
    private void configureFlightController(){

        /*
        Coordinate System:
        Either Ground or Body coordinate system can be chosen.
        All horizontal movement commands (X, Y, pitch, roll) will be relative to the coordinate system.
         */
        flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

        /*
        Roll Pitch Control Mode:
        Virtual stick commands to move the aircraft horizontally can either be set with X/Y velocities,
        or roll/pitch angles.
        Larger roll and pitch angles result in larger Y and X velocities respectively.
        Roll and pitch angles are always relative to the horizontal.
        Roll and pitch directions are dependent on the coordinate system, and can be confusing.
        For convenience a table detailing how the aircraft moves depending on coordinate system
        and roll pitch control mode is given below.
        These can all be calculated using the definition of the coordinate systems.
         */
        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);

        /*
        Yaw Control Mode:
        Can be set to Angular Velocity Mode or Angle Mode.
        In Angular Velocity mode the yaw argument specifies the speed of rotation,
        in degrees/second, and yaw is affected by the coordinate system being used.
        When Yaw Control Mode is set to Angle Mode,
        value will be interpreted as an angle in the Ground Coordinate System.
        Please make sure that you select the right coordinate system.
         */
        flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);


        /*
        Vertical Throttle Control Mode:
        Vertical movement can be achieved either using velocity or position.
        Position is an altitude relative to the take-off location.
        Velocity is always relative to the aircraft,
        and does not follow typical coordinate system convention
        (positive vertical velocity results in the aircraft ascending).
         */
        flightController.setVerticalControlMode(VerticalControlMode.POSITION);

    }

    /**
     * Gets the FlightController instance from the DJI SDK.
     * @return FlightController instance
     */
    private FlightController getFlightController() {
        return DJISampleApplication.getAircraftInstance().getFlightController();
    }

    /**
     * Commands the drone to pitch (tilt forward or backward).
     * @param pitch value
     */
    public void goPitch(float pitch) {
        sendVirtualStickCommands(pitch, 0, 0, 0);
    }

    /**
     * Commands the drone to roll (tilt left or right).
     * @param roll value
     */
    public void goRoll(float roll) {
        sendVirtualStickCommands(0, roll, 0, 0);
    }


    /**
     * Commands the drone to yaw (rotate left or right).
     * @param yaw value
     */
    public void goYaw(float yaw) {
        sendVirtualStickCommands(0, 0, yaw, 0);
    }

    /**
     * Commands the drone to change throttle (altitude control).
     * @param throttle value
     */
    public void goThrottle(float throttle) {
        sendVirtualStickCommands(0, 0, 0, throttle);
    }

    /**
     * Sends virtual stick commands to the FlightController to control the drone's movement.
     *
     * @param pX Pitch control value
     * @param pY Roll control value
     * @param pZ Yaw control value
     * @param pThrottle Throttle control value
     */
    private void sendVirtualStickCommands(final float pX, final float pY, final float pZ, final float pThrottle){

        // Maximum control speeds
        float pitchJoyControlMaxSpeed = 10;
        float rollJoyControlMaxSpeed = 10;
        float yawJoyControlMaxSpeed = 30;
        float verticalJoyControlMaxSpeed = 2;

        // Set pitch, roll, yaw, throttle
        float mPitch = (float)(pitchJoyControlMaxSpeed * pX);        // forward-backwards
        float mRoll = (float)(rollJoyControlMaxSpeed * pY);          // left-right
        float mYaw = (float)(yawJoyControlMaxSpeed * pZ);          // tilt right/left
        float mThrottle = (float)(verticalJoyControlMaxSpeed * pThrottle);  // height

        if (flightController != null) {

            // If virtual stick is enabled, send the command, otherwise turn it on
            if (virtualSticksEnabled){

                FlightControlData flightControlData = new FlightControlData(0,0,0,0);
                flightControlData.setPitch(mPitch);
                flightControlData.setRoll(mRoll);
                flightControlData.setYaw(mYaw);
                flightControlData.setVerticalThrottle(mThrottle);
                flightController.sendVirtualStickFlightControlData(flightControlData, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError!=null){
                                    setResultToToast(djiError.getDescription());
                                }
                            }
                        }
                );
            }
            else {
                setResultToToast("flight controller virtual mode off");

                // If virtual stick is not enabled, enable
                flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null){
                            setResultToToast(djiError.getDescription());
                        }else
                        {
                            setResultToToast("Enable Virtual Stick Success");
                            virtualSticksEnabled = true;
                            sendVirtualStickCommands(pX, pY, pZ, pThrottle);

                        }
                    }
                });
            }
        }
        else{
            setResultToToast("Flight Controller Null");
        }
    }
}