package com.dji.sdk.sample.demo.accurateLandingController;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

import java.util.ArrayList;
import java.util.List;

public class EdgeDetection {

    private static final int edgeThreshold = 150;
    private static final int threshold1 = 170; //200;//170;
    private static final int threshold2 = 280; //300;//280;
    private static final int apertureSize = 3;
    // adjust the maximum number of found lines in the image
    private static final int maxLines = 5;
    private static final double minLineLength = 100;  // Minimum length of the detected line
    private static final double maxLineGap = 20;      // Maximum gap between line segments
    private static boolean in_blur = false;

    // Check if it works = return an array with edges
    public static List<Object[]> detectLines(Mat input, double droneHeight, boolean detectLineAlgo, double originalSlope) {
        in_blur = false;
        // Declare the output variables
        Mat dst = new Mat(), cdst = new Mat();

        // Edge detection
        Imgproc.Canny(input, dst, threshold1, threshold2, apertureSize, false);

        // Copy edges to the images that will display the results in BGR
        Imgproc.cvtColor(dst, cdst, Imgproc.COLOR_GRAY2BGR);

        // Standard Hough Line Transform
        Mat lines = new Mat(); // will hold the results of the detection
        Imgproc.HoughLines(dst, lines, 1, Math.PI / 180, edgeThreshold); // runs the actual detection

        int num = lines.rows();
        List<Object[]> pointList = new ArrayList<>();
        int i = 0;
        // Draw the lines
        for (int x = 0; x < num; x++) {
            double[] line = lines.get(x, 0);
            double rho = line[0], theta = line[1];
            // Convert theta from radians to degrees
            double thetaDegrees = Math.toDegrees(theta);

            // Determine if the line is horizontal (within 30 degrees of the x-axis)
            boolean isHorizontal = (Math.abs(thetaDegrees) <= 30) || (thetaDegrees >= 150 && thetaDegrees <= 180);


            double a = Math.cos(theta), b = Math.sin(theta);
            double x0 = a * rho, y0 = b * rho;
            Point pt1 = new Point(Math.round(x0 + 1000 * (-b)), Math.round(y0 + 1000 * (a)));
            Point pt2 = new Point(Math.round(x0 - 1000 * (-b)), Math.round(y0 - 1000 * (a)));
//            if (detectLineAlgo) {
//                // Calculate the slope of the detected line
//                double slope = (pt2.y - pt1.y) / (pt2.x - pt1.x);
//
//                // If the slope is close to the original slope, save the line
//                if (Math.abs(slope - originalSlope) < 0.1) { // Adjust threshold as needed
//                    pointList.add(new Point[]{pt1, pt2});
//                }
//            } else {
            if(!isHorizontal) {
                pointList.add(new Object[]{new Point[]{pt1, pt2}, isHorizontal});
                Imgproc.line(input, pt1, pt2, new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
            }


//                Imgproc.line(input, pt1, pt2, new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
//            }

        }

        if (pointList.size() == 0 && droneHeight <= 0.2) {
            pointList = detectBlurLines(input, true);
        }

        return pointList.subList(0, Math.min(50, pointList.size()));
    }

    public static List<Object[]> detectBlurLines(Mat input, boolean detectLowerHalf) {
        in_blur = true;
        Mat imgSrcRgb = new Mat();
        Imgproc.cvtColor(input, imgSrcRgb, Imgproc.COLOR_BGR2RGB);

        // Optionally crop the image to the lower half
        if (detectLowerHalf) {
            int height = input.rows();
            int width = input.cols();
            // Define the region of interest (ROI) as the lower half of the image
            Rect roi = new Rect(0, height / 2, width, height / 2);
            imgSrcRgb = new Mat(imgSrcRgb, roi); // Crop the image
        }


        // Create an inpainting mask with "red-enough" pixels
        Mat mask = new Mat();
        Core.inRange(imgSrcRgb, new Scalar(200, 0, 0), new Scalar(255, 50, 50), mask);

        // Enlarge the mask to cover the borders
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(mask, mask, kernel, new Point(-1, -1), 1);

        // Inpaint the red parts using Navier-Stokes based approach
//        Mat imgDst = new Mat();
        Photo.inpaint(imgSrcRgb, mask, imgSrcRgb, 50, Photo.INPAINT_NS);

        // Convert to grayscale
//        Mat gray = new Mat();
        Imgproc.cvtColor(imgSrcRgb, imgSrcRgb, Imgproc.COLOR_RGB2GRAY);

        // Detect edges using the autoCanny function
        Mat edges = autoCanny(imgSrcRgb); // You need to implement autoCanny in Java

        // Detect lines using HoughLines
        Mat lines = new Mat();
        Imgproc.HoughLines(edges, lines, 1, Math.PI / 90, 50);

        List<Object[]> pointList = new ArrayList<>();

        // Draw the lines on the image
        for (int i = 0; i < lines.rows() && i < 50; i++) {
            double[] line = lines.get(i, 0);
            double rho = line[0];
            double theta = line[1];
            double a = Math.cos(theta);
            double b = Math.sin(theta);
            double x0 = a * rho;
            double y0 = b * rho;

            // Convert theta from radians to degrees
            double thetaDegrees = Math.toDegrees(theta);

            // Determine if the line is horizontal (within 30 degrees of the x-axis)
            boolean isHorizontal = (Math.abs(thetaDegrees) <= 30) || (thetaDegrees >= 150 && thetaDegrees <= 180);

            Point pt1 = new Point(Math.round(x0 + 10000 * (-b)), Math.round(y0 + 10000 * (a)));
            Point pt2 = new Point(Math.round(x0 - 10000 * (-b)), Math.round(y0 - 10000 * (a)));
            if (detectLowerHalf) {
                pt1.y += input.rows() / 2.0; // Adjust y-coordinates back to full image scale
                pt2.y += input.rows() / 2.0;
            }
            pointList.add(new Object[]{new Point[]{pt1, pt2}, isHorizontal});

            Imgproc.line(input, pt1, pt2, new Scalar(0, 255, 0), 3, Imgproc.LINE_AA, 0);
        }

        // Save the result image
//        Imgcodecs.imwrite("linesDetected.jpg", imgDst);
        return pointList.subList(0, Math.min(50, pointList.size()));
    }

    // Implement autoCanny function in Java
    public static Mat autoCanny(Mat image) {
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(image, blurred, new Size(5, 5), 0);
        Mat edges = new Mat();
        Imgproc.Canny(blurred, edges, 50, 150);
        return edges;
    }

    public static boolean isIn_blur() {
        return in_blur;
    }

    public static List<List<Object[]>> detectLinesAndPoint(Mat input) {
        // Declare the output variables
        Mat dst = new Mat(), cdst = new Mat();

        // Edge detection
        Imgproc.Canny(input, dst, threshold1, threshold2, apertureSize, false);

        // Copy edges to the images that will display the results in BGR
        Imgproc.cvtColor(dst, cdst, Imgproc.COLOR_GRAY2BGR);

        // Standard Hough Line Transform
        Mat lines = new Mat(); // will hold the results of the detection
        Imgproc.HoughLines(dst, lines, 1, Math.PI / 180, edgeThreshold); // runs the actual detection

        int num = lines.rows();
        List<Object[]> horizontalLines = new ArrayList<>();
        List<Object[]> verticalLines = new ArrayList<>();

        List<List<Object[]>> pointList = new ArrayList<>();
        int i = 0;
        // Draw the lines
        for (int x = 0; x < num; x++) {
            double[] line = lines.get(x, 0);
            double rho = line[0], theta = line[1];
            // Convert theta from radians to degrees
            double thetaDegrees = Math.toDegrees(theta);

            // Determine if the line is horizontal (within 30 degrees of the x-axis)
            boolean isHorizontal = (Math.abs(thetaDegrees) <= 30) || (thetaDegrees >= 150 && thetaDegrees <= 180);

            double a = Math.cos(theta), b = Math.sin(theta);
            double x0 = a * rho, y0 = b * rho;
            Point pt1 = new Point(Math.round(x0 + 1000 * (-b)), Math.round(y0 + 1000 * (a)));
            Point pt2 = new Point(Math.round(x0 - 1000 * (-b)), Math.round(y0 - 1000 * (a)));

//            pointList.add(new Object[]{new Point[]{pt1, pt2}, isHorizontal});
            Imgproc.line(input, pt1, pt2, new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);

            if (isHorizontal) {
                horizontalLines.add(new Object[]{new Point[]{pt1, pt2}, rho, theta});
            } else {
                verticalLines.add(new Object[]{new Point[]{pt1, pt2}, rho, theta});
            }
            pointList.add(horizontalLines);
            pointList.add(verticalLines);
        }

        return pointList;
    }

//    private static List<Object[]> combineAndSelectLines(List<Object[]> lines, Point center) {
//        List<Object[]> combinedLines = new ArrayList<>();
//        boolean[] merged = new boolean[lines.size()];
//
//        for (int i = 0; i < lines.size(); i++) {
//            if (merged[i]) continue;
//
//            Point[] lineA = (Point[]) lines.get(i)[0];
//            Point midpointA = getMidpoint(lineA);
//
//            List<Point[]> similarLines = new ArrayList<>();
//            similarLines.add(lineA);
//            merged[i] = true;
//
//            for (int j = i + 1; j < lines.size(); j++) {
//                if (merged[j]) continue;
//
//                Point[] lineB = (Point[]) lines.get(j)[0];
//                Point midpointB = getMidpoint(lineB);
//
//                // Calculate the distance between midpoints
//                double distance = getDistance(midpointA, midpointB);
//
//                // If the distance is below the threshold, merge the lines
//                if (distance < 50) { // Example threshold, adjust as needed
//                    similarLines.add(lineB);
//                    merged[j] = true;
//                }
//            }
//
//            // Combine the similar lines into one
//            Point[] mergedLine = mergeLines(similarLines);
//
//            combinedLines.add(new Object[]{mergedLine, true}); // Assuming horizontal lines for simplicity
//        }
//        // Sort lines by distance to the center and return the closest one
//        combinedLines.sort((a, b) -> {
//            Point[] lineA = (Point[]) a[0];
//            Point[] lineB = (Point[]) b[0];
//
//            double distA = distanceToCenter(lineA, center);
//            double distB = distanceToCenter(lineB, center);
//
//            return Double.compare(distA, distB);
//        });
//
//        return combinedLines.subList(0, 1); // Return only the closest line
//    }
//
//    // Helper function to get the midpoint of a line
//    private static Point getMidpoint(Point[] line) {
//        return new Point((line[0].x + line[1].x) / 2, (line[0].y + line[1].y) / 2);
//    }
//
//    // Helper function to calculate the distance between two points
//    private static double getDistance(Point p1, Point p2) {
//        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
//    }

    // Helper function to merge multiple lines into one
//    private static Point[] mergeLines(List<Point[]> lines) {
//        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
//        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
//
//        for (Point[] line : lines) {
//            minX = Math.min(minX, Math.min(line[0].x, line[1].x));
//            minY = Math.min(minY, Math.min(line[0].y, line[1].y));
//            maxX = Math.max(maxX, Math.max(line[0].x, line[1].x));
//            maxY = Math.max(maxY, Math.max(line[0].y, line[1].y));
//        }
//
//        return new Point[]{
//                new Point(minX, minY),
//                new Point(maxX, maxY)
//        };
//    }
//
//    // Function to calculate the distance of a line's midpoint to the center of the image
//    private static double distanceToCenter(Point[] line, Point center) {
//        Point midpoint = getMidpoint(line);
//        return getDistance(midpoint, center);
//    }
}
