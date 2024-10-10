package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;
import static org.opencv.android.Utils.matToBitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.dji.sdk.sample.demo.kcgremotecontroller.VLD_PID;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ControllerImageDetection {

    // depth map python view

    private static final String TAG = ControllerImageDetection.class.getSimpleName();
    private final int displayFps = 0;
    private final DataFromDrone dataFromDrone;
    //    Double PP = 0.5, II = 0.02, DD = 0.01, MAX_I = 0.5;
    Double PP = 0.05, II = 0.02, DD = 0.01, MAX_I = 0.5;
    private ImageView imageView;  // ImageView to display the frames
    private boolean isPlaying = false;  // To control the video playback
    private boolean isDetectingPlane = false;
    private int landingMode = 0;
    private boolean isObjectDetecting = false;
    private Handler handler = new Handler(Looper.getMainLooper());  // For updating the UI
    private Python python;
    private double focal_length = 4.8; // Field of View in mm
    private double sensor_width = 6.4;  // Sensor width in millimeters (approximate for 1/2" CMOS sensor)
    private ArrayList<Bitmap> imageList = new ArrayList<>();
    private PyObject PlaneHandlerClass;
    private PyObject getOutputFunc;
    private int frameCounter = 0;
    private Map<String, Double> controlStatus = new HashMap<>();

    //    private final ALRemoteControllerView mainView;
    private long prevTime = System.currentTimeMillis();
    private boolean first_detect = true;
    private int not_found = 0;

    //    private double aspectRatio = 0;
//    private double VerticalFOV = 0;
    private boolean edgeDetectionMode = false;
    private CenterTracker centerTracker;
    private Bitmap current_frame;
    private Mat previous_image = null;
    private Mat current_image = null;
    private double[] previous_image_pos = null;
    private double[] current_image_pos = null;
    // Variables to store the initially selected line and whether a line has been selected
    private Point[] selectedLine = null; // The Line class represents a detected line in the image
    private boolean lineSelected = false;
    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private FlightControlMethods flightControlMethods;
    private float descentRate = 0;

    private Point clickedPoint = null;
    private VLD_PID roll_pid = new VLD_PID(PP, II, DD, MAX_I); // side-to-side tilt of the drone
    private VLD_PID pitch_pid = new VLD_PID(PP, II, DD, MAX_I); // forward and backward tilt of the drone
    private VLD_PID yaw_pid = new VLD_PID(PP, II, DD, MAX_I); // left and right rotation
    private VLD_PID throttle_pid = new VLD_PID(PP, II, DD, MAX_I); //vertical up and down motion
    private Context context;
    private int frameHeight, frameWidth;
    private GimbalController gimbalController;
    private float lastP = 0, lastR = 0; // previous error
    private float r, gp = 0;
    private float p = 0.5f, i = 0.02f, d = 0.01f, max_i = 1, t = -0.6f;//t fot vertical throttle
    private double error_x, error_y;
    private double pitch = 0.0, roll = 0.0, alt = 0.0, dt = 0.0;
    private boolean activate = false;
    private TrackLine trackLine;
    private YoloDetector yoloDetector;
    private int frameCount = 0;
    private Runnable toggleMovementDetection;
    private Button edgeDetect;
    private double initialDistance = 0;  // Store the original distance when the movement starts
    private double velocity = 0.02;      // Start with an initial velocity of 0.01
    private double accelerationRate = 0.01;  // Fixed rate of velocity increase/decrease
    private LandingAlgorithm landingAlgorithm;
    private VLD_PID pitchPID;
    private VLD_PID throttlePID;
    private double fieldOfView = 84.0;  // Camera's horizontal field of view in degrees
    private int imageWidth = 640;  // Width of the camera feed in pixels
    private int imageHeight = 480;  // Width of the camera feed in pixels

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
//        this.mainView = mainView;
        this.dataFromDrone = dataFromDrone;
        this.flightControlMethods = flightControlMethods;

        this.edgeDetect = edgeDetect;

//        this.recordingVideo = recordingVideo;
        this.gimbalController = gimbalController;
        this.imageView = imageView;  // Initialize the ImageView

        //Do we need it
        this.frameWidth = 640;
        this.frameHeight = 480;
        this.trackLine = new TrackLine();

        initPIDs(p, i, d, max_i, "roll");
        initPIDs(p, i, d, max_i, "pitch");
        initPIDs(p, i, d, max_i, "throttle");
        setDescentRate(t);

        this.pitchPID = new VLD_PID(p, i, d, max_i);
        this.throttlePID = new VLD_PID(p, i, d, max_i);
        this.landingAlgorithm = new LandingAlgorithm(pitchPID, throttlePID);

    }

    public void setClickedPoint(Point clickedPoint) {
        this.clickedPoint = clickedPoint;
        this.lineSelected = false;
    }

    private void updateLog(ControlCommand control, int numEdges, Point[] chosenEdge, double dy) {

//        saw_target, edgeX,edgeY,edgeDist,PitchOutput,RollOutput,ErrorX,ErrorY,P,I,D,MaxI

//        controlStatus.put("saw_target",(double)(imageCoordinates.saw_target()? 1 : 0));
//        controlStatus.put("saw_target", (double) numEdges);
        if (numEdges > 0) {
            controlStatus.put("edgeX", (chosenEdge[0].x + chosenEdge[1].x) / 2.0);
            controlStatus.put("edgeY", (chosenEdge[0].y + chosenEdge[1].y) / 2.0);
            controlStatus.put("edgeDist", dy);
        }

        controlStatus.put("PitchOutput", (double) control.getPitch());
        controlStatus.put("RollOutput", (double) control.getRoll());
        controlStatus.put("ThrottleOutput", (double) control.getVerticalThrottle());

//        controlStatus.put("ErrorX", control.xError);
//        controlStatus.put("ErrorY", control.yError);

//        controlStatus.put("Pp", control.p_pitch);
//        controlStatus.put("Ip", control.i_pitch);
//        controlStatus.put("Dp", control.d_pitch);
//
//        controlStatus.put("Pr", control.p_roll);
//        controlStatus.put("Ir", control.i_roll);
//        controlStatus.put("Dr", control.d_roll);
//
//        controlStatus.put("Pt", control.p_Throttle);
//        controlStatus.put("It", control.i_Throttle);
//        controlStatus.put("Dt", control.d_Throttle);

//        controlStatus.put("maxI", control.maxI);


        boolean autonomous_mode = Objects.requireNonNull(DJISampleApplication.getAircraftInstance()).getFlightController().isVirtualStickControlModeAvailable();
        controlStatus.put("AutonomousMode", (double) (autonomous_mode ? 1 : 0));

    }

    public Map<String, Double> getControlStatus() {
        return controlStatus;
    }

    // Method to run the Python script asynchronously
    public void setDescentRate(float descentRate) {
        if (descentRate > 0) {
            descentRate = -descentRate;
        }

        this.descentRate = descentRate;
    }

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
//        ControlCommand stay = stayOnPlace();
//        flightControlMethods.sendVirtualStickCommands(stay, 0.0f);
        edgeDetect.setBackgroundColor(Color.WHITE);
        selectedLine = null; // The Line class represents a detected line in the image
        lineSelected = false;
        clickedPoint = null;
        setEdgeDetectionMode(false);
        first_detect = true;
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

        // Store the closest line as the selected line and mark that a line has been selected
        if (closestLine != null) {
            selectedLine = closestLine;
            lineSelected = true;

            // Store the original line properties
            trackLine.storeOriginalLineProperties(selectedLine);
        }

    }

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

