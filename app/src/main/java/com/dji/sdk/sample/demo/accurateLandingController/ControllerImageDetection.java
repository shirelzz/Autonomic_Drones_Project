package com.dji.sdk.sample.demo.accurateLandingController;


import android.graphics.Bitmap;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class ControllerImageDetection {

    //    private final ALRemoteControllerView mainView;
    private long t = System.currentTimeMillis();//used for fps count
    private int frameCounter = 0;
    private int displayFps = 0;

    private DataFromDrone dataFromDrone;

    //constructor
    public ControllerImageDetection(
//            ALRemoteControllerView mainView,
            DataFromDrone dataFromDrone) {
        OpenCVLoader.initDebug();

//        this.mainView = mainView;
        this.dataFromDrone = dataFromDrone;
    }

    public void setBitmapFrame(Bitmap bitmap) {

//        if (t + 1000 < System.currentTimeMillis()) {
//            t = System.currentTimeMillis();
//            Log.i("arrk", "fps " + frameCounter);
//            displayFps = frameCounter;
//            frameCounter = 0;
//        } else {
//            frameCounter++;
//        }

//        double droneHeight = dataFromDrone.getGPS().getAltitude();

//        ControllCommand command =
                proccessImage(bitmap);

    }

    //    public ControllCommand proccessImage(Bitmap frame) {
    public void proccessImage(Bitmap frame) {
        //bug exist here somehow Mat dosent exist here???
        Mat imgToProcess = new Mat();
        Utils.bitmapToMat(frame, imgToProcess);


        imgToProcess = EdgeDetection.detectLines(imgToProcess);

        Utils.matToBitmap(imgToProcess, frame);


//        return null;
    }

}
