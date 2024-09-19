package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.demo.accurateLandingController.EdgeDetection.isIn_blur;
import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;
import static org.opencv.android.Utils.matToBitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ControllerImageDetection {

    // depth map python view

    private static final String TAG = ControllerImageDetection_copy.class.getSimpleName();
    private final int displayFps = 0;
    private final DataFromDrone dataFromDrone;
    //    Double PP = 0.5, II = 0.02, DD = 0.01, MAX_I = 0.5;
    Double PP = 0.5, II = 0.02, DD = 0.01, MAX_I = 0.5;
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
    private Runnable toggleMovementDetection;

    //constructor
    public ControllerImageDetection(DataFromDrone dataFromDrone, FlightControlMethods flightControlMethods, Context context, ImageView imageView, GimbalController gimbalController, YoloDetector yoloDetector, Runnable toggleMovementDetection) {

        this.yoloDetector = yoloDetector;
        this.context = context;
        this.toggleMovementDetection = toggleMovementDetection;
//        this.mainView = mainView;
        this.dataFromDrone = dataFromDrone;
        this.flightControlMethods = flightControlMethods;

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
    }

    // Method to calculate intersection point of two lines given by points (p1, p2) and (p3, p4)
    private static Point calculateIntersection(Point p1, Point p2, Point p3, Point p4) {
        if (p1 == null || p2 == null || p3 == null || p4 == null) {
            return null;
        }
        double a1 = p2.y - p1.y;
        double b1 = p1.x - p2.x;
        double c1 = a1 * p1.x + b1 * p1.y;

        double a2 = p4.y - p3.y;
        double b2 = p3.x - p4.x;
        double c2 = a2 * p3.x + b2 * p3.y;

        double delta = a1 * b2 - a2 * b1;
        if (delta == 0) {
            return new Point(-1, -1); // Lines are parallel
        }

        double x = (b2 * c1 - b1 * c2) / delta;
        double y = (a1 * c2 - a2 * c1) / delta;
        return new Point(x, y);
    }

    // Function to calculate the angle of the line in degrees
    public static double calculateLineAngle(Point p1, Point p2) {
        // Calculate the differences in x and y coordinates
        double deltaX = p2.x - p1.x;
        double deltaY = p2.y - p1.y;

        // Calculate the angle in radians using atan2
        double angleRad = Math.atan2(deltaY, deltaX);

        // Convert the angle from radians to degrees
        double angleDeg = Math.toDegrees(angleRad);

        // Ensure the angle is in the range [0, 360)
        if (angleDeg < 0) {
            angleDeg += 360;
        }
        if (angleDeg >= 360) {
            angleDeg = angleDeg - 360;
        }

        return angleDeg;
    }

    // Function to determine the largest section
    private static int determineLargestSection(Point intersection, Point imgCenter) {
        boolean isAbove = intersection.y < imgCenter.y;
        boolean isLeft = intersection.x < imgCenter.x;

        // Return 1 for top-left, 2 for top-right, 3 for bottom-left, 4 for bottom-right
        if (isAbove) {
            if (isLeft) {
                return 1; // Top-left section
            } else {
                return 2; // Top-right section
            }

        } else {
            if (isLeft) {
                return 3; // Bottom-left section
            } else {
                return 4; // Bottom-right section
            }
        }
    }

    // Adjust bisector angle based on the largest section
    private static double adjustAngleForSection(double bisectorAngle, int section) {
        switch (section) {
            case 1: // Top-left
                bisectorAngle += 90; // Adjust bisector toward top-left
                break;
            case 2: // Top-right
                bisectorAngle -= 90; // Adjust bisector toward top-right
                break;
            case 3: // Bottom-left
                bisectorAngle += 180; // Adjust bisector toward bottom-left
                break;
            case 4: // Bottom-right
                bisectorAngle -= 180; // Adjust bisector toward bottom-right
                break;
        }
        return bisectorAngle;
    }

    public void setClickedPoint(Point clickedPoint) {
        this.clickedPoint = clickedPoint;
        this.lineSelected = false;
    }

    public int getLandingMode() {
        return landingMode;
    }

    public void setLandingMode(int landingMode) {
        this.landingMode = landingMode;
    }

    // Function to calculate the point on the bisector line
    public Point calculateBisectorLine(Point intersection, Point[] line1, Point[] line2, double length) {
        // Calculate the angles of both lines
        double angle1 = calculateLineAngle(line1[0], line1[1]);
        Log.i("EdgeDetect", "line1:" + line1[0] + " , " + line1[1]);
//angle1 = 360 -angle1;
        Log.i("EdgeDetect", "angle line1:" + angle1);
        double angle2 = calculateLineAngle(line2[0], line2[1]);
        Log.i("EdgeDetect", "line2:" + line2[0] + " , " + line2[1]);

        Log.i("EdgeDetect", "angle line2:" + angle2);

        // Handle angular wrap-around: If the difference between the angles is greater than 180,
        // shift one of the angles by 360 degrees to bring them closer together.
//        if (Math.abs(angle1 - angle2) > 180) {
//            if (angle1 > angle2) {
//                angle2 += 360;
//            } else {
//                angle1 += 360;
//            }
//        }
//        Log.i("EdgeDetect", "after: angle line1:" + angle1);
//        Log.i("EdgeDetect", "after: angle line2:" + angle2);

        // Calculate the average angle (bisector angle)
        double bisectorAngle = (angle1 + angle2) / 2;

        // Ensure the bisector line points downward toward the bottom of the image
        // Adjust based on the known corner direction
        if (landingMode == 1) {
            if (bisectorAngle < 270 || bisectorAngle > 360) {
                bisectorAngle += 180; // Adjust to ensure it's in the right quadrant
            }
        } else {
            if (bisectorAngle < 180 || bisectorAngle > 270) {
                bisectorAngle += 180; // Adjust to ensure it's in the correct quadrant
            }
        }

        // After adjusting, ensure the angle is within 0-360 degrees range
        if (bisectorAngle > 360) {
            bisectorAngle -= 360;
        } else if (bisectorAngle < 0) {
            bisectorAngle += 360;
        }
        Log.i("EdgeDetect", "bisectorAngle:" + bisectorAngle);
//
//        else {
//            bisectorAngle += 180;
//        }

        // Convert the angle to radians
        double bisectorAngleRad = Math.toRadians(bisectorAngle);

        // Compute the end point of the bisector line using the length and angle
        double xEnd = intersection.x + length * Math.cos(bisectorAngleRad);
        double yEnd = intersection.y + length * Math.sin(bisectorAngleRad);

        return new Point(xEnd, yEnd);
    }

    private void updateLog(ControlCommand control, int numEdges, Point[] chosenEdge, double dy) {

//        saw_target, edgeX,edgeY,edgeDist,PitchOutput,RollOutput,ErrorX,ErrorY,P,I,D,MaxI

//        controlStatus.put("saw_target",(double)(imageCoordinates.saw_target()? 1 : 0));
        controlStatus.put("saw_target", (double) numEdges);
        if (numEdges > 0) {
            controlStatus.put("edgeX", (chosenEdge[0].x + chosenEdge[1].x) / 2.0);
            controlStatus.put("edgeY", (chosenEdge[0].y + chosenEdge[1].y) / 2.0);
            controlStatus.put("edgeDist", dy);
        }

        controlStatus.put("PitchOutput", (double) control.getPitch());
        controlStatus.put("RollOutput", (double) control.getRoll());

        controlStatus.put("ErrorX", control.xError);
        controlStatus.put("ErrorY", control.yError);

        controlStatus.put("Pp", control.p_pitch);
        controlStatus.put("Ip", control.i_pitch);
        controlStatus.put("Dp", control.d_pitch);

        controlStatus.put("Pr", control.p_roll);
        controlStatus.put("Ir", control.i_roll);
        controlStatus.put("Dr", control.d_roll);

        controlStatus.put("Pt", control.p_Throttle);
        controlStatus.put("It", control.i_Throttle);
        controlStatus.put("Dt", control.d_Throttle);

        controlStatus.put("maxI", control.maxI);

        controlStatus.put("Throttle", (double) control.getVerticalThrottle());

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
        ControlCommand stay = stayOnPlace();
        flightControlMethods.sendVirtualStickCommands(stay, 0.0f);

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

    public ControlCommand moveDroneToLine(Mat imgToProcess) {
        showToast("bestHorizontalLine");
        long currTime = System.currentTimeMillis();
        double dt = (currTime - prevTime) / 1000.0; // Calculate time difference
        prevTime = currTime;

        if (selectedLine == null) {
            return null;
        }
        Point[] currentLine = trackLine.trackSelectedLineUsingOpticalFlow(imgToProcess);
        showToast(Arrays.toString(currentLine));

        double droneRelativeHeight = dataFromDrone.getAltitudeBelow();
        boolean isUltrasonicBeingUsed = dataFromDrone.isUltrasonicBeingUsed();

        if (currentLine == null) {
            if (isUltrasonicBeingUsed && droneRelativeHeight <= 0.15) {
                showToast("Land2!!!!");
                flightControlMethods.land(toggleMovementDetection);
                stopEdgeDetection();
                return null;
            } else {
                return stayOnPlace();
            }
            //TODO: Maybe add a scenario when the line is disappearing from the image
//            return null;
        }

        selectedLine = currentLine;
        Imgproc.line(imgToProcess, selectedLine[0], selectedLine[1], new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);

        double dyRealPitch = adjustDronePosition(selectedLine, imgToProcess.height(), droneRelativeHeight, 0);
        ControlCommand command = buildControlCommandEdge(dyRealPitch, dt, imgToProcess);
        updateLog(command, 1, selectedLine, dyRealPitch);
        return command;
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
            }
            ControlCommand command = moveDroneToLine(imgToProcess);
//            command = detectLending(imgToProcess, droneHeight);

            if (command != null) {
                flightControlMethods.sendVirtualStickCommands(command, 0.0f);
            }
        } catch (Exception e) {
            Log.e("Error: ", Objects.requireNonNull(e.getMessage()));
            showToast(Objects.requireNonNull(e.getMessage()));
            stopEdgeDetection();
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

    private ControlCommand detectLending(Mat imgToProcess, double droneHeight) throws Exception {
        long currTime = System.currentTimeMillis();
        double dt = (currTime - prevTime) / 1000.0; // Calculate time difference
        prevTime = currTime;
        double droneRelativeHeight = dataFromDrone.getAltitudeBelow();
        boolean isUltrasonicBeingUsed = dataFromDrone.isUltrasonicBeingUsed();
        List<Object[]> pointArr = EdgeDetection.detectLines(imgToProcess, droneRelativeHeight);
        int size = pointArr.size();
        showToast("size:  " + size);
        if (size > 0) {
            first_detect = false;
            not_found = 0;
        } else if (!first_detect) {
            ControlCommand stay = stayOnPlace();
            flightControlMethods.sendVirtualStickCommands(stay, 0.0f);
            not_found++;
            if (not_found > 5) {
                if (isUltrasonicBeingUsed && droneRelativeHeight <= 0.15) {
                    releasePythonResources();
                    showToast("Land2!!!!");
                    flightControlMethods.land(this::stopEdgeDetection);
                    return null;
                } else {
                    throw new Exception("Error in detection mode, edge disappear");
                }
            }
            return null;
        } else {
            return null;
        }
        showToast("isIn_blur() " + isIn_blur());
//        Point[] bestLine;
        Point[][] bestLines = selectMiddleBestLines(pointArr, imgToProcess);
//        Point[][] bestLines = isIn_blur() ? selectMiddleBestLines(pointArr, imgToProcess) : selectBest90DegreeLines(pointArr, imgToProcess);

        assert bestLines != null;
        Point[] bestHorizontalLine = bestLines[0];
        Point[] bestVerticalLine = bestLines[1];
        if (bestHorizontalLine != null && bestVerticalLine != null && landingMode != 0) {
            showToast("cornerDetection");
//            Point intersection = calculateIntersection(bestHorizontalLine[0], bestHorizontalLine[1], bestVerticalLine[0], bestVerticalLine[1]);
//            Log.i("intersection:  ", intersection.toString());

//            Point endLine = calculateBisectorLine(intersection, bestHorizontalLine, bestVerticalLine, 500.0);

            // Draw both lines
            Imgproc.line(imgToProcess, bestHorizontalLine[0], bestHorizontalLine[1], new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);
            Imgproc.line(imgToProcess, bestVerticalLine[0], bestVerticalLine[1], new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);
//            Imgproc.line(imgToProcess, intersection, endLine, new Scalar(255, 255, 255), 3, Imgproc.LINE_AA, 0);
            //Level 1
//            double dy = imgToProcess.height() / 2.0 - (bestHorizontalLine[0].y + bestHorizontalLine[1].y) / 2.0;
//            double dx = imgToProcess.width() / 2.0 - (bestHorizontalLine[0].x + bestHorizontalLine[1].x) / 2.0;
//            if (dy < dx) {
//                dy = dx - dy;
//                double GSD = (sensor_width * droneRelativeHeight * 100) / (focal_length * imgToProcess.height());
//
//                // Build control command for both pitch and roll
//                ControlCommand command = buildControlCommandEdge(GSD * dy, 0, dt, imgToProcess);
//                updateLog(command, pointArr.size(), bestHorizontalLine, GSD * dy);
//                return command;
//            } else if (dx < dy) {
//                dx = dy - dx;
//                double GSD = (sensor_width * droneRelativeHeight * 100) / (focal_length * imgToProcess.width());
//
//                // Build control command for both pitch and roll
//                ControlCommand command = buildControlCommandEdge(0, GSD * dx, dt, imgToProcess);
//                updateLog(command, pointArr.size(), bestHorizontalLine, GSD * dx);
//                return command;
//            }
            double dyRealPitch = adjustDronePosition(bestHorizontalLine, imgToProcess.height(), droneRelativeHeight, 0);
            double dxRealRoll = adjustDronePosition(bestVerticalLine, imgToProcess.width(), droneRelativeHeight, 25);

            // Build control command for both pitch and roll
            ControlCommand command = buildControlCommandEdge(dyRealPitch, dxRealRoll, dt, imgToProcess);
            updateLog(command, pointArr.size(), bestHorizontalLine, dyRealPitch);

            return command;
        } else {
            showToast("bestHorizontalLine");

            if (bestVerticalLine != null || bestHorizontalLine == null) {
                bestHorizontalLine = isIn_blur() ? selectMiddleBestLinePoint(pointArr, imgToProcess) : selectBestLinePoint(pointArr);
            }
            Imgproc.line(imgToProcess, bestHorizontalLine[0], bestHorizontalLine[1], new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);

            double dyRealPitch = adjustDronePosition(bestHorizontalLine, imgToProcess.height(), droneRelativeHeight, 0);
            // Build control command for both pitch and roll
            ControlCommand command = buildControlCommandEdge(dyRealPitch, dt, imgToProcess);
            updateLog(command, pointArr.size(), bestHorizontalLine, dyRealPitch);
            return command;
        }
    }

    private Point[][] selectMiddleBestLines(List<Object[]> pointArr, Mat imgToProcess) {
        Point[] bestLine1 = null;
        Point[] bestLine2 = null;
        double bestAngleDiff = Double.MAX_VALUE;
        Point imgCenter = new Point(imgToProcess.width() / 2.0, imgToProcess.height() / 2.0);
        double bestDistanceToCenter = Double.MAX_VALUE;

        // Loop through all possible pairs of lines
        for (int i = 0; i < pointArr.size(); i++) {
            for (int j = i + 1; j < pointArr.size(); j++) {
                Point[] line1 = (Point[]) pointArr.get(i)[0];
                Point[] line2 = (Point[]) pointArr.get(j)[0];

                if (line1 != null && line2 != null && line1[0] != null && line1[1] != null && line2[0] != null && line2[1] != null) {
                    // Calculate angle between the two lines
                    double angle = calculateAngleBetweenLines(line1, line2);

                    // Find the intersection point of the lines
                    Point intersection = calculateIntersection(line1[0], line1[1], line2[0], line2[1]);
//                    Log.i("intersection1:  ", intersection.toString());

                    // Calculate distance from intersection to center of image
                    double distanceToCenter = Math.sqrt(Math.pow(intersection.x - imgCenter.x, 2) + Math.pow(intersection.y - imgCenter.y, 2));

                    // Check if this pair of lines is closer to 90 degrees and closer to the image center
                    if (Math.abs(angle - 90.0) < bestAngleDiff || (Math.abs(angle - 90.0) == bestAngleDiff && distanceToCenter < bestDistanceToCenter)) {
                        bestAngleDiff = Math.abs(angle - 90.0);
                        bestDistanceToCenter = distanceToCenter;
                        bestLine1 = line1;
                        bestLine2 = line2;
                    }
                }
            }
        }
        // If we found the best two lines, assign one as horizontal and the other as vertical
        if (bestLine1 != null && bestLine2 != null) {
            double angle1 = calculateLineAngle(bestLine1[0], bestLine1[1]);
            double angle2 = calculateLineAngle(bestLine2[0], bestLine2[1]);

            // Swap the lines if necessary, so that bestLine1 is horizontal and bestLine2 is vertical
            if (Math.abs(angle1 - 90.0) < Math.abs(angle2 - 90.0)) {
                // Swap them to ensure bestLine1 is the horizontal one
                Point[] temp = bestLine1;
                bestLine1 = bestLine2;
                bestLine2 = temp;
            }

            return new Point[][]{bestLine1, bestLine2};
        }

        // If no valid pair is found, just return the best single line
        bestLine1 = isIn_blur() ? selectMiddleBestLine(pointArr, imgToProcess) : selectBestLine(pointArr, imgToProcess);
        return new Point[][]{bestLine1, null};

    }

    // Helper function to calculate the angle between two lines
    private double calculateAngleBetweenLines(Point[] line1, Point[] line2) {
        double dx1 = line1[1].x - line1[0].x;
        double dy1 = line1[1].y - line1[0].y;
        double dx2 = line2[1].x - line2[0].x;
        double dy2 = line2[1].y - line2[0].y;

        double dotProduct = dx1 * dx2 + dy1 * dy2;
        double magnitude1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
        double magnitude2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);

        // Calculate the angle between the two lines in degrees
        return Math.acos(dotProduct / (magnitude1 * magnitude2)) * (180.0 / Math.PI);
    }

    private Point[][] selectBest90DegreeLines(List<Object[]> pointArr, Mat imgToProcess) {
        Point[][] bestPair = new Point[2][2]; // To store the best pair of lines
        double centerX = imgToProcess.width() / 2.0;
        double centerY = imgToProcess.height() / 2.0;
        double bestScore = Double.MAX_VALUE;

        // Loop through all pairs of lines
        for (int i = 0; i < pointArr.size(); i++) {
            Point[] line1 = (Point[]) pointArr.get(i)[0];
            for (int j = i + 1; j < pointArr.size(); j++) {
                Point[] line2 = (Point[]) pointArr.get(j)[0];

                // Calculate the angle between the two lines
                double[] vector1 = {line1[1].x - line1[0].x, line1[1].y - line1[0].y};
                double[] vector2 = {line2[1].x - line2[0].x, line2[1].y - line2[0].y};

                double dotProduct = vector1[0] * vector2[0] + vector1[1] * vector2[1];
                double mag1 = Math.sqrt(vector1[0] * vector1[0] + vector1[1] * vector1[1]);
                double mag2 = Math.sqrt(vector2[0] * vector2[0] + vector2[1] * vector2[1]);

                double cosTheta = dotProduct / (mag1 * mag2);
                double angle = Math.acos(cosTheta) * (180.0 / Math.PI);

                // Check if the angle is close to 90 degrees
                if (Math.abs(angle - 90.0) < 5.0) { // Allow a small deviation from 90 degrees

                    // Calculate the intersection point (if any)
                    Point intersection = calculateIntersection(line1[0], line1[1], line2[0], line2[1]);
                    Log.i("intersection3:  ", intersection.toString());

                    double dx = intersection.x - centerX;
                    double dy = intersection.y - centerY;
                    double distanceFromCenter = Math.sqrt(dx * dx + dy * dy);

                    // Minimize the distance from the center
                    if (distanceFromCenter < bestScore) {
                        bestScore = distanceFromCenter;
                        bestPair[0] = line1;
                        bestPair[1] = line2;
                    }
                }
            }
        }

        return bestPair[0] != null ? bestPair : null;
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

    private Point[] selectBestLine(List<Object[]> pointArr, Mat imgToProcess) {
        Point[] bestHorizontalLine = null;
        Point[] bestLine = null;
        double dy_best_horizontal = Double.MAX_VALUE;
        double dy_best = Double.MAX_VALUE;

        Log.i("EdgeDetection", "size3: " + pointArr.size());
        for (Object[] entry : pointArr) {
            Point[] point = (Point[]) entry[0];

            if (point != null && point[0] != null && point[1] != null) {

                // Access the horizontal flag (cast to Boolean)
                boolean isHorizontal = (Boolean) entry[1];

                double c_y = (point[0].y + point[1].y) / 2.0;
                double dy = imgToProcess.height() / 2.0 - c_y;
//                Imgproc.putText(imgToProcess, String.valueOf(slope), new Point((point[0].x + point[1].x) / 2.0 - 20, (point[0].y + point[1].y) / 2.0 - 20) , 5, 1, new Scalar(255, 255, 0));

//                Log.d("EdgeDetection", Arrays.toString(point) + " : " + slope);
                if (isHorizontal) {
                    if (Math.abs(dy) < dy_best_horizontal) {
                        bestHorizontalLine = point;
                        dy_best_horizontal = Math.abs(dy);
                    }
                } else if (Math.abs(dy) < dy_best) {
                    bestLine = point;
                    dy_best = Math.abs(dy);
                }
            }
        }

        return bestHorizontalLine != null ? bestHorizontalLine : bestLine;
    }

    private Point[] selectMiddleBestLine(List<Object[]> pointArr, Mat imgToProcess) {
        Point[] bestHorizontalLine = null;
        Point[] bestLine = null;
        double dy_best_horizontal = Double.MAX_VALUE;
        double dy_best = Double.MAX_VALUE;

        double proximityThreshold = 10.0;
        List<List<Point[]>> horizontalGroups = new ArrayList<>();

        Log.i("EdgeDetection", "array size:" + pointArr.size());
        for (Object[] entry : pointArr) {
            Point[] point = (Point[]) entry[0];

            if (point != null && point[0] != null && point[1] != null) {

                // Access the horizontal flag (cast to Boolean)
                boolean isHorizontal = (Boolean) entry[1];

                double c_y = (point[0].y + point[1].y) / 2.0;
                double dy = imgToProcess.height() / 2.0 - c_y;
//                Imgproc.putText(imgToProcess, String.valueOf(slope), new Point((point[0].x + point[1].x) / 2.0 - 20, (point[0].y + point[1].y) / 2.0 - 20) , 5, 1, new Scalar(255, 255, 0));

//                Log.d("EdgeDetection", Arrays.toString(point) + " : " + slope);
                if (isHorizontal) {
                    boolean addedToGroup = false;
                    // Check if this line is close to any existing group
                    for (List<Point[]> group : horizontalGroups) {
                        double groupCenterY = (group.get(0)[0].y + group.get(0)[1].y) / 2.0;
                        if (Math.abs(c_y - groupCenterY) < proximityThreshold) {
                            group.add(point);
                            addedToGroup = true;
                            break;
                        }
                    }
                    // If no group was close enough, create a new group
                    if (!addedToGroup) {
                        List<Point[]> newGroup = new ArrayList<>();
                        newGroup.add(point);
                        horizontalGroups.add(newGroup);
                    }
//                    if (Math.abs(dy) < dy_best_horizontal) {
//                        bestHorizontalLine = point;
//                        dy_best_horizontal = Math.abs(dy);
//                    }
                } else if (Math.abs(dy) < dy_best) {
                    bestLine = point;
                    dy_best = Math.abs(dy);
                }
            }
        }

        // Find the best line within the largest group of horizontal lines
        for (List<Point[]> group : horizontalGroups) {
            if (group.size() > 1) { // Only consider groups with multiple lines
                // Find the line closest to the center of this group
                double groupCenterY = 0;
                for (Point[] line : group) {
                    groupCenterY += (line[0].y + line[1].y) / 2.0;
                }
                groupCenterY /= group.size();

                for (Point[] line : group) {
                    double c_y = (line[0].y + line[1].y) / 2.0;
                    double dy = Math.abs(c_y - groupCenterY);
                    if (dy < dy_best_horizontal) {
                        bestHorizontalLine = line;
                        dy_best_horizontal = dy;
                    }
                }
            }
        }
        // In case no group is found, fallback to the closest general line
        for (Object[] entry : pointArr) {
            Point[] point = (Point[]) entry[0];
            if (point != null && point[0] != null && point[1] != null) {
                boolean isHorizontal = (Boolean) entry[1];
                double c_y = (point[0].y + point[1].y) / 2.0;
                double dy = imgToProcess.height() / 2.0 - c_y;

                if (!isHorizontal && Math.abs(dy) < dy_best) {
                    bestLine = point;
                    dy_best = Math.abs(dy);
                }
            }
        }

        return bestHorizontalLine != null ? bestHorizontalLine : bestLine;
    }

    private Point[] selectMiddleBestLinePoint(List<Object[]> pointArr, Mat imgToProcess) {
        Point[] bestHorizontalLine = null;
        Point[] bestLine = null;
        double dy_best_horizontal = Double.MAX_VALUE;
        double dy_best = Double.MAX_VALUE;

        double proximityThreshold = 10.0;
        List<List<Point[]>> horizontalGroups = new ArrayList<>();

        Log.i("EdgeDetection", "array size:" + pointArr.size());
        for (Object[] entry : pointArr) {
            Point[] point = (Point[]) entry[0];

            if (point != null && point[0] != null && point[1] != null) {

                // Access the horizontal flag (cast to Boolean)
                boolean isHorizontal = (Boolean) entry[1];

                double c_y = (point[0].y + point[1].y) / 2.0;
                double dy = imgToProcess.height() / 2.0 - c_y;
//                Imgproc.putText(imgToProcess, String.valueOf(slope), new Point((point[0].x + point[1].x) / 2.0 - 20, (point[0].y + point[1].y) / 2.0 - 20) , 5, 1, new Scalar(255, 255, 0));

//                Log.d("EdgeDetection", Arrays.toString(point) + " : " + slope);
                if (isHorizontal) {
                    boolean addedToGroup = false;
                    // Check if this line is close to any existing group
                    for (List<Point[]> group : horizontalGroups) {
                        double groupCenterY = (group.get(0)[0].y + group.get(0)[1].y) / 2.0;
                        if (Math.abs(c_y - groupCenterY) < proximityThreshold) {
                            group.add(point);
                            addedToGroup = true;
                            break;
                        }
                    }
                    // If no group was close enough, create a new group
                    if (!addedToGroup) {
                        List<Point[]> newGroup = new ArrayList<>();
                        newGroup.add(point);
                        horizontalGroups.add(newGroup);
                    }
//                    if (Math.abs(dy) < dy_best_horizontal) {
//                        bestHorizontalLine = point;
//                        dy_best_horizontal = Math.abs(dy);
//                    }
                } else if (Math.abs(dy) < dy_best) {
                    bestLine = point;
                    dy_best = Math.abs(dy);
                }
            }
        }

        // Find the best line within the largest group of horizontal lines
        for (List<Point[]> group : horizontalGroups) {
            if (group.size() > 1) { // Only consider groups with multiple lines
                // Find the line closest to the center of this group
                double groupCenterY = 0;
                for (Point[] line : group) {
                    groupCenterY += (line[0].y + line[1].y) / 2.0;
                }
                groupCenterY /= group.size();

                for (Point[] line : group) {

                    // Calculate the midpoint of the line
                    double midX = (line[0].x + line[1].x) / 2.0;
                    double midY = (line[0].y + line[1].y) / 2.0;

                    // Calculate Euclidean distance from the clicked point to the line's midpoint
                    double distance = Math.sqrt(Math.pow(clickedPoint.x - midX, 2) + Math.pow(clickedPoint.y - midY, 2));

                    // Check if this line is closer to the clicked point than the current best
                    if (distance < dy_best_horizontal) {
                        bestLine = line;
                        dy_best_horizontal = distance;
                    }

//                    if (dy < dy_best_horizontal) {
//                        bestHorizontalLine = line;
//                        dy_best_horizontal = dy;
//                    }
                }
            }
        }
        // In case no group is found, fallback to the closest general line
        for (Object[] entry : pointArr) {
            Point[] point = (Point[]) entry[0];
            if (point != null && point[0] != null && point[1] != null) {
                boolean isHorizontal = (Boolean) entry[1];
//                double c_y = (point[0].y + point[1].y) / 2.0;
//                double dy = imgToProcess.height() / 2.0 - c_y;
                // Calculate the midpoint of the line
                double midX = (point[0].x + point[1].x) / 2.0;
                double midY = (point[0].y + point[1].y) / 2.0;

                // Calculate Euclidean distance from the clicked point to the line's midpoint
                double distance = Math.sqrt(Math.pow(clickedPoint.x - midX, 2) + Math.pow(clickedPoint.y - midY, 2));

                // Check if this line is closer to the clicked point than the current best
//                if (distance < dy_best_horizontal) {
//                    bestLine = point;
//                    dy_best_horizontal = distance;
//                }
                if (!isHorizontal && Math.abs(distance) < dy_best) {
                    bestLine = point;
                    dy_best = Math.abs(distance);
                }
            }
        }

        return bestHorizontalLine != null ? bestHorizontalLine : bestLine;
    }

    private Point[][] selectBestLines(List<Object[]> pointArr, Mat imgToProcess) {
        Point[] bestHorizontalLine = null;
        Point[] bestVerticalLine = null;
        double dy_best_horizontal = Double.MAX_VALUE;
        double dx_best_vertical = Double.MAX_VALUE;

        Log.i("EdgeDetection", "" + pointArr.size());
        for (Object[] entry : pointArr) {
            Point[] point = (Point[]) entry[0];

            if (point != null && point[0] != null && point[1] != null) {

                // Access the horizontal flag (cast to Boolean)
                boolean isHorizontal = (Boolean) entry[1];

                // Calculate vertical distance to the center for horizontal lines
                if (isHorizontal) {
                    double c_y = (point[0].y + point[1].y) / 2.0;
                    double dy = imgToProcess.height() / 2.0 - c_y;

                    if (Math.abs(dy) < dy_best_horizontal) {
                        bestHorizontalLine = point;
                        dy_best_horizontal = Math.abs(dy);
                    }
                }
                // Calculate horizontal distance to the center for vertical lines
                else {
                    double c_x = (point[0].x + point[1].x) / 2.0;
                    double dx = imgToProcess.width() / 2.0 - c_x;

                    if (Math.abs(dx) < dx_best_vertical) {
                        bestVerticalLine = point;
                        dx_best_vertical = Math.abs(dx);
                    }
                }
            }
        }

        // Return both best horizontal and vertical lines as an array
        return new Point[][]{bestHorizontalLine, bestVerticalLine};
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

    private double adjustDronePosition(Point[] finalLine, double frameSizePx, double droneHeight, double distanceFromCenter) {
        distanceFromCenter = distanceFromCenter * (landingMode == 1 ? 1 : -1);
        double dy_best = (frameSizePx / 2.0 + distanceFromCenter) - (finalLine[0].y + finalLine[1].y) / 2.0;
        double GSD = (sensor_width * droneHeight * 100) / (focal_length * frameSizePx);

        return GSD * dy_best;
    }

    private ControlCommand buildControlCommandEdge(double dyReal, double dt, Mat imgToProcess) {
        double maxSpeed = 2.0;
        double Kp = 0.02;  // Proportional gain, adjust as needed - 0.01
        error_y = dyReal * Kp;

        if (0 <= error_y && error_y <= 0.001) {
            showToast("Landing!!");
            t = -0.02f;
            error_y = 0.02f;

        } else {
            t = 0;
        }
        p = (float) pitch_pid.update(error_y, dt, maxSpeed);
//        r = (float) roll_pid.update(0, dt, maxSpeed);
        t = (float) throttle_pid.update(t, dt, maxSpeed);
        Imgproc.putText(imgToProcess, "dy: " + dyReal, new Point(imgToProcess.cols() / 2.0, imgToProcess.rows() / 2.0), 5, 1, new Scalar(0, 255, 0));
        showToast("p: " + p);

        ControlCommand ans = new ControlCommand(p, r, t);
        ans.setErr(1000, error_x, error_y, dataFromDrone.getAltitudeBelow());
        ans.setPID(throttle_pid.getP(), throttle_pid.getI(), throttle_pid.getD(), pitch_pid.getP(), pitch_pid.getI(), pitch_pid.getD(), roll_pid.getP(), roll_pid.getI(), roll_pid.getD(), roll_pid.getMax_i());

        return ans;
    }

    private ControlCommand buildControlCommandEdge(double dyReal, double dxReal, double dt, Mat imgToProcess) {
        double maxSpeed = 2.0;
        double Kp = 0.02;  // Proportional gain, adjust as needed - 0.01
        error_y = dyReal * Kp;
        error_x = dxReal * Kp;

        if (0 <= error_y && error_y <= 0.001 && 0 <= error_x && error_x <= 0.001) {
            showToast("Landing!!");

            t = -0.05f;
            error_y = 0.05f;
            error_x = 0.05f;

        } else {
            t = 0;
        }
        p = (float) pitch_pid.update(error_y, dt, maxSpeed);
        r = (float) roll_pid.update(error_x, dt, maxSpeed);
        t = (float) throttle_pid.update(t, dt, maxSpeed);
        Imgproc.putText(imgToProcess, "dy: " + dyReal, new Point(imgToProcess.cols() / 2.0, imgToProcess.rows() / 2.0), 5, 1, new Scalar(0, 255, 0));
        showToast("p: " + p);

        ControlCommand ans = new ControlCommand(p, r, t);
        ans.setErr(1000, error_x, error_y, dataFromDrone.getAltitudeBelow());
        ans.setPID(throttle_pid.getP(), throttle_pid.getI(), throttle_pid.getD(), pitch_pid.getP(), pitch_pid.getI(), pitch_pid.getD(), roll_pid.getP(), roll_pid.getI(), roll_pid.getD(), roll_pid.getMax_i());

        return ans;
    }

    public void stopPlanarVideo() {
        isPlaying = false;
//        releasePythonResources();
    }

    // TODO : Use this function after the drone has landed
    public void stopPlaneDetectionAlgo() {
        isDetectingPlane = false;
//        releasePythonResources();
    }

    public void stopObjectDetectionAlgo() {
        isObjectDetecting = false;
//        releasePythonResources();
    }

    private void releasePythonResources() {
        if (PlaneHandlerClass != null) {
            try {
                PlaneHandlerClass.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing Python module", e);
            }
            PlaneHandlerClass = null;
        }

        if (getOutputFunc != null) {
            try {
                getOutputFunc.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing Python function", e);
            }
            getOutputFunc = null;
        }
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
                            showToast(String.valueOf(result));
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
                                            startObjectDetectionAlgo(inAlgo);
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

    public void startObjectDetectionAlgo(boolean inAlgo) {
        Log.d(TAG, "entered startObjectDetectionAlgo");

        isObjectDetecting = true;

        DroneSafety droneSafety = new DroneSafety(yoloDetector);

        new Thread(() -> {
            try {

                while (isObjectDetecting) {
                    // Check for hazards before attempting to land
                    boolean isHazardous = droneSafety.checkForHazards(current_image);

                    if (isHazardous) {
                        System.out.println("Landing aborted due to hazards.");
                        runOnUiThread(() -> showToast("Landing aborted due to hazards."));
                    } else {
                        System.out.println("No hazards detected. Safe to land.");
                        runOnUiThread(() -> showToast("No hazards detected. Safe to land."));
                        stopObjectDetectionAlgo();
                        if (inAlgo) {
                            setEdgeDetectionMode(true);
                        }
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();

            }
        }).start();

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
