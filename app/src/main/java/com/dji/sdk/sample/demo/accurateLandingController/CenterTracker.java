package com.dji.sdk.sample.demo.accurateLandingController;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.tracking.TrackerCSRT;
import org.opencv.tracking.TrackerKCF;
import org.opencv.tracking.Tracking;

import org.opencv.video.Tracker;

public class CenterTracker {
    private static final double PITCH_SCALING_FACTOR = 0.01;
    private static final double ROLL_SCALING_FACTOR = 0.01;
    private boolean gotFirstImage;
    private Rect currentRect;
    private Drawing mDrawing = Drawing.DRAWING;
    private boolean isPaused;
    private String alg = "TrackerKCF";
    private double initialLatitude;
    private double initialLongitude;
    private int size;
    protected Tracker mTracker;
    private Mat mImageGrab;

    public CenterTracker(int size, String alg) {
        this.size = size;
        this.alg = alg;
    }


    public CenterTracker() {
        size = 50;
        alg = "TrackerKCF";
    }

    public Mat init(Mat frame) {

//        switch (alg) {
//            case "TrackerCSRT":
//                mTracker = TrackerCSRT.create();
//                break;
//            case "TrackerKCF":
//                mTracker = TrackerKCF.create();
//                break;
//        }
        mTracker = (Tracker) TrackerKCF.create();

        currentRect = selectROI(frame);

        mTracker.init(frame, currentRect);
//        currentRect = mInitRectangle;

        Tracking t = new Tracking();

//        mDrawing = Drawing.TRACKING;
//
//        //TODO: DEBUG
//        Rect testRect = new org.opencv.core.Rect(currentRect.x, currentRect.y, currentRect.width, currentRect.height);
//        Mat roi = new Mat(mImageGrab, testRect);
//        Bitmap bmp = null;
//        Mat tmp = new Mat (roi.rows(), roi.cols(), CvType.CV_8U, new Scalar(4));
//        try {
//            Imgproc.cvtColor(roi, tmp, Imgproc.COLOR_RGB2BGRA);
//            bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(tmp, bmp);
//        }
//        catch (CvException e){
//            Log.d("Exception",e.getMessage());
//        }



        // Select ROI
//        if (mInitRectangle == null) {
//            System.out.println("Error: Could not select ROI");
//            return;
//        }

//            int x = currentRect.x;
//            int y = currentRect.y;
//            int w = currentRect.width;
//            int h = currentRect.height;
//
//            Mat roi = new Mat(frame, currentRect);
//            Mat hsv_roi = new Mat();
//            Imgproc.cvtColor(roi, hsv_roi, Imgproc.COLOR_BGR2HSV);
//            Mat mask = new Mat();
//            Core.inRange(hsv_roi, new Scalar(0, 60, 32), new Scalar(180, 255, 255), mask);
//
//            Mat roi_hist = new Mat();
//            Imgproc.calcHist(Arrays.asList(hsv_roi), new MatOfInt(0), mask, roi_hist, new MatOfInt(180), new MatOfFloat(0, 180));
//            Core.normalize(roi_hist, roi_hist, 0, 255, Core.NORM_MINMAX);
//
//            TermCriteria term_crit = new TermCriteria(TermCriteria.EPS | TermCriteria.COUNT, 10, 1);

        return null;
    }

    public double[] process(Mat frame) {
        if (!gotFirstImage) {
            init(frame);
            gotFirstImage = true;
            return new double[]{0, 0};
        }


        int x = currentRect.x;
        int y = currentRect.y;
        int w = currentRect.width;
        int h = currentRect.height;

//        Mat roi = new Mat(frame, currentRect);
//        Mat hsv_roi = new Mat();
//        Imgproc.cvtColor(roi, hsv_roi, Imgproc.COLOR_BGR2HSV);
//        Mat mask = new Mat();
//        Core.inRange(hsv_roi, new Scalar(0, 60, 32), new Scalar(180, 255, 255), mask);
//
//        Mat roi_hist = new Mat();
//        Imgproc.calcHist(Arrays.asList(hsv_roi), new MatOfInt(0), mask, roi_hist, new MatOfInt(180), new MatOfFloat(0, 180));
//        Core.normalize(roi_hist, roi_hist, 0, 255, Core.NORM_MINMAX);
//
//        TermCriteria term_crit = new TermCriteria(TermCriteria.EPS | TermCriteria.COUNT, 10, 1);

        // Update the tracking result
        Point p2 = new Point(currentRect.x + currentRect.width, currentRect.y + currentRect.height);
        boolean ok = mTracker.update(frame, currentRect);
        Imgproc.rectangle(
                frame,                          // Matrix obj of the image
                new Point(currentRect.x, currentRect.y),        // p1
                p2, // p2
                new Scalar(255, 0, 0),          // Rect color
                2                               // Thickness of the rectangle lines
        );
        // If tracking was successful, roi contains the new bounding box of the tracked object,
//        if (ok) {
            double dx = (currentRect.x + currentRect.width / 2.0 - frame.cols() / 2.0);
            double dy = (currentRect.y + currentRect.height / 2.0 - frame.rows() / 2.0);
            return new double[]{dx, dy};

//        } else {
//            System.out.println("Tracking failure detected");
//        }


//            while (true) {
////                cap.read(frame);
//                if (frame.empty()) {
//                    break;
//                }
//
//                Mat hsv = new Mat();
//                Imgproc.cvtColor(frame, hsv, Imgproc.COLOR_BGR2HSV);
//                Mat dst = new Mat();
//                Imgproc.calcBackProject(Arrays.asList(hsv), new MatOfInt(0), roi_hist, dst, new MatOfFloat(0, 180), 1);
////
//                Rect track_window = new Rect(x, y, w, h);
//                Imgproc.meanShift(dst, track_window, term_crit);
//
//                x = track_window.x;
//                y = track_window.y;
//                w = track_window.width;
//                h = track_window.height;
//                Imgproc.rectangle(frame, new Point(x, y), new Point(x + w, y + h), new Scalar(255, 0, 0), 2);
//
//                HighGui.imshow("Tracking", frame);
//                int k = HighGui.waitKey(30) & 0xff;
//                if (k == 27) {
//                    break;
//                }
//            }
//        return new double[]{0, 0};
    }

    private Rect selectROI(Mat img) {

        // Define the ROI selector (You can replace this with a fixed ROI for simplicity)
        Point p1 = new Point(img.cols() / 2.0 - size, img.rows() / 2.0 - size);
        Point p2 = new Point(img.cols() / 2.0 + size, img.rows() / 2.0 + size);
        Rect rect = new Rect(p1, p2);

        // Draw the rectangle on the image
        Imgproc.rectangle(img, p1, p2, new Scalar(0, 255, 0), 2);

        return rect;
    }

    enum Drawing {
        DRAWING,
        TRACKING,
        CLEAR,
    }
}
