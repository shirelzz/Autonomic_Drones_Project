package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.controller.DJISampleApplication.getProductInstance;
import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

import android.util.Log;

import dji.common.camera.SettingsDefinitions;
import dji.common.product.Model;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;

public class RecordVideo {

    private VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;

    private boolean isCameraRecording = false;

    public RecordVideo() {
        initPreviewer();
    }

    public static synchronized Camera getCameraInstance() {
        if (getProductInstance() == null) return null;
        Camera camera = null;
        if (getProductInstance() instanceof Aircraft) {
            camera = ((Aircraft) getProductInstance()).getCamera();
        } else if (getProductInstance() instanceof HandHeld) {
            camera = ((HandHeld) getProductInstance()).getCamera();
        }
        return camera;
    }

    public void initPreviewer() {
        BaseProduct product = getProductInstance();
        if (product == null || !product.isConnected()) {
            showToast("Disconnected");
            return;
        }

        Camera camera = getCameraInstance();
        if (camera != null) {
            camera.setSystemStateCallback(cameraSystemState -> {
                if (cameraSystemState != null) {
                    if (isCameraRecording != cameraSystemState.isRecording()) {
                        isCameraRecording = cameraSystemState.isRecording();
                    }
                }
            });

            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {
                    @Override
                    public void onReceive(byte[] videoBuffer, int size) {
                        Log.d("DJI", "Received video data of size: " + size);
                    }
                };

                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
            }
        }
    }

    public boolean isCameraRecording() {
        return isCameraRecording;
    }

    public void setCameraRecording(boolean cameraRecording) {
        isCameraRecording = cameraRecording;
    }

    public void toggleRecording() {
        Camera camera = getCameraInstance();
        if (camera == null) {
            Log.e("DJI", "Camera instance is null");
            return;
        }

        if (isCameraRecording) {
            stopRecording();
        } else {
            recordVideo();
        }
    }

    private void recordVideo() {
        setCameraRecording(true);

        Camera camera = getCameraInstance();
        assert camera != null;
        camera.setMode(SettingsDefinitions.CameraMode.RECORD_VIDEO, djiError -> {
            if (djiError == null) {
                camera.startRecordVideo(djiError1 -> {
                    showToast("Recording started successfully");
                    if (djiError1 == null) {
                        Log.i("DJI", "Recording started successfully");
                    } else {
                        Log.e("DJI", "Error starting recording: " + djiError1.getDescription());
                    }
                });
            } else {
                Log.e("DJI", "Error setting camera mode: " + djiError.getDescription());
            }
        });
    }

    private void stopRecording() {
        setCameraRecording(false);
        Camera camera = getCameraInstance();
        assert camera != null;
        camera.stopRecordVideo(djiError -> {
            if (djiError == null) {
                Log.i("DJI", "Recording stopped successfully");
            } else {
                Log.e("DJI", "Error stopping recording: " + djiError.getDescription());
            }
        });
    }

    public void uninitPreviewer() {
        VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(mReceivedVideoDataListener);
    }
}
