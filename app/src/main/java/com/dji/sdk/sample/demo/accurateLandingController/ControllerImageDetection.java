package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

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

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ControllerImageDetection {

    private final int displayFps = 0;
    private final DataFromDrone dataFromDrone;
    //    Double PP = 0.5, II = 0.02, DD = 0.01, MAX_I = 0.5;
    Double PP = 0.5, II = 0.02, DD = 0.01, MAX_I = 0.5;
    private int frameCounter = 0;
    private DepthMap depthMap;

    //    private final ALRemoteControllerView mainView;
    private long prevTime = System.currentTimeMillis();
    private boolean first_detect = true;
    private int not_found = 0;
    private boolean edgeDetectionMode = false;
    private boolean inCheckImageMode = false;
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
    public ControllerImageDetection(DataFromDrone dataFromDrone, FlightControlMethods flightControlMethods,
                                    Context context
//            , RecordingVideo recordingVideo
    ) {
        this.context = context;
//        this.mainView = mainView;
        this.dataFromDrone = dataFromDrone;
        this.flightControlMethods = flightControlMethods;
        this.depthMap = new DepthMap();
//        this.recordingVideo = recordingVideo;

        //Do we need it
//        this.frameWidth = frameWidth;
//        this.frameHeight = frameHeight;

//        this.objectTracking = new ObjectTracking(true, "GREEN");
//        centerTracker = new CenterTracker();
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

    // Method to run the Python script asynchronously

    public boolean checkPlain(Mat mat1) {

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(context));
        }

        byte[] mat1Bytes = matToBytes(mat1);
        byte[] mat2Bytes = matToBytes(previous_image);
        Python py = Python.getInstance();
        PyObject pyObj = py.getModule("DepthMap").get("process_images");

        // Handle the result from Python (true/false)
        assert pyObj != null;
        PyObject isPlane = pyObj.call(mat1Bytes, mat2Bytes);
        Toast.makeText(context, "The ground is a plane: " + isPlane.toString(), Toast.LENGTH_LONG).show();

//        return isPlane;
        return isPlane.toBoolean();
    }

//    public Future<Boolean> checkPlainAsync(Mat mat1) {
//        return executorService.submit(new Callable<Boolean>() {
//            @Override
//            public Boolean call() throws Exception {
//                return checkPlain(mat1);
//            }
//        });
//    }

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

