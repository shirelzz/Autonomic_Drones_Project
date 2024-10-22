package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;
import static org.opencv.android.Utils.matToBitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.dji.sdk.sample.demo.kcgremotecontroller.VLD_PID;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ControllerImageDetection {

    // depth map python view

    private static final String TAG = ControllerImageDetection.class.getSimpleName();
    private final DataFromDrone dataFromDrone;
    Double PP = 0.05, II = 0.02, DD = 0.01, MAX_I = 0.5;
    private ImageView imageView;  // ImageView to display the frames
    private boolean isObjectDetecting = false;

    private Map<String, Double> controlStatus = new HashMap<>();
    private long prevTime = System.currentTimeMillis();

    private boolean edgeDetectionMode = false;
    private Bitmap current_frame;
    private Mat previous_image = null;
    private Mat current_image = null;
    private double[] previous_image_pos = null;
    private double[] current_image_pos = null;
    // Variables to store the initially selected line and whether a line has been selected
    private Point[] selectedLine = null; // The Line class represents a detected line in the image
    private boolean lineSelected = false;
    private FlightControlMethods flightControlMethods;
    private Point clickedPoint = null;
    private VLD_PID roll_pid = new VLD_PID(PP, II, DD, MAX_I); // side-to-side tilt of the drone
    private VLD_PID pitch_pid = new VLD_PID(PP, II, DD, MAX_I); // forward and backward tilt of the drone
    private VLD_PID yaw_pid = new VLD_PID(PP, II, DD, MAX_I); // left and right rotation
    private VLD_PID throttle_pid = new VLD_PID(PP, II, DD, MAX_I); //vertical up and down motion
    private Context context;
    private GimbalController gimbalController;
    private float r, gp = 0;
    private float p = 0.5f, i = 0.02f, d = 0.01f, max_i = 1, t = -0.6f;//t fot vertical throttle

    private TrackLine trackLine;
    private YoloDetector yoloDetector;
    private int frameCount = 0;
    private Runnable toggleMovementDetection;
    private Button edgeDetect;
    private LandingAlgorithm landingAlgorithm;
    private VLD_PID pitchPID;
    private VLD_PID throttlePID;
    private int imageHeight = 480;  // Width of the camera feed in pixels
    private int bottomTargetRangeMin = imageHeight - 20;  // Target range 20-50 pixels from bottom
    private int bottomTargetRangeMax = imageHeight - 10;

    private boolean step2 = false;
    private boolean step3 = false;
    private Point[] currentTrackedLine;


    //constructor
    public ControllerImageDetection(DataFromDrone dataFromDrone,
                                    FlightControlMethods flightControlMethods,
                                    Context context,
                                    ImageView imageView,
                                    GimbalController gimbalController,
                                    YoloDetector yoloDetector,
                                    Runnable toggleMovementDetection,
                                    Button edgeDetect) {

        this.yoloDetector = yoloDetector;
        this.context = context;
        this.toggleMovementDetection = toggleMovementDetection;
        this.dataFromDrone = dataFromDrone;
        this.flightControlMethods = flightControlMethods;

        this.edgeDetect = edgeDetect;

        this.gimbalController = gimbalController;
        this.imageView = imageView;  // Initialize the ImageView

        this.trackLine = new TrackLine(context);

        initPIDs(p, i, d, max_i, "roll");
        initPIDs(p, i, d, max_i, "pitch");
        initPIDs(p, i, d, max_i, "throttle");

        this.pitchPID = new VLD_PID(p, i, d, max_i);
        this.throttlePID = new VLD_PID(p, i, d, max_i);
        this.landingAlgorithm = new LandingAlgorithm(pitchPID, throttlePID);

    }

    public void setClickedPoint(Point clickedPoint) {
        this.clickedPoint = clickedPoint;
        this.lineSelected = false;
    }

    private void updateLog(@Nullable ControlCommand control, int numEdges, Point[] chosenEdge, double dy, double steps) {

        if (numEdges > 0) {
            controlStatus.put("edgeX", (chosenEdge[0].x + chosenEdge[1].x) / 2.0);
            controlStatus.put("edgeY", (chosenEdge[0].y + chosenEdge[1].y) / 2.0);
            controlStatus.put("edgeDist", dy);
        }
        if (control != null) {
            controlStatus.put("PitchOutput", (double) control.getPitch());
            controlStatus.put("RollOutput", (double) control.getRoll());
            controlStatus.put("ThrottleOutput", (double) control.getVerticalThrottle());
        }

        controlStatus.put("LandingSteps", steps);

        boolean autonomous_mode = Objects.requireNonNull(DJISampleApplication.getAircraftInstance()).getFlightController().isVirtualStickControlModeAvailable();
        controlStatus.put("AutonomousMode", (double) (autonomous_mode ? 1 : 0));

    }

    public Map<String, Double> getControlStatus() {
        return controlStatus;
    }

    // Method to run the Python script asynchronously

    public void initPIDs(double p, double i, double d, double max_i, String type) {

        if (type.equals("roll")) {
            if (roll_pid == null) {
                roll_pid = new VLD_PID(p, i, d, max_i);
            } else {
                roll_pid.setPID(p, i, d, max_i);
            }
        }

        if (type.equals("pitch")) {
            if (pitch_pid == null) {
                pitch_pid = new VLD_PID(p, i, d, max_i);
            } else {
                pitch_pid.setPID(p, i, d, max_i);
            }
        }

        if (type.equals("throttle")) {
            if (throttle_pid == null) {
                throttle_pid = new VLD_PID(p, i, d, max_i);
            } else {
                throttle_pid.setPID(p, i, d, max_i);
            }
        }
    }

    public void setBitmapFrame(Bitmap bitmap) {
        try {
            double droneHeight = dataFromDrone.getGPS().getAltitude();
            processImage(bitmap, droneHeight);
        } catch (Exception exception) {
            Log.e("Error: ", Objects.requireNonNull(exception.getMessage()));
            showToast(Objects.requireNonNull(exception.getMessage()));
        }
    }

    public void stopEdgeDetection() {
        showToast("Edge Detection Stopped");
        ControlCommand stay = stayOnPlace();
        flightControlMethods.sendVirtualStickCommands(stay, 0.0f);
        edgeDetect.setBackgroundColor(Color.WHITE);
        selectedLine = null; // The Line class represents a detected line in the image
        lineSelected = false;
        clickedPoint = null;
        setEdgeDetectionMode(false);
    }

    public void rotateGimbalToMinus45() {
        gimbalController.rotateGimbalToDegree(-45);
    }

    // Method to find the closest line to the point where the user clicked
    private void findClosestLine(Mat imgToProcess) {
        Log.i("EdgeDetect", "Hey");
        // After detecting all lines, find the line closest to the clicked point (clickedX, clickedY)
        double droneRelativeHeight = dataFromDrone.getAltitudeBelow();
//        boolean isUltrasonicBeingUsed = dataFromDrone.isUltrasonicBeingUsed();
        List<Object[]> pointArr = EdgeDetection.detectLines(imgToProcess, droneRelativeHeight);
        Point[] closestLine = selectBestLinePoint(pointArr);
        trackLine.detectLineFeatures(closestLine, imgToProcess);
//        trackLine.initializeTracking(imgToProcess, selectedLine);


        // Store the closest line as the selected line and mark that a line has been selected
        if (closestLine != null) {
            selectedLine = closestLine;
            lineSelected = true;
        }

    }

    private void initCurrentTrackedLine() {
        double droneRelativeHeight = dataFromDrone.getAltitudeBelow();
        List<Object[]> pointArr = EdgeDetection.detectLines(current_image, droneRelativeHeight);
        Point[] closestLine = selectBestLinePoint(pointArr);

        currentTrackedLine = closestLine;
        lineSelected = true;
    }

    // Do not delete
    public void setCurrentImage(Bitmap frame, double[] pos) {
        Mat newCurrentImg = new Mat();
        Utils.bitmapToMat(frame, newCurrentImg);

        if (previous_image == null) {
            previous_image = newCurrentImg;
            previous_image_pos = pos;

        } else {
            previous_image = current_image;
            previous_image_pos = current_image_pos;
        }

        // Set the new current image
        current_frame = frame;
        current_image = newCurrentImg;
        current_image_pos = pos;
    }

    public ControlCommand executeLandingSequence(Mat imgToProcess) {
        long currTime = System.currentTimeMillis();
        double dt = (currTime - prevTime) / 1000.0;
        prevTime = currTime;

        double altitude = dataFromDrone.getAltitudeBelow();

        // Step 1: Detect the line and get the center of the line segment
        List<Object[]> detectedLines = EdgeDetection.detectLines(imgToProcess, altitude);
        Point[] detectedLine = trackLine.updateLineUsingDetectedLines(detectedLines, currentTrackedLine);
        Log.d(TAG, Arrays.toString(detectedLine));

        // Check if the line was updated
        boolean lineUpdated = (detectedLine != null);
        if (lineUpdated) {
            currentTrackedLine = detectedLine;  // Update to the new detected line or keep current
        }

        //        showToast(Arrays.toString(detectedLine));
        Log.d(TAG, Arrays.toString(detectedLine));

        if (detectedLine != null) {

            if (!step2 && !step3 && (detectedLine[0].y > 0 && detectedLine[0].y < 480 || detectedLine[1].y > 0 && detectedLine[1].y < 480)) {
                // Line is detected, proceed to position it at the bottom range
                Imgproc.line(imgToProcess, detectedLine[0], detectedLine[1], new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);

                // Calculate the y position of the line center
                double lineCenterY = (detectedLine[0].y + detectedLine[1].y) / 2.0;

                Imgproc.putText(imgToProcess, "Y: " + lineCenterY, new Point(imgToProcess.cols() / 2.0, imgToProcess.rows() / 2.0 - 20),
                        Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 0), 2);
                //

                Imgproc.putText(imgToProcess, "YS: " + detectedLine[0].y, new Point(imgToProcess.cols() / 2.0, imgToProcess.rows() / 2.0 - 40),
                        Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 0), 2);

                Imgproc.putText(imgToProcess, "YE: " + detectedLine[1].y, new Point(imgToProcess.cols() / 2.0, imgToProcess.rows() / 2.0 - 60),
                        Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 0), 2);

                // Move forward until the line is in target range (10-20 pixels from the bottom)
                if (lineCenterY < bottomTargetRangeMin || lineCenterY > bottomTargetRangeMax) {
//                450, 470
//                showToast("Aligning line to bottom range.");

                    ControlCommand moveForwardCommand = landingAlgorithm.moveForward(dt, imgToProcess);

                    updateLog(moveForwardCommand, 1, detectedLine, lineCenterY, 2);

                    return moveForwardCommand;
                }

                // If the line is in the target position, adjust gimbal to prepare for blind spot movement
                showToast("Line positioned correctly. Preparing for blind spot movement.");
                gimbalController.rotateGimbalToDegree(-90);
                step2 = true;

            }
        }

