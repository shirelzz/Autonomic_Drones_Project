package com.dji.sdk.sample.demo.accurateLandingController;

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
    private Handler handler = new Handler(Looper.getMainLooper());  // For updating the UI
    private Python python;
    private double focal_length = 4.8; // Field of View in mm
    private double sensor_width = 6.4;  // Sensor width in millimeters (approximate for 1/2" CMOS sensor)
    private ArrayList<Bitmap> imageList = new ArrayList<>();
    private PyObject depthMapClass;
    private PyObject getOutputFunc;
    private int frameCounter = 0;
    private DepthMap depthMap;
    private Map<String, Double> controlStatus = new HashMap<>();

    //    private final ALRemoteControllerView mainView;
    private long prevTime = System.currentTimeMillis();
    private boolean first_detect = true;
    private int not_found = 0;

    //    private double aspectRatio = 0;
//    private double VerticalFOV = 0;
    private boolean edgeDetectionMode = false;
    private CenterTracker centerTracker;
    private Mat previous_image = null;
    private Mat current_image = null;
    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private boolean check_depth;
    private FlightControlMethods flightControlMethods;
    private ObjectTracking objectTracking;
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
        this.depthMap = new DepthMap();
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

    public void setCurrentImage(Bitmap frame) {
        Mat newCurrentImg = new Mat();
        Utils.bitmapToMat(frame, newCurrentImg);

        current_image = newCurrentImg;
        if (current_image == null) {
            previous_image = newCurrentImg;
        } else {
            Mat temp = current_image;
            previous_image = temp;
        }

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
                    showToast("Land2!!!!");
//                    return flightControlMethods.land();
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
//            showToast("dyRealPitch:  " + dyRealPitch);
            ControlCommand command = buildControlCommand(dyRealPitch, dt, imgToProcess);
            updateLog(command, pointArr.size(), bestLine, dyRealPitch);

            return command;

        } else {
            return null; // No line detected or valid
        }

//        return buildControlCommand(dyReal, dt);
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

    private ControlCommand buildControlCommand(double dyReal, double dt, Mat imgToProcess) {
        double maxSpeed = 2.0;
        double Kp = 0.02;  // Proportional gain, adjust as needed - 0.01
        error_y = dyReal * Kp;


        if (0 <= error_y && error_y <= 0.001) {
//            if (gimbalController.getPrevDegree() == -90) {
//                return flightControlMethods.land();
//            }
            showToast("Landing!!");
//            float gd = gimbalController.getPrevDegree() - 10;
//            gd = gd < -90 ? -90 : gd;
//            gimbalController.rotateGimbalToDegree(gd);

            t = -0.05f;
            error_y = 0.05f;

//////            p = 0f;
//            return stayOnPlace();

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

    // Method to start video playback
    // display depthmap python

    public void startDepthMapVideo() {

        Log.d(TAG, "entered startDepthMapVideo");

        isPlaying = true;

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(context)); // 'this' is the Context here
        }

        python = Python.getInstance();


//        depthMapClass = python.getModule("DepthMap_");
//        getOutputFunc = depthMapClass.get("computeDepthMapSGBM");
        depthMapClass = python.getModule("PlaneHandler");
        getOutputFunc = depthMapClass.get("start_detect");

        new Thread(() -> {
            try {

                while (isPlaying) {

                    if (getOutputFunc != null) {

                        if (current_image == null) {
                            Log.d(TAG, "current_image is null");
                            return;
                        }

                        if (previous_image == null) {
                            Log.d(TAG, "previous_image is null");
                            return;
                        }

                        byte[] previousImageBytes = matToBytes(previous_image);
                        byte[] currentImageBytes = matToBytes(current_image);
                        double altitude = dataFromDrone.isUltrasonicBeingUsed() ? dataFromDrone.getAltitudeBelow() : dataFromDrone.getGPS().getAltitude();

                        // Call Python function with the byte arrays
                        PyObject result = getOutputFunc.call(PyObject.fromJava(previousImageBytes), PyObject.fromJava(currentImageBytes), altitude);
                        List<Object> javaList = new ArrayList<>();
                        try {
                            PyObject imageBytesObj = result.asList().get(0);
                            PyObject positionsObj = result.asList().get(1);

                            String imageBytesBase64 = imageBytesObj.toString();
                            List<PyObject> positionsPyList = positionsObj.asList();

                            for (PyObject item : positionsPyList) {
                                javaList.add(item.toJava(Object.class));  // Convert each Python object to a Java object
                            }
                            // Decode Base64 to byte array
                            byte[] imageBytes = Base64.decode(imageBytesBase64, Base64.DEFAULT);

                            // Convert byte array to Bitmap
                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                            Log.d(TAG, "Convert byte array to Bitmap: " + bitmap.toString());

                            handler.post(() -> {
                                imageView.setImageBitmap(bitmap);  // Update the ImageView with the new frame
                                Log.d(TAG, "imageView updated");
                            });
                        } catch (ClassCastException e) {
                            throw new RuntimeException("Error processing Python result", e);
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

    // Method to stop video playback
    public void stopDepthMapVideo() {
        isPlaying = false;

        // Release Python resources
        if (depthMapClass != null) {
            try {
                depthMapClass.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing Python module", e);
            }
            depthMapClass = null;
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

}
