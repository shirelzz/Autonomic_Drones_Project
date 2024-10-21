package com.dji.sdk.sample.demo.accurateLandingController;

import android.view.View;
import android.widget.ImageView;

import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.ToastUtils;

import dji.common.camera.SettingsDefinitions;

public class RecordVideo {
    private ImageView recIcon;

    public RecordVideo(ImageView recIcon) {
        onStart();
        this.recIcon = recIcon;
    }

    protected void onStart() {

        if (ModuleVerificationUtil.isCameraModuleAvailable()) {
//            DJISampleApplication.getProductInstance()
//                    .getCamera()
//                    .setMode(SettingsDefinitions.CameraMode.RECORD_VIDEO,
//                            djiError -> {
//                                if (djiError == null)
//                                    ToastUtils.setResultToToast("SetCameraMode to recordVideo");
//                                else ToastUtils.setResultToToast(djiError.getDescription());
//
//                            });
            DJISampleApplication.getProductInstance().getCamera().setFlatMode(SettingsDefinitions.FlatCameraMode.VIDEO_HDR,
                    djiError -> {
                        if (djiError == null)
                            ToastUtils.setResultToToast("SetCameraMode to recordVideo");
                        else ToastUtils.setResultToToast(djiError.getDescription());

                    });
        }
    }

    public void onEnd() {
        if (ModuleVerificationUtil.isCameraModuleAvailable()) {
            DJISampleApplication.getProductInstance()
                    .getCamera()
                    .setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO,
                            djiError -> {
                                if (djiError == null)
                                    ToastUtils.setResultToToast("SetCameraMode to shootPhoto");
                                else ToastUtils.setResultToToast(djiError.getDescription());

                            });
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
                        } else ToastUtils.setResultToToast(djiError.getDescription());
                    });
        }
    }

    protected void stopRecording() {
        recIcon.setVisibility(View.INVISIBLE);

        if (ModuleVerificationUtil.isCameraModuleAvailable()) {
            DJISampleApplication.getProductInstance()
                    .getCamera()
                    .stopRecordVideo(djiError -> {
                        //success so, Stop recording
                        if (null == djiError) {
                            ToastUtils.setResultToToast("Stop record");
                        } else ToastUtils.setResultToToast(djiError.getDescription());
                    });
        }
    }
}