//        else if (!step2 && !step3) {
////            updateLog(null, 0, null, 0, 0);
////            Point[] prevLine = trackLine.getPrevLine();
////            Point middlePoint = new Point((prevLine[0].x + prevLine[1].x) / 2.0, (prevLine[0].y + prevLine[1].y) / 2.0);
////            edgeNotFoundWithClickedPoint(middlePoint);
//            gimbalController.rotateGimbalToDegree(-90);
//
//            step2 = true;
//        }

        if (step2 && !step3) {
            // If we lose the line (likely due to close proximity), proceed with blind spot movement
            showToast("Step 2: Moving by blind spot distance.");

            double blindSpotDistance = calculateBlindSpotDistance(dataFromDrone.getAltitudeBelow());
            showToast("blindSpotDistance: " + blindSpotDistance);
            ControlCommand blindSpotMoveCommand = landingAlgorithm.moveForwardByDistance(blindSpotDistance, dt, imgToProcess);

            step2 = false;
            step3 = true;
//            updateLog(blindSpotMoveCommand, 1, detectedLine, blindSpotDistance, 2);

            return blindSpotMoveCommand;
        }

        if (step3) {
            // Step 3: After moving forward by the blind spot distance, check if the line is detected
            showToast("Step 3: Land or Hover.");

//            detectedLine = trackLine.trackSelectedLineUsingOpticalFlow(imgToProcess);
//            if (detectedLine != null) {
                showToast("Line detected. Landing now.");
                if (isHazardous(dataFromDrone.getAltitudeBelow())){
                    showToast("Landing aborted due to hazards.");
                    ControlCommand stayOnPlaceCommand = stayOnPlace();
                    updateLog(stayOnPlaceCommand, 0, detectedLine, 0, 3);
                    return stayOnPlaceCommand;

                } else {
                    showToast("No hazards. Landing.");

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    updateLog(null, 0, detectedLine, 0, 3);
                    flightControlMethods.land(this::stopEdgeDetection, this::rotateGimbalToMinus45);
                }

            step3 = false;
        }

        return null;  // No further movement command if line positioning is still in process
    }

    private double calculateBlindSpotDistance(double altitude) {
        // Based on your measurements: for every 0.7 meters altitude, there is a 0.3 meters blind spot.
        double blindSpotRatio = 0.3 / 0.7;
        return altitude * blindSpotRatio;
    }

    public boolean isHazardous(double altitude) {
        Log.d(TAG, "entered checkForHazards");

        DroneSafety droneSafety = new DroneSafety(yoloDetector);
        return droneSafety.checkForHazardsInRegion(current_image, altitude);
    }

    public void processImage(Bitmap frame, double droneHeight) {
        // Added python function but it causes an error in loading
        Mat imgToProcess = new Mat();
        Utils.bitmapToMat(frame, imgToProcess);
        try {
            if (clickedPoint == null) {
                List<Object[]> PointList = EdgeDetection.detectLines(imgToProcess, droneHeight);
                matToBitmap(imgToProcess, frame);
                return;
            }

            if (!lineSelected) {
                findClosestLine(imgToProcess);
                initCurrentTrackedLine();
                frameCount = 0;
            }

            ControlCommand command = executeLandingSequence(imgToProcess);

            if (command != null) {
                flightControlMethods.sendVirtualStickCommands(command, 0.0f);
            }
        } catch (Exception e) {
            Log.e("Error: ", Objects.requireNonNull(e.getMessage()));
            showToast(Objects.requireNonNull(e.getMessage()));
            double droneRelativeHeight = dataFromDrone.getAltitudeBelow();
            if (droneRelativeHeight <= 0.2f) {
                showToast("LandError!!!!");
                throttle_pid.reset();
                pitch_pid.reset();
                flightControlMethods.land(toggleMovementDetection, this::stopEdgeDetection);

            } else {
                stopEdgeDetection();
            }
            matToBitmap(imgToProcess, frame);
            throw new RuntimeException(e);
        }

        matToBitmap(imgToProcess, frame);
    }

    public boolean isEdgeDetectionMode() {
        return edgeDetectionMode;
    }

    public void setEdgeDetectionMode(boolean edgeDetectionMode) {
        this.edgeDetectionMode = edgeDetectionMode;
    }

    private ControlCommand stayOnPlace() {
        // Reset all values or drone movement, keeping the drone in place
        roll_pid.reset();
        pitch_pid.reset();
        t = 0;
        r = 0;
        p = 0;
//        y = 0;
//        need to set another gp
//        set first gimbal angle
//        gp = (float) gimbelValue;
        ControlCommand ans = new ControlCommand(p, r, t);
        ans.setErr(1000, 0, 0, 0);
        ans.setPID(throttle_pid.getP(), throttle_pid.getI(), throttle_pid.getD(), pitch_pid.getP(), pitch_pid.getI(), pitch_pid.getD(), roll_pid.getP(), roll_pid.getI(), roll_pid.getD(), roll_pid.getMax_i());
        ans.setImageDistance(-1);

        return ans;
    }

    private Point[] selectBestLinePoint(List<Object[]> pointArr) {
        Point[] bestLine = null;
        double bestDistance = Double.MAX_VALUE;

        Log.i("EdgeDetection", "size3: " + pointArr.size());
        for (Object[] entry : pointArr) {
            Point[] point = (Point[]) entry[0];

            if (point != null && point[0] != null && point[1] != null) {

                // Calculate the midpoint of the line
                double midX = (point[0].x + point[1].x) / 2.0;
                double midY = (point[0].y + point[1].y) / 2.0;

                // Calculate Euclidean distance from the clicked point to the line's midpoint
                double distance = Math.sqrt(Math.pow(clickedPoint.x - midX, 2) + Math.pow(clickedPoint.y - midY, 2));

                // Check if this line is closer to the clicked point than the current best
                if (distance < bestDistance) {
                    bestLine = point;
                    bestDistance = distance;
                }
            }
        }

        return bestLine;
    }

    public boolean isObjectDetecting() {
        return isObjectDetecting;
    }

    public void stopObjectDetectionAlgo() {
        isObjectDetecting = false;
    }

    public void startObjectDetectionAlgo() {
        Log.d(TAG, "entered startObjectDetectionAlgo");

        isObjectDetecting = true;

        DroneSafety droneSafety = new DroneSafety(yoloDetector);

        try {

            // Check for hazards before attempting to land
            boolean isHazardous = droneSafety.checkForHazards(current_image);

            if (isHazardous) {
                System.out.println("Landing aborted due to hazards.");
                showToast("Landing aborted due to hazards.");
                stayOnPlace();
                edgeDetect.setBackgroundColor(Color.WHITE);
            } else {
                System.out.println("No hazards detected. Safe to land.");
                showToast("No hazards detected. Safe to land.");
                setEdgeDetectionMode(true);
                stopObjectDetectionAlgo();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        isObjectDetecting = false;

        Log.d(TAG, "ended startObjectDetectionAlgo");

    }

}