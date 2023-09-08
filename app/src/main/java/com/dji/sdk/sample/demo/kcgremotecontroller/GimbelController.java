package com.dji.sdk.sample.demo.kcgremotecontroller;

import androidx.annotation.NonNull;

import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;

import java.util.LinkedList;
import java.util.List;

import dji.common.gimbal.GimbalState;
import dji.sdk.gimbal.Gimbal;


public class GimbelController
{

    private Controller controller;
    private Gimbal gimbal = null;
    private List<gimbelListener> gimbelListenersList = new LinkedList<gimbelListener>();

    public GimbelController()
    {
        if (ModuleVerificationUtil.isGimbalModuleAvailable())
        {
            DJISampleApplication.getProductInstance().getGimbal().setStateCallback(new GimbalState.Callback() {
                @Override
                public void onUpdate(@NonNull GimbalState gimbalState)
                {
                    //controller.setGimbelValue(gimbalState.getAttitudeInDegrees().getPitch());
                    for (gimbelListener listener: gimbelListenersList) {
                        listener.updateGimbel(gimbalState.getAttitudeInDegrees().getPitch());
                    }
                }
            });
        }

    }

    public void addListener(gimbelListener listener)
    {
        this.gimbelListenersList.add(listener);
    }
}
