package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;
import static org.opencv.android.Utils.matToBitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
    Double PP = 0.5, II = 0.02, DD = 0.01, MAX_I = 0.5;
    private ImageView imageView;  // ImageView to display the frames
    private boolean isPlaying = false;  // To control the video playback
    private boolean isDetectingPlane = false;
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

    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private boolean check_depth;
    private FlightControlMethods flightControlMethods;
    private float descentRate = 0;

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


    private double error_x, error_y, error_z, error_yaw, D;

    private double pitch = 0.0, roll = 0.0, alt = 0.0, dt = 0.0;
    private boolean activate = false;


    //    private RecordingVideo recordingVideo = null;
    //constructor
    public ControllerImageDetection(DataFromDrone dataFromDrone, FlightControlMethods flightControlMethods, Context context, ImageView imageView
            , GimbalController gimbalController
//            , RecordingVideo recordingVideo
    ) {
        this.context = context;
//        this.mainView = mainView;
        this.dataFromDrone = dataFromDrone;
        this.flightControlMethods = flightControlMethods;

//        this.recordingVideo = recordingVideo;
        this.gimbalController = gimbalController;
        this.imageView = imageView;  // Initialize the ImageView

        //Do we need it
        this.frameWidth = 640;
        this.frameHeight = 480;

//        p = Float.parseFloat(textP.getText().toString());
//        i = Float.parseFloat(textI.getText().toString());
//        d = Float.parseFloat(textD.getText().toString());
//        t = Float.parseFloat(textT.getText().toString());
//
        initPIDs(p, i, d, max_i, "roll");
        initPIDs(p, i, d, max_i, "pitch");
        initPIDs(p, i, d, max_i, "throttle");
        setDescentRate(t);

//        this.objectTracking = new ObjectTracking(true, "GREEN");
//        centerTracker = new CenterTracker();

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
    //    private double calculateVerticalFOV(double FOV, double aspectRatio) {
//        double halfHorizontalFOV = Math.toRadians(FOV / 2);
//        double verticalFOV = 2 * Math.atan(Math.tan(halfHorizontalFOV) / aspectRatio);
//        return Math.toDegrees(verticalFOV);
//    }

    public void DepthBool() {
        this.check_depth = true;
    }

    public boolean isCheck_depth() {
        return check_depth;
    }

    public void setDescentRate(float descentRate) {
        if (descentRate > 0) {
            descentRate = -descentRate;
        }

        this.descentRate = descentRate;
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


//        if (roll_pid == null) {
//            roll_pid = new VLD_PID(p, i, d, max_i);
//            pitch_pid = new VLD_PID(p, i, d, max_i);
//            throttle_pid = new VLD_PID(p, i, d, max_i);
//        }
//        else{
//            roll_pid.setPID(p, i, d, max_i);
//            pitch_pid.setPID(p, i, d, max_i);
//            throttle_pid.setPID(p, i, d, max_i);
//        }
    }

    public void saveImage(Bitmap bitmap, String filename) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d(TAG,
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }
//        BufferedWriter logFile;
//        File externalStorage = context.getExternalFilesDir(null);
//        String fileName = externalStorage.getAbsolutePath() + filename + System.currentTimeMillis() + ".png";
//        File log = new File(fileName);
//
//        try {
//            if (!log.exists()) {
//                boolean data = log.createNewFile();
//            }
//        } catch (IOException e) {
//            ToastUtils.showToast(e.getMessage());
//            e.printStackTrace();
//        }
//        try {
//
//            logFile = new BufferedWriter(new FileWriter(log.getAbsoluteFile()));
////            logFile.write(String.join(",", header) + "\r\n");
//            logFile.compress(Bitmap.CompressFormat.PNG, 100, fos); // PNG is lossless, so quality is ignored
//
//            logFile.flush();
//            logFile.close();
//            logFile = null;
//            ToastUtils.showToast("Close Log");
//
//        } catch (IOException e) {
//            e.printStackTrace();
////            text.setText(e.getMessage());
//
//        }

    //        // Create the image file
//        File imageFile = new File(Environment.getExternalStorageDirectory(), filename + ".png");
//        FileOutputStream fos = null;
//
//        try {
//            // Open a FileOutputStream to write the image data
//            fos = new FileOutputStream(imageFile);
//
//            // Compress the bitmap and write to the OutputStream
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos); // PNG is lossless, so quality is ignored
//
//            fos.flush();  // Make sure all data is written
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            if (fos != null) {
//                try {
//                    fos.close();  // Close the OutputStream
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
    private File getOutputMediaFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + context.getPackageName()
                + "/Files");

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        // Create a media file name
        File mediaFile;
        String mImageName = "MI_" + System.currentTimeMillis() + ".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }

    public double[] getPIDs(String type) {
        double[] ans = {-1, -1, -1};
        if (type.equals("roll")) {
            if (roll_pid == null) {
                return ans;
            } else {
                ans[0] = roll_pid.getP();
                ans[1] = roll_pid.getI();
                ans[2] = roll_pid.getD();
                return ans;
            }
        }

        if (type.equals("pitch")) {
            if (pitch_pid == null) {
                return ans;
            } else {
                ans[0] = pitch_pid.getP();
                ans[1] = pitch_pid.getI();
                ans[2] = pitch_pid.getD();
                return ans;
            }
        }

        if (type.equals("throttle")) {
            if (throttle_pid == null) {
                return ans;
            } else {
                ans[0] = throttle_pid.getP();
                ans[1] = throttle_pid.getI();
                ans[2] = throttle_pid.getD();
                return ans;
            }
        }

        return ans;
    }

    public void setBitmapFrame(Bitmap bitmap) {
        try {

//        if (t + 1000 < System.currentTimeMillis()) {
//            t = System.currentTimeMillis();
//            Log.i("arrk", "fps " + frameCounter);
//            displayFps = frameCounter;
//            frameCounter = 0;
//        } else {
//            frameCounter++;
//        }

            double droneHeight = dataFromDrone.getGPS().getAltitude();
//            if (aspectRatio == 0) {
//                aspectRatio = calculateAspectRatio(bitmap.getWidth(), bitmap.getHeight());
////                VerticalFOV = calculateVerticalFOV(FOV, aspectRatio);
//            }
            processImage(bitmap, droneHeight);
//            if (recordingVideo.getIsRecording()) {
//                // Convert the Bitmap to a format suitable for FFmpeg
////                byte[] frameData = convertBitmapToByteArray(bitmap);
//                recordingVideo.writeFrame(bitmap);
//            }
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

    public void setCurrentImage(Bitmap frame, double[] pos) {
        Mat newCurrentImg = new Mat();
        Utils.bitmapToMat(frame, newCurrentImg);

        if (previous_image == null) {
//            saveImage(frame, "current_image");

            // This is the first image being set
            previous_image = newCurrentImg;
            previous_image_pos = pos;
        } else {
//            saveImage(frame, "previous_image");

            // Move the current image to previous_image
            previous_image = current_image;
            previous_image_pos = current_image_pos;
        }

        // Set the new current image
        current_frame = frame;
        current_image = newCurrentImg;
        current_image_pos = pos;
    }

    public void processImage(Bitmap frame, double droneHeight) {
        // Added python function but it causes an error in loading
        Mat imgToProcess = new Mat();
        Utils.bitmapToMat(frame, imgToProcess);
        try {
            ControlCommand command = detectLending(imgToProcess, droneHeight);

            if (command != null) {
                flightControlMethods.sendVirtualStickCommands(command, 0.0f);
            }
        } catch (Exception e) {
            Log.e("Error: ", Objects.requireNonNull(e.getMessage()));
            showToast(Objects.requireNonNull(e.getMessage()));
            stopEdgeDetection();
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
        //  מאפס את כל הערכים לאפס - מתייצב
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

    public ControlCommand detectLending(Mat imgToProcess, double droneHeight) throws Exception {

        long currTime = System.currentTimeMillis();
        double dt = (currTime - prevTime) / 1000.0; // Calculate time difference
        prevTime = currTime;
        double droneRelativeHeight = dataFromDrone.getAltitudeBelow();
        boolean isUltrasonicBeingUsed = dataFromDrone.isUltrasonicBeingUsed();

        if ((isUltrasonicBeingUsed && droneRelativeHeight <= 0.2) || droneHeight <= 0.2) {
            showToast("Land!!!!");
            releasePythonResources();
            ControlCommand ans = new ControlCommand(0, 0, -3);
            ans.setErr(1000, error_x, error_y, dataFromDrone.getAltitudeBelow());
            ans.setPID(throttle_pid.getP(), throttle_pid.getI(), throttle_pid.getD(), pitch_pid.getP(), pitch_pid.getI(), pitch_pid.getD(), roll_pid.getP(), roll_pid.getI(), roll_pid.getD(), roll_pid.getMax_i());
            return ans;

        }
        // Initialize variables for edge detection and pitch/roll adjustment
        List<Object[]> pointArr = EdgeDetection.detectLines(imgToProcess);

        if (pointArr.size() > 0) {
            first_detect = false;
            not_found = 0;
        } else if (!first_detect) {
            ControlCommand stay = stayOnPlace();
            flightControlMethods.sendVirtualStickCommands(stay, 0.0f);
            not_found++;
            if (not_found > 5) {
                if (isUltrasonicBeingUsed && droneRelativeHeight <= 0.3) {
                    releasePythonResources();
                    showToast("Land2!!!!");
                    return flightControlMethods.land();
                } else {
                    throw new Exception("Error in detection mode, edge disappear");
                }
            }
            return null;
        } else {
            return null;
        }

        Point[] bestLine = selectBestLine(pointArr, imgToProcess);

        if (bestLine != null) {
            Imgproc.line(imgToProcess, bestLine[0], bestLine[1], new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);
            double dyRealPitch = adjustDronePosition(bestLine, imgToProcess.height(), droneRelativeHeight);
            ControlCommand command = buildControlCommandEdge(dyRealPitch, dt, imgToProcess);
            updateLog(command, pointArr.size(), bestLine, dyRealPitch);

            return command;

        } else {
            return null; // No line detected or valid
        }
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

//        double lat1 = Math.toRadians(pos1.getLatitude());
//        double lon1 = Math.toRadians(pos1.getLongitude());
//        double alt1 = pos1.getAltitude();
//
//        double lat2 = Math.toRadians(pos2.getLatitude());
//        double lon2 = Math.toRadians(pos2.getLongitude());
//        double alt2 = pos2.getAltitude();
//
//        // Earth radius in meters
//        final double R = 6371000;
//
//        // Haversine formula
//        double dLat = lat2 - lat1;
//        double dLon = lon2 - lon1;
//        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
//                Math.cos(lat1) * Math.cos(lat2) *
//                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
//        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
//        double horizontalDistance = R * c;
//
//        // Total baseline distance (Euclidean)
//        return Math.sqrt(horizontalDistance * horizontalDistance + Math.pow((alt2 - alt1), 2));
    }

    private Point[] selectBestLine(List<Object[]> pointArr, Mat imgToProcess) {
        Point[] bestHorizontalLine = null;
        Point[] bestLine = null;
        double dy_best_horizontal = Double.MAX_VALUE;
        double dy_best = Double.MAX_VALUE;

        Log.i("EdgeDetection", "" + pointArr.size());
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

    /**
     * @param finalLine
     * @param frameSizePx is the size of the image in pixels, if calculating the roll - width,
     *                    if calculating the pitch - height
     * @param droneHeight
     * @return
     */
    private double adjustDronePosition(Point[] finalLine, double frameSizePx, double droneHeight) {
        double dy_best = frameSizePx / 2.0 - (finalLine[0].y + finalLine[1].y) / 2.0;
        double GSD = (sensor_width * droneHeight * 100) / (focal_length * frameSizePx);

        return GSD * dy_best;
    }

    private ControlCommand buildControlCommandEdge(double dyReal, double dt, Mat imgToProcess) {
        double maxSpeed = 2.0;
        double Kp = 0.02;  // Proportional gain, adjust as needed - 0.01
        error_y = dyReal * Kp;

        if (0 <= error_y && error_y <= 0.001) {
            showToast("Landing!!");

            t = -0.05f;
            error_y = 0.05f;

        } else {
            t = 0;
        }
        p = (float) pitch_pid.update(error_y, dt, maxSpeed);
        r = (float) roll_pid.update(0, dt, maxSpeed);
        t = (float) throttle_pid.update(t, dt, maxSpeed);
        Imgproc.putText(imgToProcess, "dy: " + dyReal, new Point(imgToProcess.cols() / 2.0, imgToProcess.rows() / 2.0), 5, 1, new Scalar(0, 255, 0));
        showToast("p: " + p);

        ControlCommand ans = new ControlCommand(p, r, t);
        ans.setErr(1000, error_x, error_y, dataFromDrone.getAltitudeBelow());
        ans.setPID(throttle_pid.getP(), throttle_pid.getI(), throttle_pid.getD(), pitch_pid.getP(), pitch_pid.getI(), pitch_pid.getD(), roll_pid.getP(), roll_pid.getI(), roll_pid.getD(), roll_pid.getMax_i());

        return ans;
    }

    void buildControlCommand(double pitch, double roll, double dt, double altitude) {
        activate = true;
        Log.i("buildControlCommand", "buildControlCommand");
        double maxSpeed = 2.0;
        double Kp = 0.02;  // Proportional gain, adjust as needed - 0.01
        error_y = pitch * Kp;
        error_x = roll * Kp;

        p = (float) pitch_pid.update(error_y, dt, maxSpeed);
        r = (float) roll_pid.update(error_x, dt, maxSpeed);
        t = (float) throttle_pid.update(0, dt, maxSpeed);
        showToast("p: " + p);

        ControlCommand ans = new ControlCommand(p, r, t);
        ans.setErr(1000, error_x, error_y, error_z);
        ans.setPID(throttle_pid.getP(), throttle_pid.getI(), throttle_pid.getD(), pitch_pid.getP(), pitch_pid.getI(), pitch_pid.getD(), roll_pid.getP(), roll_pid.getI(), roll_pid.getD(), roll_pid.getMax_i());
        flightControlMethods.sendVirtualStickCommands(ans, 0.0f);

        if (altitude > 2) {
            error_z = 2;
        } else {
            error_z = 0;
        }
        p = (float) pitch_pid.update(0, dt, maxSpeed);
        r = (float) roll_pid.update(0, dt, maxSpeed);
        t = (float) throttle_pid.update(error_z, dt, maxSpeed);
        ans = new ControlCommand(p, r, t);
        ans.setErr(1000, error_x, error_y, error_z);
        ans.setPID(throttle_pid.getP(), throttle_pid.getI(), throttle_pid.getD(), pitch_pid.getP(), pitch_pid.getI(), pitch_pid.getD(), roll_pid.getP(), roll_pid.getI(), roll_pid.getD(), roll_pid.getMax_i());
        flightControlMethods.sendVirtualStickCommands(ans, 0.0f);

        activate = false;
    }

    // Method to start video playback
    // display depthmap python

    public void startDepthMapVideo() {

        Log.d(TAG, "entered startDepthMapVideo");

        isPlaying = true;

        new Thread(() -> {
            try {

                while (isPlaying) {

                    if (getOutputFunc != null && !activate) {

                        boolean validImages = validateImages();
                        if (!validImages) {
                            return;
                        }

                        long currTime = System.currentTimeMillis();
                        double dt = (currTime - prevTime) / 1000.0; // Calculate time difference
                        prevTime = currTime;
                        byte[] previousImageBytes = matToBytes(previous_image);
                        byte[] currentImageBytes = matToBytes(current_image);
                        double altitude = dataFromDrone.isUltrasonicBeingUsed() ? dataFromDrone.getAltitudeBelow() : dataFromDrone.getGPS().getAltitude();
                        double baseLine = calculateBaseline(previous_image_pos, current_image_pos);
                        Log.i("baseLine", "" + baseLine);

                        try {
                            // Call Python function with the byte arrays
                            PyObject result = getOutputFunc.call(PyObject.fromJava(previousImageBytes), PyObject.fromJava(currentImageBytes), altitude, baseLine);

                            PyObject imageBytesObj = result.asList().get(0);
                            PyObject positionsObj = result.asList().get(1);

                            String imageBytesBase64 = imageBytesObj.toString();
                            Bitmap bitmap = convertBase64ToBitmap(imageBytesBase64);

                            List<PyObject> positionsPyList = positionsObj.asList();
                            List<Object> javaList = new ArrayList<>();
                            for (PyObject item : positionsPyList) {
                                javaList.add(item.toJava(Object.class));  // Convert each Python object to a Java object
                            }

                            Log.i("EdgeDetect:", javaList.toString());
                            if (javaList.size() == 2) {
                                this.pitch = (double) javaList.get(1);
                                this.roll = (double) javaList.get(0);
                                this.activate = true;
                                this.alt = altitude;
                                this.dt = dt;
                            }
//                            javaList.get(0);

                            handler.post(() -> {

                                imageView.setImageBitmap(bitmap);  // Update the ImageView with the new frame
                                Log.d(TAG, "imageView updated");
                            });
                        } catch (ClassCastException e) {
                            e.printStackTrace();

//                            throw new RuntimeException("Error processing Python result", e);
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

            }
        }).start();

        Log.d(TAG, "ended startDepthMapVideo");
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

//    public double calculateAspectRatio(int width, int height) {
//        return (double) width / height;
//    }

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
        startPlaneDetectionAlgo();
        startObjectDetectionAlgo();
//        detectLending();
    }

    public void startPlaneDetectionAlgo() {
        Log.d(TAG, "entered startPlaneDetectionAlgo");

        isDetectingPlane = true;

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(context));
        }

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

                        long currTime = System.currentTimeMillis();
                        double dt = (currTime - prevTime) / 1000.0; // Calculate time difference
                        prevTime = currTime;

                        try {
                            // Call Python function with the byte arrays
                            PyObject result = getOutputFunc.call(PyObject.fromJava(previousImageBytes), PyObject.fromJava(currentImageBytes), altitude, baseLine);

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
                                    // Update the ImageView with the new frame
                                    imageView.setImageBitmap(bitmap);

                                    // Check if movement values are non-zero
                                    if (dx != 0 || dy != 0) {
                                        // Control the drone movement

                                        moveDrone(dx, dy, dt);

                                        Log.d(TAG, "Drone moved with dx: " + dx + ", dy: " + dy);

                                        if(altitude < 4){

                                            // Stop plane detection
                                            stopPlaneDetectionAlgo();
                                        }

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

            }
        }).start();

        Log.d(TAG, "ended startPlaneDetectionAlgo");

    }

    private void moveDrone(float dx, float dy, double dt) {
        // TODO
        // Move the drone towards the landing spot
        double altitude = dataFromDrone.isUltrasonicBeingUsed() ? dataFromDrone.getAltitudeBelow() : dataFromDrone.getGPS().getAltitude();

        buildControlCommand(dy, dx, dt, altitude);
    }

    public void startObjectDetectionAlgo() {
        Log.d(TAG, "entered startObjectDetectionAlgo");

        isObjectDetecting = true;

        // Initialize YOLO detector and hazard detection system
        YoloDetector yoloDetector = new YoloDetector(context, "yolov3-tiny.cfg", "yolov3-tiny.weights");
        DroneSafety droneSafety = new DroneSafety(yoloDetector);

        new Thread(() -> {
            try {

                while (isObjectDetecting) {
                    // Check for hazards before attempting to land
                    boolean isHazardous = droneSafety.checkForHazards(current_image);

                    if (isHazardous) {
                        System.out.println("Landing aborted due to hazards.");
                        showToast("Landing aborted due to hazards.");
                        findOtherLandingSpot();
                    } else {
                        System.out.println("No hazards detected. Safe to land.");
                        showToast("No hazards detected. Safe to land.");
                        stopObjectDetectionAlgo();
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();

            }
        }).start();

        Log.d(TAG, "ended startPlaneDetectionAlgo");

    }

    private void findOtherLandingSpot() {
        // TODO
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
