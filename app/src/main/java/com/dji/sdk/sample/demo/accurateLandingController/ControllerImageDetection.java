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
import com.dji.sdk.sample.demo.kcgremotecontroller.Controller;
import com.dji.sdk.sample.demo.kcgremotecontroller.VLD_PID;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
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
    private double horizontalFOV = 84.0;
    //
    private ArrayList<Bitmap> imageList = new ArrayList<>();
    private PyObject depthMapClass;
    private PyObject getOutputFunc;
    private int frameCounter = 0;
    private DepthMap depthMap;

    //    private final ALRemoteControllerView mainView;
    private long prevTime = System.currentTimeMillis();
    private boolean first_detect = true;
    private int not_found = 0;

    private double aspectRatio = 0;
    private double VerticalFOV = 0;
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
    private Controller controller;

    private float lastP = 0, lastR = 0; // previous error

    private float p, r, t, gp = 0;

    private double error_x, error_y, error_z, error_yaw, D;
    private Queue<Double> errorQx = new LinkedList<Double>();
    private Queue<Double> errorQy = new LinkedList<Double>();
    private Queue<Double> errorQz = new LinkedList<Double>();

    //    private RecordingVideo recordingVideo = null;
    //constructor
    public ControllerImageDetection(DataFromDrone dataFromDrone, FlightControlMethods flightControlMethods, Context context, ImageView imageView
//            , RecordingVideo recordingVideo
    ) {
        this.context = context;
//        this.mainView = mainView;
        this.dataFromDrone = dataFromDrone;
        this.flightControlMethods = flightControlMethods;
        this.depthMap = new DepthMap();
//        this.recordingVideo = recordingVideo;
        this.imageView = imageView;  // Initialize the ImageView

        //Do we need it
        this.frameWidth = 640;
        this.frameHeight = 480;

//        this.objectTracking = new ObjectTracking(true, "GREEN");
//        centerTracker = new CenterTracker();
    }

    private double calculateVerticalFOV(double horizontalFOV, double aspectRatio) {
        double halfHorizontalFOV = Math.toRadians(horizontalFOV / 2);
        double verticalFOV = 2 * Math.atan(Math.tan(halfHorizontalFOV) / aspectRatio);
        return Math.toDegrees(verticalFOV);
    }

    private double calculatePitchAdjustment(int dyPixels, int imageHeight) {
        // Calculate the angular offset needed
        double thetaOffset = ((double) dyPixels / imageHeight) * this.VerticalFOV;
        return thetaOffset; // This is the pitch adjustment needed in degrees
    }

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
            if (aspectRatio == 0) {
                aspectRatio = calculateAspectRatio(bitmap.getWidth(), bitmap.getHeight());
                VerticalFOV = calculateVerticalFOV(horizontalFOV, aspectRatio);
            }
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

    public ControlCommand detectLending(Mat imgToProcess, double droneHeight) throws Exception {

        long currTime = System.currentTimeMillis();
        double dt = (currTime - prevTime) / 1000.0; // Calculate time difference
        prevTime = currTime;

        // Initialize variables for edge detection and pitch/roll adjustment
        Point[][] pointArr = EdgeDetection.detectLines(imgToProcess);
        double droneRelativeHeight = dataFromDrone.getAltitudeBelow();
        boolean isUltrasonicBeingUsed = dataFromDrone.isUltrasonicBeingUsed();

        if (pointArr.length > 0) {
            first_detect = false;
            not_found = 0;
        } else if (!first_detect) {
            not_found++;
            if (not_found > 5) {
                if (isUltrasonicBeingUsed && droneRelativeHeight <= 0.4) {
                    showToast("Land!!!!");
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
            double dyReal = adjustDronePosition(bestLine, imgToProcess.height(), droneRelativeHeight);
            showToast("dyReal:  " + dyReal);
            return buildControlCommand(dyReal, dt, imgToProcess);

        } else {
            return null; // No line detected or valid
        }

//        return buildControlCommand(dyReal, dt);
    }

    private Point[] selectBestLine(Point[][] pointArr, Mat imgToProcess) {
        Point[] bestHorizontalLine = null;
        Point[] bestLine = null;
        double dy_best_horizontal = Double.MAX_VALUE;
        double dy_best = Double.MAX_VALUE;

        for (Point[] point : pointArr) {
            if (point != null && point[0] != null && point[1] != null) {
                double angle = Math.atan2(point[1].y - point[0].y, point[1].x - point[0].x);
                double slope = Math.tan(Math.abs(angle));

                double c_y = (point[0].y + point[1].y) / 2.0;
                double dy = imgToProcess.height() / 2.0 - c_y;

                if (slope <= Math.PI / 32 && Math.abs(dy) < dy_best_horizontal) {
                    bestHorizontalLine = point;
                    dy_best_horizontal = Math.abs(dy);
                } else if (Math.abs(dy) < dy_best) {
                    bestLine = point;
                    dy_best = Math.abs(dy);
                }
            }
        }

        return bestHorizontalLine != null ? bestHorizontalLine : bestLine;
    }

    private double adjustDronePosition(Point[] finalLine, double frameHeightPx, double droneRelativeHeight) {
//        double fovVerticalRadians = Math.toRadians(horizontalFOV);
        double fovVerticalRadians = Math.toRadians(VerticalFOV);
        double dy_best = frameHeightPx / 2.0 - (finalLine[0].y + finalLine[1].y) / 2.0;

        return 2 * droneRelativeHeight * Math.tan(fovVerticalRadians / 2) * (dy_best / frameHeightPx);
    }

    private ControlCommand buildControlCommand(double dyReal, double dt, Mat imgToProcess) {
        double maxSpeed = 2.0;
        p = (float) pitch_pid.update(dyReal / 100f, dt, maxSpeed);
        r = (float) roll_pid.update(0, dt, maxSpeed);

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

                        // Call Python function with the byte arrays
                        PyObject result = getOutputFunc.call(PyObject.fromJava(previousImageBytes), PyObject.fromJava(currentImageBytes));
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

    public double calculateAspectRatio(int width, int height) {
        return (double) width / height;
    }

    // Convert Mat to byte array
    public byte[] matToBytes(Mat mat) {
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".png", mat, matOfByte);
        return matOfByte.toArray();
    }

}