//    public ControlCommand moveDroneToLine(Mat imgToProcess) {
//        showToast("bestHorizontalLine");
//        long currTime = System.currentTimeMillis();
//        double dt = (currTime - prevTime) / 1000.0; // Calculate time difference
//        prevTime = currTime;
//
//        if (selectedLine == null) {
//            return null;
//        }
//
//        Point[] currentLine = trackLine.trackSelectedLineUsingOpticalFlow(imgToProcess);
//
//        double droneRelativeHeight = dataFromDrone.getAltitudeBelow();
//        boolean isUltrasonicBeingUsed = dataFromDrone.isUltrasonicBeingUsed();
//        if (droneRelativeHeight <= 0f) {
//            showToast("Land2!!!!");
//            throttle_pid.reset();
//            pitch_pid.reset();
//            flightControlMethods.land(toggleMovementDetection, this::stopEdgeDetection);
//            return null;
//        }
//        if (currentLine == null) {
//            Log.i("EdgeDetect", isUltrasonicBeingUsed + ": rh: " + droneRelativeHeight + ", h:" + dataFromDrone.getGPS().getAltitude());
//            if (droneRelativeHeight <= 0.2f
//                //|| dataFromDrone.getGPS().getAltitude() <= 0.2f
//            ) {
//                showToast("Land2!!!!");
//                throttle_pid.reset();
//                pitch_pid.reset();
//                flightControlMethods.land(toggleMovementDetection, this::stopEdgeDetection);
//                return null;
//            } else {
//                return stayOnPlace();
//            }
//            //TODO: Maybe add a scenario when the line is disappearing from the image
////            return null;
//        }
//
//        selectedLine = currentLine;
//        Imgproc.line(imgToProcess, selectedLine[0], selectedLine[1], new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);
////        showToast("droneRelativeHeight: " + droneRelativeHeight);
//        double dyRealPitch = adjustDronePosition(selectedLine, imgToProcess.height(), droneRelativeHeight, 0);
////        ControlCommand command = buildControlCommandEdge(dyRealPitch, dt, imgToProcess);
//        ControlCommand command = buildControlCommandEdgeFixedVelocity(dyRealPitch, dt, imgToProcess);
//
//        updateLog(command, 1, selectedLine, dyRealPitch);
//        return command;
//    }

    public ControlCommand moveDroneToLine_sh(Mat imgToProcess) {
        long currTime = System.currentTimeMillis();
        double dt = (currTime - prevTime) / 1000.0;
        prevTime = currTime;

        if (selectedLine == null) {
            return null;
        }

        Point[] currentLine = trackLine.trackSelectedLineUsingOpticalFlow(imgToProcess);

        double droneRelativeHeight = dataFromDrone.getAltitudeBelow();
        if (droneRelativeHeight <= 0.03) {
            showToast("Landing now!");
            flightControlMethods.land(toggleMovementDetection, this::stopEdgeDetection);
            return null;
        }

        double dyReal = calculateDistanceToLine(currentLine);
        ControlCommand command = landingAlgorithm.approachLine(dyReal, dt, imgToProcess);

        return command == null ? stayOnPlace() : command;
    }

    private double calculateDistanceToLine(Point[] line) {
        Point start = line[0];
        Point end = line[1];
        Point dronePosition = new Point(imageWidth / 2, imageHeight);

        double numerator = Math.abs((end.y - start.y) * dronePosition.x - (end.x - start.x) * dronePosition.y + end.x * start.y - end.y * start.x);
        double denominator = Math.sqrt(Math.pow(end.y - start.y, 2) + Math.pow(end.x - start.x, 2));
        double imageDistance = denominator != 0 ? numerator / denominator : 0;

        return convertImageDistanceToRealDistance(imageDistance);
    }

    private double convertImageDistanceToRealDistance(double imageDistance) {
        double altitude = dataFromDrone.getAltitudeBelow();
        double metersPerPixel = (2 * altitude * Math.tan(Math.toRadians(fieldOfView) / 2)) / imageWidth;
        return imageDistance * metersPerPixel;
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
                frameCount = 0;
            }
            ControlCommand command = moveDroneToLine_sh(imgToProcess);

