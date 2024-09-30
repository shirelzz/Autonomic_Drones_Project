package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.utils.ToastUtils.setResultToToast;
import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.ToastUtils;

import java.util.Objects;

import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.sdk.flightcontroller.FlightController;

/**
 * FlightControlMethods class provides methods to control the drone's movement using virtual sticks.
 * It interfaces with the DJI SDK's FlightController to send virtual stick commands.
 */
public class FlightControlMethods {

    private static final int CONTROL_DURATION = 3000; // Duration in milliseconds (3 seconds)
    // Maximum control speeds
    protected final float pitchJoyControlMaxSpeed = 4;
    protected final float rollJoyControlMaxSpeed = 4;
    protected final float yawJoyControlMaxSpeed = 10;
    protected final float throttleJoyControlMaxSpeed = 4;
    private final FlightController flightController;
    Double PP = 0.5, II = 0.02, DD = 0.01, MAX_I = 0.5;
    private float descentRate = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private VLD_PID roll_pid = new VLD_PID(PP, II, DD, MAX_I); //
    private VLD_PID pitch_pid = new VLD_PID(PP, II, DD, MAX_I);
    private VLD_PID yaw_pid = new VLD_PID(PP, II, DD, MAX_I);
    private VLD_PID throttle_pid = new VLD_PID(PP, II, DD, MAX_I);
    private boolean virtualStickEnabled = false;
    //    private boolean inLandingMode = false;
    private long startTime; // Record the start time


    /**
     * Default constructor initializes the FlightControlMethods class
     * and sets up the FlightController.
     */
    public FlightControlMethods() {
        flightController = getFlightControllerFromAircraft();
        configureFlightController();

    }

    /**
     * Documentation (including tables) of parameters here:
     * <a href="https://developer.dji.com/mobile-sdk/documentation/introduction/component-guide-flightController.html#virtual-sticks">...</a>
     */
    private void configureFlightController() {

        if (flightController == null) {
            System.out.println("flightController is null");
            return;
        }

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
//        flightController.setYawControlMode(YawControlMode.ANGLE); // Asaf


        /*
        Vertical Throttle Control Mode:
        Vertical movement can be achieved either using velocity or position.
        Position is an altitude relative to the take-off location.
        Velocity is always relative to the aircraft,
        and does not follow typical coordinate system convention
        (positive vertical velocity results in the aircraft ascending).
         */
//        flightController.setVerticalControlMode(VerticalControlMode.POSITION);
        flightController.setVerticalControlMode(VerticalControlMode.VELOCITY); // Asaf


//        FlightController flightController = Objects.requireNonNull(DJISampleApplication.getAircraftInstance()).getFlightController();
//        Objects.requireNonNull(flightController.getFlightAssistant()).setLandingProtectionEnabled(false, djiError -> {
//
//        });
//
//        flightController.getFlightAssistant().setActiveObstacleAvoidanceEnabled(false, djiError -> {
//
//        });
//
//        flightController.getFlightAssistant().setCollisionAvoidanceEnabled(false, djiError -> {
//
//        });

    }

    public void initPIDs(double p, double i, double d, double max_i, String type) {

        if (type.equals("roll")) {
            if (roll_pid == null) {
                roll_pid = new VLD_PID(p, i, d, max_i);
            } else {
                roll_pid.setPID(p, i, d, max_i);
            }
        }

        if (type.equals("pitch")) {
            if (pitch_pid == null) {
                pitch_pid = new VLD_PID(p, i, d, max_i);
            } else {
                pitch_pid.setPID(p, i, d, max_i);
            }
        }

        if (type.equals("throttle")) {
            if (throttle_pid == null) {
                throttle_pid = new VLD_PID(p, i, d, max_i);
            } else {
                throttle_pid.setPID(p, i, d, max_i);
            }
        }


//        if (roll_pid == null) {
//            roll_pid = new VLD_PID(p, i, d, max_i);
//            pitch_pid = new VLD_PID(p, i, d, max_i);
//            throttle_pid = new VLD_PID(p, i, d, max_i);
//        }
//        else{
//            roll_pid.setPID(p, i, d, max_i);
//            pitch_pid.setPID(p, i, d, max_i);
//            throttle_pid.setPID(p, i, d, max_i);
//        }
    }

    public void setDescentRate(float descentRate) {
        if (descentRate > 0) {
            descentRate = -descentRate;
        }

        this.descentRate = descentRate;
    }

    public ControlCommand stayOnPlace() {
        //  מאפס את כל הערכים לאפס - מתייצב
//        roll_pid.reset();
//        pitch_pid.reset();
        float t = 0;
        float r = 0;
        float p = 0;
//        y = 0;
//        need to set another gp
        ControlCommand ans = new ControlCommand(p, r, t);
        ans.setErr(1000, 0, 0, 0);
        ans.setPID(throttle_pid.getP(), throttle_pid.getI(), throttle_pid.getD(), pitch_pid.getP(), pitch_pid.getI(), pitch_pid.getD(), roll_pid.getP(), roll_pid.getI(), roll_pid.getD(), roll_pid.getMax_i());
        ans.setImageDistance(-1);

        return ans;
    }

    public void land(Runnable function1, Runnable function2) {
        flightController.startLanding(djiError -> {
            if (djiError != null)
                showToast(djiError.getDescription());
        });
        flightController.confirmLanding(djiError -> {
            if (djiError != null) {
                showToast(djiError.getDescription());
            } else {
                showToast("land correctly");
                if (function1 != null)
                    function1.run();
                if (function2 != null)
                    function2.run();
            }
        });
    }

