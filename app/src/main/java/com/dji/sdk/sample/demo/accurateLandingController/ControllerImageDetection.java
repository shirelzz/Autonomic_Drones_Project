package com.dji.sdk.sample.demo.accurateLandingController;


import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class ControllerImageDetection {

    //    private final ALRemoteControllerView mainView;
    private long t = System.currentTimeMillis();//used for fps count
    private int frameCounter = 0;
    private int displayFps = 0;
    private boolean edgeDetectionMode = false;
    private CenterTracker centerTracker;
    private ObjectTracking objectTracking;

    private DataFromDrone dataFromDrone;

    //constructor
    public ControllerImageDetection(
//            ALRemoteControllerView mainView,
            DataFromDrone dataFromDrone) {
//        OpenCVLoader.initDebug();

//        this.mainView = mainView;
        this.dataFromDrone = dataFromDrone;
//        this.objectTracking = new ObjectTracking(true, "GREEN");
//        centerTracker = new CenterTracker();
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

        double droneHeight = dataFromDrone.getGPS().getAltitude();

//        ControllCommand command =
        proccessImage(bitmap, droneHeight);

    }

    //    public ControllCommand proccessImage(Bitmap frame) {
    public void proccessImage(Bitmap frame, double droneHeight) {
        //bug exist here somehow Mat dosent exist here???
        Mat imgToProcess = new Mat();
        Utils.bitmapToMat(frame, imgToProcess);
//        if (edgeDetectionMode) {
        detectLending(imgToProcess, droneHeight);
//        }
//        double [] delta = centerTracker.process(imgToProcess);
//        Point delta = objectTracking.track(imgToProcess, 100);

        Utils.matToBitmap(imgToProcess, frame);

//        return null;
    }

    public boolean isEdgeDetectionMode() {
        return edgeDetectionMode;
    }

    public void setEdgeDetectionMode(boolean edgeDetectionMode) {
        this.edgeDetectionMode = edgeDetectionMode;
    }

    public void detectLending(Mat imgToProcess, double droneHeight) {
        Point[][] point_arr = EdgeDetection.detectLines(imgToProcess);
        double slop;
        Point[] point;
        // Find the longest line
        double maxLength = 0;
        Point[] longestLine = null;

        for (Point[] points : point_arr) {
            if (points != null && points[0] != null && points[1] != null) {
                double x1 = points[0].x, y1 = points[0].y, x2 = points[1].x, y2 = points[1].y;
//                double length = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
//                if (length > maxLength) {
//                    maxLength = length;
//                    longestLine = points;
//                }
                boolean isHorizontal = Math.abs(y1 - y2) < Math.abs(x1 - x2);


                slop = (points[0].y - points[1].y) / (points[0].x - points[1].x);
                Log.d("slop:  ", String.valueOf(slop));
            }

//            if (slop < 5 && slop > -5) {
//                boolean is_slop = true;
//            }
        }
        if (longestLine != null) {
            double x1 = longestLine[0].x, y1 = longestLine[0].y, x2 = longestLine[1].x, y2 = longestLine[1].y;
            System.out.println("Longest line: (" + x1 + ", " + y1 + ") to (" + x2 + ", " + y2 + ")");
            Imgproc.line(imgToProcess, longestLine[0], longestLine[1], new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);

            // Determine if the longest line is horizontal
            boolean isHorizontal = Math.abs(y1 - y2) < Math.abs(x1 - x2);
            System.out.println("Is the longest line horizontal? " + isHorizontal);
        } else {
            System.out.println("No lines detected.");
        }
    }

}
