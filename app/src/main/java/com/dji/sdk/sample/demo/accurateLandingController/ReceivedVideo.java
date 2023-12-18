package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.controller.DJISampleApplication.getProductInstance;

import dji.common.product.Model;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;

/**
 * Manages the display of video feed on screen received from the DJI product's camera.
 */
public class ReceivedVideo {
    private VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;

    protected DJICodecManager mCodecManager = null;

    public ReceivedVideo() {
        init();
    }

    /**
     * Initializes the video receiver by setting up listeners to receive video data.
     */
    private void init() {
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                setVideoData(videoBuffer, size);
            }
        };
        if (!getProductInstance().getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
        }
    }

    /**
     * Cleans up the previewer by removing the video data listener.
     */
    private void unInitPreviewer() {
        VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(mReceivedVideoDataListener);
    }

    /**
     * Sets video data to the codec manager for decoding and displaying.
     *
     * @param videoBuffer Byte array containing video data.
     * @param size        Size of the video data.
     */
    public void setVideoData(byte[] videoBuffer, int size) {
        if (mCodecManager != null) {
            mCodecManager.sendDataToDecoder(videoBuffer, size);
        }
    }

    /**
     * Getter method to retrieve the codec manager.
     *
     * @return DJICodecManager instance.
     */
    public DJICodecManager getMCodecManager() {
        return mCodecManager;
    }

    /**
     * Setter method to set the codec manager.
     *
     * @param mCodecManager DJICodecManager instance to be set.
     */
    public void setMCodecManager(DJICodecManager mCodecManager) {
        this.mCodecManager = mCodecManager;
    }
}