    /**
     * Gets the FlightController instance from the DJI SDK.
     *
     * @return FlightController instance
     */
    private FlightController getFlightControllerFromAircraft() {
        return DJISampleApplication.getAircraftInstance().getFlightController();
    }

    public FlightController getFlightController() {
        try {
            if (flightController != null) {
                return flightController;
            }
        } catch (Exception e) {
            Log.d("Null", "FlightControlMethods: flightController is null");
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Commands the drone to pitch (tilt forward or backward).
     *
     * @param pitch value
     */
    public void goPitch(float pitch) {
        ControlCommand command = new ControlCommand(pitch, 0, 0);
        // Send virtual stick commands to the drone
        sendVirtualStickCommands(command, 0);
    }

    /**
     * Commands the drone to roll (tilt left or right).
     *
     * @param roll value
     */
    public void goRoll(float roll) {
        ControlCommand command = new ControlCommand(0, roll, 0);
        // Send virtual stick commands to the drone
        sendVirtualStickCommands(command, 0);
    }

    public void takeOff() {
        flightController.startTakeoff(djiError -> {
            if (djiError != null) {
                showToast(djiError.getDescription());
            }
        });
    }


    /**
     * Commands the drone to yaw (rotate left or right).
     *
     * @param yaw value
     */
    public void goYaw(float yaw) {
        ControlCommand command = new ControlCommand(0, 0, 0);
        // Send virtual stick commands to the drone
        sendVirtualStickCommands(command, yaw);
    }

    /**
     * Commands the drone to change throttle (altitude control).
     *
     * @param throttle value
     */
    public void goThrottle(float throttle) {
        ControlCommand command = new ControlCommand(0, 0, throttle);
        // Send virtual stick commands to the drone
        sendVirtualStickCommands(command, 0);
    }

    /**
     * Sends virtual stick commands to the FlightController to control the drone's movement.
     *
     * @param command control command the contains init the pitch, roll and throttle commands values
     * @param pZ      Yaw control value
     */
    public void sendVirtualStickCommands(ControlCommand command, float pZ) {
        Log.d("log:  ", "in sendVirtualStickCommands");
        if (Objects.isNull(startTime)) {
            startTime = System.currentTimeMillis();
        }
        if (flightController != null) {

            // If virtual stick is enabled, send the command, otherwise turn it on
            if (!virtualStickEnabled) {
                // If virtual stick is not enabled, enable
                flightController.setVirtualStickModeEnabled(true, djiError -> {
                    if (djiError != null) {
                        setResultToToast(djiError.getDescription());
                    } else {
                        virtualStickEnabled = true;
//                        sendVirtualStickCommands(command, pZ);
                    }
                });
            }

            flightController.setRollPitchControlMode(command.getPitchMode());

            FlightControlData flightControlData = new FlightControlData(0, 0, 0, 0);

            //pitch and roll are opposite

            // Sets the aircraft's velocity (m/s) along the y-axis or angle value (in degrees) for pitch
            if (command.getPitchMode() == RollPitchControlMode.VELOCITY) {
                flightControlData.setPitch(command.getRoll());
                flightControlData.setRoll(command.getPitch());
            } else {
                flightControlData.setPitch(command.getPitch());
                // Sets the aircraft's velocity (m/s) along the x-axis or angle value (in degrees) for roll
                flightControlData.setRoll(command.getRoll());
            }

            // Sets the angular velocity (degrees/s) or angle (degrees) value for yaw
            flightControlData.setYaw((float) pZ * yawJoyControlMaxSpeed);
//            flightControlData.setYaw(0.0f);
            // Sets the aircraft's velocity (m/s) or altitude (m) value for verticalControl

            flightControlData.setVerticalThrottle(command.getVerticalThrottle());
            Log.i("command:  ", flightControlData.getPitch() + ", " + flightControlData.getRoll() + ", " + flightControlData.getYaw() + ", " + flightControlData.getVerticalThrottle());

//                if (command.getControllMode() == VerticalControlMode.VELOCITY) {
//                if(confirmLand )
//                    flightController.confirmLanding(djiError -> {
//                        showToast("hey");
//                        String finalText = "";
//
//                        if (djiError != null) {
////            showToast(""+djiError);
////            err+=djiError;
////            ep=p;er=r;et=t;
//
//                            finalText += "djiErr: " + djiError;
//                            showToast(djiError.getDescription());
//                        }
//                    });
//                }

            flightController.sendVirtualStickFlightControlData(flightControlData, djiError -> {
                        Log.i("djiError", String.valueOf(djiError));
                        ToastUtils.showToast("VS:" + flightControlData.getPitch() + ", " + flightControlData.getRoll() + ", " + flightControlData.getYaw() + ", " + flightControlData.getVerticalThrottle());
//                        movementFinished = true;
//                            if (command.getControllMode() == VerticalControlMode.VELOCITY) {
//
//                            }
                    }
            );
        } else {
            setResultToToast("Flight Controller Null");
        }
    }

    public void disableVirtualStickControl() {
        // Disable virtual stick control
        flightController.sendVirtualStickFlightControlData(new FlightControlData(0, 0, 0, 0), djiError -> {
                    if (djiError != null) {

                        flightController.setVirtualStickModeEnabled(false, djiError1 -> {
                            if (djiError1 != null) {
                                setResultToToast(djiError1.getDescription());
                            } else {
                                virtualStickEnabled = false;
                            }
                        });
                    }
                }
        );
    }
}