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
        Point[] detectLandLine = null;

        for (Point[] points : point_arr) {
            if (points != null && points[0] != null && points[1] != null) {
                double x1 = points[0].x, y1 = points[0].y, x2 = points[1].x, y2 = points[1].y;
//                double length = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
//                if (length > maxLength) {
//                    maxLength = length;
//                    detectLandLine = points;
//                }

                //TODO: need to be changed, how do i choose the line that i want to land according to?
                boolean isHorizontal = Math.abs(y1 - y2) < Math.abs(x1 - x2);
                slop = (points[0].y - points[1].y) / (points[0].x - points[1].x);
                if (slop < 2 && slop > -2) {
                    isHorizontal = true;
                }
                if (isHorizontal) {
                    detectLandLine = points;
                }
                Point centerPoint = new Point(imgToProcess.cols() / 2.0, imgToProcess.rows() / 2.0);
                double Calc_dis = distancePointToLine(centerPoint, points);
                // Do we need the drone to know every time what the altitude according to the gps?
                Imgproc.putText(imgToProcess, "dy: " + Calc_dis, centerPoint, 5, 0.3, new Scalar(0, 255, 0));
                // TODO: We send a command to the drone how he need to go so it will be exactly
                //  above the line and horizontal to the line.
                // TODO: Move the Drone to be 90 degrees from the line, so it will still be in the
                //  middle of the line, but the line is vertical
                // TODO: We will move the drone to the left and to the right of the line, and each
                //  time we will move or until the alt is different or until the line is not in the frame.
                // TODO: we will turn the drone until he will on top the smaller altitude position.
                // TODO: check radius 20CM from the line to the back and left and right if the
                //  radius exist and all the altitude is the same.
                // TODO: start lowering down and always check that the line is in sight if not, search for it.
                Log.i("Is?", "Is the line horizontal? " + droneHeight + " Calc_dis:  " + Calc_dis);
            }

        }
        if (detectLandLine != null) {
            double x1 = detectLandLine[0].x, y1 = detectLandLine[0].y, x2 = detectLandLine[1].x, y2 = detectLandLine[1].y;
            Log.i("Detect", "Longest line: (" + x1 + ", " + y1 + ") to (" + x2 + ", " + y2 + ")");
            Imgproc.line(imgToProcess, detectLandLine[0], detectLandLine[1], new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);


        }
//        else {
////            Log.i("No lines detected.");
//        }
    }

    private double distancePointToLine(final Point point, final Point[] line) {
        final Point l1 = line[0];
        final Point l2 = line[1];
        return Math.abs((l2.x - l1.x) * (l1.y - point.y) - (l1.x - point.x) * (l2.y - l1.y))
                / Math.sqrt(Math.pow(l2.x - l1.x, 2) + Math.pow(l2.y - l1.y, 2));
    }

}
