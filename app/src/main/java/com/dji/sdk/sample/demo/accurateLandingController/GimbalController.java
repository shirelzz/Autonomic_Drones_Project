package com.dji.sdk.sample.demo.accurateLandingController;

import com.dji.sdk.sample.demo.kcgremotecontroller.Controller;
import com.dji.sdk.sample.demo.kcgremotecontroller.gimbelListener;
import com.dji.sdk.sample.internal.utils.CallbackHandlers;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.ToastUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import dji.common.error.DJIError;
import dji.common.flightcontroller.flightassistant.FillLightMode;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.gimbal.CapabilityKey;
import dji.common.gimbal.GimbalMode;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.util.CommonCallbacks;
import dji.common.util.DJIParamCapability;
import dji.common.util.DJIParamMinMaxCapability;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class GimbalController implements gimbelListener {
    FlightController flightController;
    private float gimbalValue = 0;
    private Controller controller;
    private Gimbal gimbal = null;
    private List<gimbelListener> gimbelListenersList = new LinkedList<gimbelListener>();
    private float prevDegree = -1000;
    private int maxGimbalDegree = 1000;
    private int minGimbalDegree = -1000;
    private int currentGimbalId = 0;


    public GimbalController() {
        if (flightController == null) {
            flightController = ModuleVerificationUtil.getFlightController();
            if (flightController == null) {
                return;
            }
        }

        flightController.setYawControlMode(YawControlMode.ANGLE);
        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        flightController.setYawControlMode(YawControlMode.ANGLE);
        flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
        flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
        initFullGimbal();
    }

    public GimbalController(FlightControlMethods flightControlMethods) {
        flightController = flightControlMethods.getFlightController();
        initFullGimbal();
    }

    private void initFullGimbal(){
        gimbal = getGimbalInstance();
        if (gimbal != null) {
            getGimbalInstance().setMode(GimbalMode.YAW_FOLLOW, new CallbackHandlers.CallbackToastHandler());
        } else {
            ToastUtils.setResultToToast("Ark: Gimbal failed");
        }

        if (!isFeatureSupported(CapabilityKey.ADJUST_PITCH)) {
            ToastUtils.setResultToToast("Ark: Gimbal pitch not supported");
        }

        Object key = CapabilityKey.ADJUST_PITCH;
        minGimbalDegree = ((DJIParamMinMaxCapability) (gimbal.getCapabilities().get(key))).getMin().intValue();
        maxGimbalDegree = ((DJIParamMinMaxCapability) (gimbal.getCapabilities().get(key))).getMax().intValue();

        rotateGimbalToDegree(minGimbalDegree);
    }

    private Gimbal getGimbalInstance() {
        if (gimbal == null) {
            initGimbal();
        }
        return gimbal;
    }

    private void initGimbal() {
        if (DJISDKManager.getInstance() != null) {
            BaseProduct product = DJISDKManager.getInstance().getProduct();
            if (product != null) {
                if (product instanceof Aircraft) {
                    gimbal = ((Aircraft) product).getGimbals().get(currentGimbalId);
                } else {
                    gimbal = product.getGimbal();
                }

                if (isFeatureSupported(CapabilityKey.PITCH_RANGE_EXTENSION)) {
                    gimbal.setPitchRangeExtensionEnabled(true, new CallbackHandlers.CallbackToastHandler());
                }
            }
        }
    }

    private boolean isFeatureSupported(CapabilityKey key) {

        Gimbal gimbal = getGimbalInstance();
        if (gimbal == null) {
            return false;
        }

        DJIParamCapability capability = null;
        if (gimbal.getCapabilities() != null) {
            capability = gimbal.getCapabilities().get(key);
        }

        if (capability != null) {
            return capability.isSupported();
        }
        return false;
    }


    public void rotateGimbalToDegree(float degree) {

        if (gimbal == null) {
            return;
        }
        if (degree == prevDegree) {
            return;
        }

        prevDegree = degree;

        if (degree > maxGimbalDegree) {
            degree = maxGimbalDegree;
        }
        if (degree < minGimbalDegree) {
            degree = minGimbalDegree;
        }



        Rotation.Builder builder = new Rotation.Builder().mode(RotationMode.ABSOLUTE_ANGLE).time(2);
        Rotation.Builder pitchBuilder = builder.pitch(degree);

        float finalDegree = degree;
        gimbal.rotate(pitchBuilder.build(), new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    ToastUtils.setResultToToast("Gimbal rotation error: " + djiError.getDescription());
                } else {
                    ToastUtils.setResultToToast("Gimbal rotated successfully to degree: " + finalDegree);
                }
            }
        });    }

    public void updateGimbel(float gimbalValue) {
        this.gimbalValue = gimbalValue;
    }

}
