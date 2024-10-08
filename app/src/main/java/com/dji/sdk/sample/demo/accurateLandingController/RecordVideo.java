package com.dji.sdk.sample.demo.accurateLandingController;
import android.view.View;
import android.widget.ImageView;

import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.ToastUtils;

import dji.common.camera.SettingsDefinitions;

public class RecordVideo{
    private ImageView recIcon;
    public RecordVideo(ImageView recIcon) {
        onStart();
        this.recIcon = recIcon;
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
        recIcon.setVisibility(View.VISIBLE);

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

    protected void stopRecording() {
        recIcon.setVisibility(View.INVISIBLE);

        if (ModuleVerificationUtil.isCameraModuleAvailable()) {
            DJISampleApplication.getProductInstance()
                    .getCamera()
                    .stopRecordVideo(djiError -> ToastUtils.setResultToToast("StopRecord"));
        }
    }
}
