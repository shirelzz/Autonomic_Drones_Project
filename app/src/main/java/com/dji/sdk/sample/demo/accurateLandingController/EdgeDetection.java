package com.dji.sdk.sample.demo.accurateLandingController;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class EdgeDetection {

    private static final int edgeThreshold = 150;
    // adjust the maximum number of found lines in the image
    private static final int maxLines = 5;

//    public EdgeDetection() {
//
//    }
    // Check if it works = return an array with edges
    public static Point[][] detectLines(Mat input) {
        // Declare the output variables
        Mat dst = new Mat(), cdst = new Mat();

        // Edge detection
        Imgproc.Canny(input, dst, 100, 400, 3, false);

        // Copy edges to the images that will display the results in BGR
        Imgproc.cvtColor(dst, cdst, Imgproc.COLOR_GRAY2BGR);

        // Standard Hough Line Transform
        Mat lines = new Mat(); // will hold the results of the detection
//        LinkedList<LineParametric2D_F32> newLines = new LinkedList<>();
        Imgproc.HoughLines(dst, lines, 1, Math.PI / 180, edgeThreshold); // runs the actual detection
        Log.i("EdgeDetection", "Number of lines detected: " + lines.rows());

        int num = lines.rows();
        Point[][] point_array = new Point[num][2];
        int i = 0;
        // Draw the lines
        for (int x = 0; x < num; x++) {
            double[] line = lines.get(x, 0);
            double rho = line[0], theta = line[1];

            double a = Math.cos(theta), b = Math.sin(theta);
            double x0 = a * rho, y0 = b * rho;
            Point pt1 = new Point(Math.round(x0 + 1000 * (-b)), Math.round(y0 + 1000 * (a)));
            Point pt2 = new Point(Math.round(x0 - 1000 * (-b)), Math.round(y0 - 1000 * (a)));

//            Imgproc.line(cdst, pt1, pt2, new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
            Imgproc.line(input, pt1, pt2, new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
            point_array[i] = new Point[]{pt1, pt2};
        }
        return point_array;
    }
}