//    private byte[] convertBitmapToByteArray(Bitmap bitmap) {
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
//        return byteArrayOutputStream.toByteArray();
//    }

    public void stopEdgeDetection() {
        setEdgeDetectionMode(false);
        first_detect = true;
    }

    private byte[] matToBytes(Mat mat) {
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".png", mat, matOfByte);
        return matOfByte.toArray();
    }

    public void processImage(Bitmap frame, double droneHeight) {
        // Added python function but it causes an error in loading
        Mat imgToProcess = new Mat();
        Mat prevImg = new Mat();
        Utils.bitmapToMat(frame, imgToProcess);
        current_image = imgToProcess;
//        if (check_depth) {
//            // Create an empty matrix to store the grayscale image
////            Mat grayImage = new Mat();
        Bitmap bitmap = null;
////            Mat colorImage = new Mat();
//            // Assuming the image is placed in res/drawable and is named image.jpg
//
//        if (frameCounter == 0) {
//            try {
//
//                bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.img1);
//                Utils.bitmapToMat(bitmap, prevImg);
//                previous_image = prevImg;
//                frameCounter++;
//
//            } catch (Exception e) {
//                Log.e("Error: ", Objects.requireNonNull(e.getMessage()));
//            }
//        } else {
////                bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.img2);
//            bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.img2);
//            Utils.bitmapToMat(bitmap, prevImg);
//            try {
////                    Future<Boolean> futureResult = checkPlainAsync(prevImg);
//                boolean bool = checkPlain(prevImg);
////                    boolean bool = futureResult.get(); // Wait for the result
//                Toast.makeText(context, "The ground is a plane: " + bool, Toast.LENGTH_LONG).show();
//                Log.i("depthMap res:", "" + bool);
//
//            } catch (Exception e) {
//                Log.e("Error: ", Objects.requireNonNull(e.getMessage()));
//                Toast.makeText(context, Objects.requireNonNull(e.getMessage()), Toast.LENGTH_LONG).show();
//
//            }
//        }
//////          Convert Bitmap to Mat
////            assert bitmap != null;
////            Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC3);
////            Utils.bitmapToMat(bitmap, mat);
////
////            // Example: Convert to grayscale
////            Mat grayMat = new Mat();
////            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY);
////            frameCounter++;
//
//            // Convert the image to grayscale
////            Imgproc.cvtColor(imgToProcess, grayImage, Imgproc.COLOR_BGR2GRAY);
////            Imgproc.cvtColor(colorImage, grayImage, Imgproc.COLOR_BGR2GRAY);
//
////            Boolean bool = depthMap.AddImage(grayMat);
////            showToast("depthMap:  " + bool);
//            this.check_depth = false;
//        }
//        if (edgeDetectionMode) {
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

//    public ControlCommand checkImage(Mat image) {
//        if (previous_image == null || previous_image.empty()) {
//            previous_image = image;
//            inCheckImageMode = true;
//            long currTime = System.currentTimeMillis();
//            double dt = (currTime - prevTime) / 1000.0; // Give as the frame
//            prevTime = currTime;
//            double maxSpeed = 2;
//            p = (float) pitch_pid.update(-0.2, dt, maxSpeed); // מעזכן את השגיאה בi
//            ControlCommand ans = new ControlCommand(p, r, t);
//            double droneRelativeHeight = dataFromDrone.getAltitudeBelow();
//            ans.setErr(1000, error_x, error_y, droneRelativeHeight);
//            ans.setPID(throttle_pid.getP(), throttle_pid.getI(), throttle_pid.getD(), pitch_pid.getP(), pitch_pid.getI(), pitch_pid.getD(), roll_pid.getP(), roll_pid.getI(), roll_pid.getD(), roll_pid.getMax_i());
//
//            return ans;
//
////            flightControlMethods.sendVirtualStickCommands(command, 0.0f);
//
//        }
//        return null;
////        return edgeDetectionMode;
//    }

    public ControlCommand detectLending(Mat imgToProcess, double droneHeight) throws Exception {

        long currTime = System.currentTimeMillis();
        double dt = (currTime - prevTime) / 1000.0; // Give as the frame
        prevTime = currTime;

         /*
            if no edge (not viewing any edge) - stay in place (Error)

            if seeing edge: get the edge to be in the middle and lower the drone, move the
            drone to 0.2 lower and 0.2 forward.

            if distance drone from ground is 0.3 or lower - land.

         */

        double maxSpeed = 0.5;
//        setDescentRate((float) dataFromDrone.getAltitudeBelow());
        Point[][] point_arr = EdgeDetection.detectLines(imgToProcess);
        double slop;

        if (point_arr.length > 0) {
            first_detect = false;
            not_found = 0;
        } else if (!first_detect) {
            not_found++;
        } else {
            throw new Exception("Error in detection mode, edge disappear");
        }
        double droneRelativeHeight = dataFromDrone.getAltitudeBelow();
        boolean isUltrasonicBeingUsed = dataFromDrone.isUltrasonicBeingUsed();
        //Switch between not found and related to ground
        if (not_found > 5) {
            if (isUltrasonicBeingUsed && droneRelativeHeight <= 0.4) {
                showToast(":  Land!!!!");

                return flightControlMethods.land();
            } else {
                throw new Exception("Error in detection mode, edge disappear");
            }

        }
        Point[] bestHorizontalLine = null;
        Point[] bestLine = null;

        double dy_best_horizontal = Double.MAX_VALUE;
        double dy_best = Double.MAX_VALUE;
        Point centerPoint = new Point(imgToProcess.cols() / 2.0, imgToProcess.rows() / 2.0);
        double dy;
        for (Point[] point : point_arr) {
            if (point != null && point[0] != null && point[1] != null) {
                Imgproc.line(imgToProcess, point[0], point[1], new Scalar(0, 255, 0), 3);

                double angle = Math.atan2(point[1].y - point[0].y, point[1].x - point[0].x);
                angle = Math.abs(angle);
                slop = Math.tan(angle);

                double c_x = (point[0].x + point[1].x) / 2.0;
                double c_y = (point[0].y + point[1].y) / 2.0;

                dy = imgToProcess.height() / 2.0 - c_y;

                if (slop <= Math.PI / 32) {
                    if (Math.abs(dy) < dy_best_horizontal) {
                        bestHorizontalLine = point;
                        dy_best_horizontal = Math.abs(dy);
                    }
                } else {
                    if (Math.abs(dy) < dy_best) {
                        bestLine = point;
                        dy_best = Math.abs(dy);
                    }
                }

                Log.i("Line Info", "Is the line horizontal? " + angle + " Calc_dis: " + dy);
            }
        }
//        if(bestLine)
        Point[] finalLine = bestHorizontalLine != null ? bestHorizontalLine : bestLine;
        Point p1 = finalLine[0];
        Point p2 = finalLine[1];
        Imgproc.line(imgToProcess, p1, p2, new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);

        // Calculate pitch adjustment based on line displacement
        double focalLength = 3.6; // In mm
        double sensorHeight = 2.76; // In mm
        double frameHeightPx = imgToProcess.height();

        Imgproc.putText(imgToProcess, "dy: " + dy_best, centerPoint, 5, 1, new Scalar(0, 255, 0));
        Imgproc.putText(imgToProcess, "px: " + p1, new Point(centerPoint.x, centerPoint.y - 20), 5, 1, new Scalar(0, 255, 0));
        Imgproc.putText(imgToProcess, "py: " + p2, new Point(centerPoint.x, centerPoint.y - 40), 5, 1, new Scalar(0, 255, 0));
        double deltaY = (frameHeightPx / 2.0) - ((p1.y + p2.y) / 2.0);
        double deltaHeightMm = (deltaY / frameHeightPx) * sensorHeight;
        double pitchAdjustment = Math.atan(deltaHeightMm / focalLength);

        // Use PID controllers to calculate the necessary pitch and roll
        double c_x = (p1.x + p2.x) / 2.0;
        double c_y = (p1.y + p2.y) / 2.0;

        double imageWidth = imgToProcess.width();
        double imageHeight = imgToProcess.height();

        double error_x = c_x - imageWidth / 2.0;
        double error_y = c_y - imageHeight / 2.0;

        // Adjust pitch_cmd calculation
//        error_y = (imgToProcess.cols() / 2.0 - aruco.center.y) / 100;
        if (dy_best != 0) {
            t = 0;
        } else {
            error_y = 0.2f;
            t = -0.2f; // 0.2 is for moving the drone forward like we do with throttle.
        }

        r = (float) roll_pid.update(0, error_x);
        p = (float) pitch_pid.update(pitchAdjustment, error_y);

        p = (float) Math.min(maxSpeed, Math.max(-maxSpeed, p));
        r = (float) Math.min(maxSpeed, Math.max(-maxSpeed, r));

        ControlCommand ans = new ControlCommand(p, r, t);
        ans.setErr(1000, error_x, error_y, droneRelativeHeight);
        ans.setPID(throttle_pid.getP(), throttle_pid.getI(), throttle_pid.getD(), pitch_pid.getP(), pitch_pid.getI(), pitch_pid.getD(), roll_pid.getP(), roll_pid.getI(), roll_pid.getD(), roll_pid.getMax_i());
//        ans.setImageDistance(aruco.approximateDistance());
        return ans;
    }

    private double distancePointToLine(final Point point, final Point[] line) {
        final Point l1 = line[0];
        final Point l2 = line[1];
        double crossProduct = (l2.x - l1.x) * (l1.y - point.y) - (l1.x - point.x) * (l2.y - l1.y);
        double distance = Math.abs(crossProduct) / Math.sqrt(Math.pow(l2.x - l1.x, 2) + Math.pow(l2.y - l1.y, 2));
        return crossProduct > 0 ? -distance : distance;
    }

}
