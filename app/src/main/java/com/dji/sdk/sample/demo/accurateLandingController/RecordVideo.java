package com.dji.sdk.sample.demo.accurateLandingController;

import android.content.Context;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.ToastUtils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;

/**
 * Created by dji on 16/1/6.
 */
public class RecordVideo{

    public RecordVideo() {
        onStart();
    }

    protected void onStart() {

        if (ModuleVerificationUtil.isCameraModuleAvailable()) {
            DJISampleApplication.getProductInstance()
                    .getCamera()
                    .setMode(SettingsDefinitions.CameraMode.RECORD_VIDEO,
                            djiError -> ToastUtils.setResultToToast("SetCameraMode to recordVideo"));
        }
    }

    public void onEnd() {
        if (ModuleVerificationUtil.isCameraModuleAvailable()) {
            DJISampleApplication.getProductInstance()
                    .getCamera()
                    .setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO,
                            djiError -> ToastUtils.setResultToToast("SetCameraMode to shootPhoto"));
        }
    }

    protected void startRecording() {
        if (ModuleVerificationUtil.isCameraModuleAvailable()) {
            DJISampleApplication.getProductInstance()
                    .getCamera()
                    .startRecordVideo(djiError -> {
                        //success so, start recording
                        if (null == djiError) {
                            ToastUtils.setResultToToast("Start record");
                        }
                    });
        }
    }

    protected void endRecording() {

        if (ModuleVerificationUtil.isCameraModuleAvailable()) {
            DJISampleApplication.getProductInstance()
                    .getCamera()
                    .stopRecordVideo(djiError -> {
                        ToastUtils.setResultToToast("StopRecord");
                    });
        }
    }
}