//            command = detectLending(imgToProcess, droneHeight);

            if (command != null) {
                flightControlMethods.sendVirtualStickCommands(command, 0.0f);
            }
        } catch (Exception e) {
            Log.e("Error: ", Objects.requireNonNull(e.getMessage()));
            showToast(Objects.requireNonNull(e.getMessage()));
            double droneRelativeHeight = dataFromDrone.getAltitudeBelow();
            if (droneRelativeHeight <= 0.2f
                //|| dataFromDrone.getGPS().getAltitude() <= 0.2f
            ) {
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

    // Function to calculate the Euclidean distance (baseline)
    private double calculateBaseline(double[] pos1, double[] pos2) {

        if (pos1 == null || pos2 == null) {
            Log.d(TAG, "pos1 == null || pos2 == null");
            return -1;
        }

        double[] vector = Cords.flatWorldDist(pos1, pos2);
        if (vector != null) {
            double baseline = Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]);
            Log.d(TAG, "The baseline between the two frames is: " + baseline + " meters");
            return baseline;
        } else {
            Log.d(TAG, "Failed to calculate the distance between the two positions.");
            return -2;
        }
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

    private double adjustDronePosition(Point[] finalLine, double frameHeightPx, double droneHeight, double distanceFromCenter) {
//        distanceFromCenter = distanceFromCenter * (landingMode == 1 ? 1 : -1);
        double dy_best = frameHeightPx / 2.0 - (finalLine[0].y + finalLine[1].y) / 2.0;
        double GSD = (sensor_width * droneHeight * 100) / (focal_length * frameHeightPx);
        return GSD * dy_best;

    }

    private ControlCommand buildControlCommandEdgeFixedVelocity(double dyReal, double dt, Mat imgToProcess) {
        double maxSpeed = 1.0;   // Cap the maximum velocity
        double minSpeed = 0.02;  // Minimum starting velocity

        // Set initial distance only at the start of the movement
        if (initialDistance == 0) {
            initialDistance = Math.abs(dyReal);  // Absolute distance to target
        }

        double currentDistance = Math.abs(dyReal);  // Current remaining distance

        // Calculate the midpoint where deceleration should begin
        double midpoint = initialDistance / 3.0;

        // Determine if we are before or after the midpoint
        if (currentDistance > midpoint) {
            // Increase velocity while the drone is before the midpoint
            velocity = Math.min(velocity + accelerationRate, maxSpeed);
        } else {

            // Decrease velocity after passing the midpoint
//            velocity = Math.max(velocity - accelerationRate, minSpeed);
            velocity = 0;
        }
        showToast("velocity: " + velocity);

        // Cap the velocity to make sure it doesn't exceed maxSpeed or drop below minSpeed
        velocity = Math.max(minSpeed, Math.min(velocity, maxSpeed));
//        velocity = 0.01;
//        error_y = dyReal * 0.01;  // Proportional gain can be adjusted if needed

        // Check if we're close enough to stop the drone
        if (currentDistance <= 1.5) {
            showToast("Stopping, distance reached!");
            velocity = 0;  // Stop the drone
            initialDistance = 0;  // Reset for the next movement
            t = -0.08f;
            error_y = 0.08;
        } else {
            t = 0;
        }
//        if (0 <= dyReal && dyReal <= 0.1) {
//            showToast("Landing!!");
//            t = -0.05f;
//            changedDy = 0.05;
//
//        } else {
//            changedDy = error_y;
//            t = 0;
//        }

        // Use PID controller to move the drone with the calculated velocity
        p = (float) pitch_pid.update(velocity, dt, maxSpeed);
        t = (float) throttle_pid.update(t, dt, maxSpeed);  // Adjust as needed for throttle

        // Debug information for testing
        Imgproc.putText(imgToProcess, "dy: " + dyReal, new Point(imgToProcess.cols() / 2.0, imgToProcess.rows() / 2.0), 5, 1, new Scalar(0, 255, 0));
        showToast("p: " + p + ", velocity: " + velocity + ", t: " + t);

        // Build control command with the updated values
        ControlCommand ans = new ControlCommand(p, r, t);
        ans.setErr(1000, error_x, error_y, dataFromDrone.getAltitudeBelow());
        ans.setPID(throttle_pid.getP(), throttle_pid.getI(), throttle_pid.getD(), pitch_pid.getP(), pitch_pid.getI(), pitch_pid.getD(), roll_pid.getP(), roll_pid.getI(), roll_pid.getD(), roll_pid.getMax_i());

        return ans;
    }

    private ControlCommand buildControlCommandEdge(double dyReal, double dt, Mat imgToProcess) {
        double maxSpeed = 2.0;
        double Kp = 0.01;  // Proportional gain, adjust as needed - 0.02 / 0.01
        error_y = (dyReal / 2.0) * Kp;
        double changedDy;

        if (0 <= dyReal && dyReal < 1) {
            maxSpeed = 1.0;
            showToast("Landing!!");
            t = -0.1f;
            changedDy = 0.1;
            pitch_pid.reset();
            throttle_pid.reset();

        } else {
            changedDy = error_y;
            t = 0;
        }
        if (frameCount % 2 != 0) {
            frameCount++;
            // Use PID controller to move the drone with the calculated velocity
            p = (float) pitch_pid.update(0, dt, maxSpeed);
            t = (float) throttle_pid.update(0, dt, maxSpeed);  // Adjust as needed for throttle
        } else {
            frameCount++;
            // Use PID controller to move the drone with the calculated velocity
            p = (float) pitch_pid.update(changedDy, dt, maxSpeed);
            r = (float) roll_pid.update(0, dt, maxSpeed);
            t = (float) throttle_pid.update(t, dt, maxSpeed);
        }

        Imgproc.putText(imgToProcess, "dy: " + dyReal, new Point(imgToProcess.cols() / 2.0, imgToProcess.rows() / 2.0), 5, 1, new Scalar(0, 255, 0));
        showToast("p: " + p + ", t: " + t);

        ControlCommand ans = new ControlCommand(p, r, t);
        ans.setErr(1000, error_x, error_y, dataFromDrone.getAltitudeBelow());
        ans.setPID(throttle_pid.getP(), throttle_pid.getI(), throttle_pid.getD(), pitch_pid.getP(), pitch_pid.getI(), pitch_pid.getD(), roll_pid.getP(), roll_pid.getI(), roll_pid.getD(), roll_pid.getMax_i());

        return ans;
    }

    // TODO : Use this function after the drone has landed
    public void stopPlaneDetectionAlgo() {
        isDetectingPlane = false;
//        releasePythonResources();
    }

    public boolean isObjectDetecting() {
        return isObjectDetecting;
    }

    public void stopObjectDetectionAlgo() {
        isObjectDetecting = false;
//        releasePythonResources();
    }

    // Convert Mat to byte array
    public byte[] matToBytes(Mat mat) {
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".png", mat, matOfByte);
        return matOfByte.toArray();
    }

    private Bitmap convertBase64ToBitmap(String imageBytesBase64) {
        // Decode Base64 to byte array
        byte[] imageBytes = Base64.decode(imageBytesBase64, Base64.DEFAULT);

        // Convert byte array to Bitmap
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        Log.d(TAG, "Convert byte array to Bitmap: " + bitmap.toString());

        return bitmap;
    }

    public void startLandingAlgo() {
        startPlaneDetectionAlgo(true);
    }

    public void startPlaneDetectionAlgo(boolean inAlgo) {
        Log.d(TAG, "entered startPlaneDetectionAlgo");

        isDetectingPlane = true;

        // Initialize Python and load the module
        try {
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(context)); // 'this' is the Context here
            }

            python = Python.getInstance();
            PlaneHandlerClass = python.getModule("PlaneHandler");
            getOutputFunc = PlaneHandlerClass.get("start_detect");
        } catch (Exception err) {
            err.printStackTrace();
        }

        pitch_pid.reset();
        roll_pid.reset();
        throttle_pid.reset();

        new Thread(() -> {
            try {

                while (isDetectingPlane) {

                    if (getOutputFunc != null) {

                        boolean validImages = validateImages();
                        if (!validImages) {
                            return;
                        }

                        byte[] previousImageBytes = matToBytes(previous_image);
                        byte[] currentImageBytes = matToBytes(current_image);
                        double altitude = dataFromDrone.isUltrasonicBeingUsed() ? dataFromDrone.getAltitudeBelow() : dataFromDrone.getGPS().getAltitude();
                        double baseLine = calculateBaseline(previous_image_pos, current_image_pos);

                        prevTime = System.currentTimeMillis();


                        try {
                            // Call Python function with the byte arrays
                            PyObject result = getOutputFunc.call(PyObject.fromJava(previousImageBytes), PyObject.fromJava(currentImageBytes), altitude, baseLine);
                            long currTime = System.currentTimeMillis();
                            double dt = (currTime - prevTime) / 1000.0; // Calculate time difference
                            prevTime = currTime;
//                            showToast(String.valueOf(result));

                            if (result != null && !activate) {
                                // Decode the returned bitmap
                                String bitmapBase64 = result.asList().get(0).toString();
                                Bitmap bitmap = convertBase64ToBitmap(bitmapBase64);

                                // Get the dx and dy values for movement
                                PyObject movement = result.asList().get(1);
                                float dx = movement.asList().get(0).toFloat();
                                float dy = movement.asList().get(1).toFloat();

                                Log.d("PlaneDetection", "Bitmap received. Movement instructions: dx=" + dx + ", dy=" + dy);

                                handler.post(() -> {
                                    showToast("HandlePost");
                                    // Update the ImageView with the new frame
                                    imageView.setImageBitmap(bitmap);

                                    // Check if movement values are non-zero
                                    if (dx != 0 || dy != 0) {
                                        // Control the drone movement

                                        moveDrone(dx, dy, dt);

                                        Log.d(TAG, "Drone moved with dx: " + dx + ", dy: " + dy);

//                                        if (altitude < 4) {

                                        // Stop plane detection
                                        stopPlaneDetectionAlgo();
                                        if (inAlgo) {
//                                            startObjectDetectionAlgo(inAlgo);
                                        }
//                                        }

                                        Log.d(TAG, "Plane detection algorithm stopped.");
                                    }
                                });

                            } else {
                                Log.e("PlaneDetection", "No result returned from Python function 'start_detect'.");
                            }

                        } catch (ClassCastException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(1000 / 30);  // Control frame rate (30 FPS)
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
//                stopPlaneDetectionAlgo();
//                startPlaneDetectionAlgo();
            }
        }).start();

        Log.d(TAG, "ended startPlaneDetectionAlgo");

    }

    private void moveDrone(float dx, float dy, double dt) {
        // TODO
        // Move the drone towards the landing spot
        double altitude = dataFromDrone.isUltrasonicBeingUsed() ? dataFromDrone.getAltitudeBelow() : dataFromDrone.getGPS().getAltitude();

        handler.post(() ->
        {
            this.pitch = dy;
            this.roll = dx;
            this.alt = altitude;
            this.dt = dt;
            this.activate = true;
        });
//                buildControlCommand(dy, dx, dt, altitude));

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

    private void runOnUiThread(Runnable runnable) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(runnable);
    }

    private boolean validateImages() {
        if (current_image == null) {
            Log.d(TAG, "current_image is null");
            return false;
        }

        if (previous_image == null) {
            Log.d(TAG, "previous_image is null");
            return false;
        }

        return true;
    }
}
