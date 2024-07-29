package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.dji.sdk.sample.demo.kcgremotecontroller.VLD_PID;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ControllerImageDetection {

    private final int displayFps = 0;
    private final DataFromDrone dataFromDrone;
//    Double PP = 0.5, II = 0.02, DD = 0.01, MAX_I = 0.5;
    Double PP = 0.0, II = 0.02, DD = 0.01, MAX_I = 0.0;
    private int frameCounter = 0;
    private DepthMap depthMap;
    //    private final ALRemoteControllerView mainView;
    private long prevTime = System.currentTimeMillis();
    private boolean first_detect = true;
    private int not_found = 0;
    private boolean edgeDetectionMode = false;
    private CenterTracker centerTracker;
    private Mat previous_image;
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
    private float p, r, t, gp = 0;

    private double error_x, error_y, error_z, error_yaw, D;

    //constructor
    public ControllerImageDetection(DataFromDrone dataFromDrone, FlightControlMethods flightControlMethods, Context context) {
//        OpenCVLoader.initDebug();
        this.context = context;
//        this.mainView = mainView;
        this.dataFromDrone = dataFromDrone;
        this.flightControlMethods = flightControlMethods;
        this.depthMap = new DepthMap();

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


    // Method to run the Python script asynchronously

    public boolean checkPlain(Mat mat1) {

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(context));
        }

        byte[] mat1Bytes = matToBytes(mat1);
        byte[] mat2Bytes = matToBytes(previous_image);
        Python py = Python.getInstance();
        PyObject pyObj = py.getModule("DepthMap").callAttr("process_images", mat1Bytes, mat2Bytes);

        // Handle the result from Python (true/false)
        boolean isPlane = pyObj.toBoolean();
        Toast.makeText(context, "The ground is a plane: " + isPlane, Toast.LENGTH_LONG).show();

        return isPlane;
    }

    public Future<Boolean> checkPlainAsync(Mat mat1) {
        return executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return checkPlain(mat1);
            }
        });
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

//        ControlCommand command =
            proccessImage(bitmap, droneHeight);
        } catch (Exception exception) {
            Log.e("Error: ", Objects.requireNonNull(exception.getMessage()));
            showToast(Objects.requireNonNull(exception.getMessage()));
        }
    }

    public void stopEdgeDetection() {
        setEdgeDetectionMode(false);
        first_detect = true;
    }

    private byte[] matToBytes(Mat mat) {
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".png", mat, matOfByte);
        return matOfByte.toArray();
    }

    public void proccessImage(Bitmap frame, double droneHeight) {
        // Added python function but it causes an error in loading
        Mat imgToProcess = new Mat();
//        Mat prevImg = new Mat();
        Utils.bitmapToMat(frame, imgToProcess);

//        if (check_depth) {
//            // Create an empty matrix to store the grayscale image
////            Mat grayImage = new Mat();
//            Bitmap bitmap = null;
////            Mat colorImage = new Mat();
//            // Assuming the image is placed in res/drawable and is named image.jpg
//
//            if (frameCounter == 0) {
//                try {
//
//                    bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.img1);
//                    Utils.bitmapToMat(bitmap, prevImg);
//                    previous_image = prevImg;
//                    frameCounter++;
//
//                } catch (Exception e) {
//                    Log.e("Error: ", Objects.requireNonNull(e.getMessage()));
//                }
//            } else {
////                bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.img2);
//                bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.img2);
//                Utils.bitmapToMat(bitmap, prevImg);
//                try {
//                    Future<Boolean> futureResult = checkPlainAsync(prevImg);
//                    boolean bool = futureResult.get(); // Wait for the result
//                    Toast.makeText(context, "The ground is a plane: " + bool, Toast.LENGTH_LONG).show();
//                    Log.i("depthMap res:", "" + bool);
//
//                } catch (Exception e) {
//                    Log.e("Error: ", Objects.requireNonNull(e.getMessage()));
//                }
//            }
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

            flightControlMethods.sendVirtualStickCommands(command, 0.0f);
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

    public ControlCommand detectLending(Mat imgToProcess, double droneHeight) throws Exception {
        long currTime = System.currentTimeMillis();
        double dt = (currTime - prevTime) / 1000.0; // Give as the frame
        prevTime = currTime;
        double maxSpeed = 2;
//        setDescentRate((float) dataFromDrone.getAltitudeBelow());
        Point[][] point_arr = EdgeDetection.detectLines(imgToProcess);
        double slop;
        // Find the longest line
        Point[] detectLandLine = null;
        if (point_arr.length > 0) {
            first_detect = false;
            not_found = 0;
        } else if (!first_detect) {
            not_found++;
        }
        double droneRelativeHeight = dataFromDrone.getAltitudeBelow();
        boolean isUltrasonicBeingUsed = dataFromDrone.isUltrasonicBeingUsed();

        if (not_found > 5) {
            if (isUltrasonicBeingUsed && droneRelativeHeight <= 0.3) {
                showToast(":  Land!!!!");
                ControlCommand command = flightControlMethods.stayOnPlace();
//                ControlCommand command = flightControlMethods.land();
                return command;
//                return null;
                //TODO: check if it is possible to land here, and if so land.
            } else {
                throw new Exception("Error in detection mode, edge disappear");
            }

        }
        double dy = 0.0;
        for (Point[] points : point_arr) {
            if (points != null && points[0] != null && points[1] != null) {
                double x1 = points[0].x, y1 = points[0].y, x2 = points[1].x, y2 = points[1].y;

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
                double calc_dis = distancePointToLine(centerPoint, points);
                // Do we need the drone to know every time what the altitude according to the gps?
                Imgproc.putText(imgToProcess, "dy: " + calc_dis, centerPoint, 5, 1, new Scalar(0, 255, 0));
                dy = calc_dis;

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
                Log.i("Is?", "Is the line horizontal? " + droneHeight + " Calc_dis:  " + calc_dis);
            }

        }
        if (detectLandLine != null) {
            double x1 = detectLandLine[0].x, y1 = detectLandLine[0].y, x2 = detectLandLine[1].x, y2 = detectLandLine[1].y;
            Log.i("Detect", "Longest line: (" + x1 + ", " + y1 + ") to (" + x2 + ", " + y2 + ")");
            Imgproc.line(imgToProcess, detectLandLine[0], detectLandLine[1], new Scalar(255, 0, 0), 3, Imgproc.LINE_AA, 0);
        }
//        error_y = (imgToProcess.cols() / 2.0 - aruco.center.y) / 100;
        error_y = ((dy) / 100.0f);
//                + 0.5f; // Check


        p = (float) pitch_pid.update(error_y, dt, maxSpeed); // מעזכן את השגיאה בi
        //TODO maybe move the drone with yaw so the line will be horizontal completely to the drone
        // Not at the moment because it is left and right and we want it to go front and back
//        r = (float) roll_pid.update(error_x, dt, maxSpeed);
        t = -0.5f;
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
