package com.dji.sdk.sample.demo.accurateLandingController;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.tracking.TrackerCSRT;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Tracker;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class DayMode {
    private List<Double> stat;
    private List<Integer> avgContours;
    private Point tempMaxLoc;
    private BackgroundSubtractorMOG2 backSub;
    private Tracker tracker;
    private boolean targetFlag;
    private Mat frame;
    private Rect bbox;
    private String lastMode;
    private Point position;
    private boolean colorDetection;
    private String color;
    private int cancelMsg;
    private int modeMsg;

    private Scalar redLower;
    private Scalar redUpper;
    private Scalar yellowLower;
    private Scalar yellowUpper;
    private Mat kernel;

    public DayMode(boolean colorDetection, String color) {
        this.stat = new ArrayList<>();
        this.avgContours = new ArrayList<>();
        this.tempMaxLoc = new Point(0, 0);
        this.backSub = Video.createBackgroundSubtractorMOG2(20, 50, true);
        this.backSub.setVarInit(8);
        this.tracker = TrackerCSRT.create();
        this.targetFlag = false;
        this.frame = new Mat();
        this.bbox = new Rect();
        this.lastMode = null;
        this.position = new Point();
        this.colorDetection = colorDetection;
        this.color = color;
        this.cancelMsg = 0;
        this.modeMsg = 0;

        // Set range for red color
        this.redLower = new Scalar(136, 87, 111);
        this.redUpper = new Scalar(180, 255, 255);

        // Set range for yellow color
        this.yellowLower = new Scalar(16, 175, 237);
        this.yellowUpper = new Scalar(65, 255, 255);

        // Morphological Transform, Dilation for each color and bitwise_and operator
        // between imageFrame and mask determines to detect only that particular color
        this.kernel = Mat.ones(new Size(5, 5), CvType.CV_8U);
    }

    public Point dayAction(Mat fr, int state) {
        this.frame = fr;

        if (this.skyModeCheck()) {
            Imgproc.putText(this.frame, "Sky Mode", new Point(5, 20), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 0, 0));
            this.position = this.skyMode(state);
        } else if (this.colorDetection) {
            Imgproc.putText(this.frame, "Ground Mode", new Point(5, 20), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 0, 0));
            this.position = this.groundModeByColor(state);
        } else { // ground without color mode
            Imgproc.putText(this.frame, "Ground Mode", new Point(5, 20), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 0, 0));
            this.position = this.groundMode(state);
        }

        return this.position;
    }

    public boolean skyModeCheck() {
        Mat hsv = new Mat();
        Imgproc.cvtColor(this.frame, hsv, Imgproc.COLOR_BGR2HSV);

        // Set range for blue color for moving to sky mode
        Scalar lowerBlue = new Scalar(0, 0, 0);
        Scalar upperBlue = new Scalar(179, 255, 155);

        Mat mask = new Mat();
        Core.inRange(hsv, lowerBlue, upperBlue, mask);
        double mean = Core.mean(mask).val[0];
        return mean < 20.0;
    }

    public Point skyMode(int state) {
        if (this.modeMsg == 0) {
            this.modeMsg = 1;
            System.out.println("--> Sky Mode");
        }

        if ("ground".equals(this.lastMode) && this.position.x != -1 && this.position.y != -1) {
            Rect box = new Rect(this.bbox.x, this.bbox.y, (this.bbox.width - this.bbox.x), (this.bbox.height - this.bbox.y));
            this.tracker.init(this.frame, box);
            this.targetFlag = true;
            this.lastMode = "sky";
            System.out.println("--> Sky Mode");
            return new Point(this.bbox.x + this.bbox.width / 2, this.bbox.y + this.bbox.height / 2);
        }

        this.lastMode = "sky";

        Mat fgMask = new Mat();
        this.backSub.apply(this.frame, fgMask);
        Mat gray = new Mat();
        Imgproc.GaussianBlur(fgMask, gray, new Size(7, 7), 0);
        Core.MinMaxLocResult mmr = Core.minMaxLoc(gray);
        double locMean = (mmr.maxLoc.x + mmr.maxLoc.y) / 2;
        this.stat.add(locMean);
        double x = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            x = this.stat.stream().mapToDouble(val -> val).average().orElse(0.0);
        }
        double getStat = this.statisticallyTarget();

        if (this.stat.size() == 100) {
            this.stat.remove(0);
        }

        if (this.avgContours.size() == 100) {
            this.avgContours.remove(0);
        }

        double measure = Math.abs(x - mmr.maxLoc.x);

        if (measure <= getStat) {
            Rect box = new Rect();
            boolean success = this.tracker.update(this.frame, box);
            Point position = null;
            if (this.targetFlag && success) {
                this.bbox = new Rect(box.x, box.y, box.width, box.height);
                position = new Point(box.x + box.width / 2.0, box.y + box.height / 2.0);
            } else {
                if (mmr.maxLoc.x != 0 && mmr.maxLoc.y != 0) {
                    Imgproc.rectangle(this.frame, new Point(mmr.maxLoc.x - 20, mmr.maxLoc.y - 20),
                            new Point(mmr.maxLoc.x + 20, mmr.maxLoc.y + 20), new Scalar(0, 0, 255), 3);
                }
                this.tempMaxLoc = mmr.maxLoc;
            }

            if (state == 67 || state == 99 || !success) {
                this.tracker = TrackerCSRT.create();
                this.targetFlag = false;
                if (this.cancelMsg == 1) {
                    this.cancelMsg = 2;
                    System.out.println("\t\tLost contact at " + new Date().toString());
                }
            }

            if (state == 32) {
                Rect trackBox = new Rect((int) (mmr.maxLoc.x - 20), (int) (mmr.maxLoc.y - 20), 40, 40);
                this.tracker.init(this.frame, trackBox);
                this.targetFlag = true;
                this.cancelMsg = 1;
                System.out.println("\t\tTracking a new target at " + new Date().toString());
            }

            if (position != null) {
                return position;
            }
        }

        return new Point(-1, -1);
    }

    public double statisticallyTarget() {
        double average = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            average = this.avgContours.stream().mapToInt(val -> val).average().orElse(0.0);
        }
        if (average >= 1000) {
            return 100;
        } else if (average >= 700) {
            return 300;
        } else if (average >= 500) {
            return 500;
        } else if (average >= 10) {
            return 700;
        } else {
            return 700;
        }
    }

    public Point groundModeByColor(int state) {
        if (this.modeMsg == 0) {
            this.modeMsg = 1;
            System.out.println("--> Ground Mode");
        }

        if ("sky".equals(this.lastMode) && this.position.x != -1 && this.position.y != -1) {
            Rect box = new Rect(this.bbox.x, this.bbox.y, (this.bbox.width - this.bbox.x), (this.bbox.height - this.bbox.y));
            this.tracker.init(this.frame, box);
            this.targetFlag = true;
            this.lastMode = "ground";
            System.out.println("--> Ground Mode");
            return new Point(this.bbox.x + this.bbox.width / 2, this.bbox.y + this.bbox.height / 2);
        }

        this.lastMode = "ground";
        Mat hsvFrame = new Mat();
        Imgproc.cvtColor(this.frame, hsvFrame, Imgproc.COLOR_BGR2HSV);

        Mat mask = new Mat();
        if (this.color == null || "RED".equals(this.color) || "ORANGE".equals(this.color)) {
            Core.inRange(hsvFrame, this.redLower, this.redUpper, mask);
        } else if ("YELLOW".equals(this.color)) {
            Core.inRange(hsvFrame, this.yellowLower, this.yellowUpper, mask);
        }

        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, this.kernel);
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, this.kernel);

        Mat edges = new Mat();
        Imgproc.Canny(mask, edges, 100, 200);
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        this.avgContours.add(contours.size());

        Mat contourFrame = new Mat();
        if (!contours.isEmpty()) {
            List<MatOfPoint> sortedContours = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                sortedContours = contours.stream()
                        .sorted(Comparator.comparingDouble(c -> Imgproc.contourArea(c)))
                        .collect(Collectors.toList());
            }

            Imgproc.drawContours(contourFrame, sortedContours, -1, new Scalar(0, 255, 0), 2);

            MatOfPoint largestContour = sortedContours.get(0);
            Rect boundingBox = Imgproc.boundingRect(largestContour);

            Imgproc.rectangle(contourFrame, boundingBox, new Scalar(0, 255, 0), 2);
            double avgX = boundingBox.x + boundingBox.width / 2.0;
            double avgY = boundingBox.y + boundingBox.height / 2.0;
            return new Point(avgX, avgY);
        }

        return new Point(-1, -1);
    }

    public Point groundMode(int state) {
        if (this.modeMsg == 0) {
            this.modeMsg = 1;
            System.out.println("--> Ground Mode");
        }

        if ("sky".equals(this.lastMode) && this.position.x != -1 && this.position.y != -1) {
            Rect box = new Rect(this.bbox.x, this.bbox.y, (this.bbox.width - this.bbox.x), (this.bbox.height - this.bbox.y));
            this.tracker.init(this.frame, box);
            this.targetFlag = true;
            this.lastMode = "ground";
            System.out.println("--> Ground Mode");
            return new Point(this.bbox.x + this.bbox.width / 2, this.bbox.y + this.bbox.height / 2);
        }

        this.lastMode = "ground";

        Mat fgMask = new Mat();
        this.backSub.apply(this.frame, fgMask);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(fgMask, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        this.avgContours.add(contours.size());

        Mat contourFrame = new Mat();
        if (!contours.isEmpty()) {
            List<MatOfPoint> sortedContours = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                sortedContours = contours.stream()
                        .sorted(Comparator.comparingDouble(c -> Imgproc.contourArea(c)))
                        .collect(Collectors.toList());
            }

            Imgproc.drawContours(contourFrame, sortedContours, -1, new Scalar(0, 255, 0), 2);

            MatOfPoint largestContour = sortedContours.get(0);
            Rect boundingBox = Imgproc.boundingRect(largestContour);

            Imgproc.rectangle(contourFrame, boundingBox, new Scalar(0, 255, 0), 2);
            double avgX = boundingBox.x + boundingBox.width / 2.0;
            double avgY = boundingBox.y + boundingBox.height / 2.0;
            return new Point(avgX, avgY);
        }

        return new Point(-1, -1);
    }

    public Rect getBox() {
        return this.bbox;
    }

//    public static void main(String[] args) {
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//
//        DayMode dayMode = new DayMode(true, "RED");
//        VideoCapture capture = new VideoCapture(0);
//        if (!capture.isOpened()) {
//            System.out.println("Error opening video stream or file");
//            return;
//        }
//
//        Mat frame = new Mat();
//        while (true) {
//            capture.read(frame);
//            if (frame.empty()) {
//                break;
//            }
//
//            int state = 0; // Update this based on actual input/state management
//            Point position = dayMode.dayAction(frame, state);
//
//            // Display the resulting frame
//            // HighGui.imshow("Frame", frame); // Uncomment if you have HighGui available
//            // HighGui.waitKey(30); // Uncomment if you have HighGui available
//        }
//
//        capture.release();
//        // HighGui.destroyAllWindows(); // Uncomment if you have HighGui available
//    }
}
