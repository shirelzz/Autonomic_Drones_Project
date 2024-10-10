package com.dji.sdk.sample.demo.accurateLandingController;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.ToastUtils;

import dji.common.camera.SettingsDefinitions;

public class RecordVideo {
    private final ImageView recIcon;

    public RecordVideo(ImageView recIcon) {
        onStart();
        this.recIcon = recIcon;
    }

    public void onStart() {

        if (ModuleVerificationUtil.isCameraModuleAvailable()) {
            DJISampleApplication.getProductInstance()
                    .getCamera()
                    .setMode(SettingsDefinitions.CameraMode.RECORD_VIDEO,
                            djiError -> {
                                //success so, start recording
                                if (null == djiError) {
                                    ToastUtils.setResultToToast("SetCameraMode to recordVideo");
                                } else {
                                    ToastUtils.setResultToToast(djiError.getDescription());
                                    Log.i("videoError:", djiError.getDescription());
                                }
                            });
        }
    }

    public void onEnd() {
        if (ModuleVerificationUtil.isCameraModuleAvailable()) {
            DJISampleApplication.getProductInstance()
                    .getCamera()
                    .setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO,
                            djiError -> { //success so, start recording
                                if (null == djiError) {
                                    ToastUtils.setResultToToast("SetCameraMode to shootPhoto");
                                } else {
                                    ToastUtils.setResultToToast(djiError.getDescription());
                                    Log.i("videoError:", djiError.getDescription());
                                }
                            });
        }

    }

    public void startRecording() {
        recIcon.setVisibility(View.VISIBLE);
        ToastUtils.setResultToToast(String.valueOf(ModuleVerificationUtil.isCameraModuleAvailable()));
        if (ModuleVerificationUtil.isCameraModuleAvailable()) {
            DJISampleApplication.getProductInstance()
                    .getCamera()
                    .startRecordVideo(djiError -> {
                        //success so, start recording
                        if (null == djiError) {
                            ToastUtils.setResultToToast("Start record");
                        } else {
                            ToastUtils.setResultToToast(djiError.getDescription());
                            Log.i("videoError:", djiError.getDescription());
                        }
                    });
        }
    }

    public void stopRecording() {
        recIcon.setVisibility(View.INVISIBLE);
        ToastUtils.setResultToToast(String.valueOf(ModuleVerificationUtil.isCameraModuleAvailable()));

        if (ModuleVerificationUtil.isCameraModuleAvailable()) {
            DJISampleApplication.getProductInstance()
                    .getCamera()
                    .stopRecordVideo(djiError -> {
                        //success so, start recording
                        if (null == djiError) {
                            ToastUtils.setResultToToast("stop record");
                        } else {
                            ToastUtils.setResultToToast(djiError.getDescription());
                            Log.i("videoError:", djiError.getDescription());
                        }
                    });
        }
    }
}
