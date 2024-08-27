package com.dji.sdk.sample.demo.accurateLandingController;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class EdgeDetection {

    private static final int edgeThreshold = 150;
    private static final int threshold1 = 200; //170
    private static final int threshold2 = 300; //280
    private static final int apertureSize = 3;

    // adjust the maximum number of found lines in the image
    private static final int maxLines = 5;
    private static final double minLineLength = 100;  // Minimum length of the detected line
    private static final double maxLineGap = 20;      // Maximum gap between line segments

    // Check if it works = return an array with edges
    public static List<Object[]> detectLines(Mat input) {
        // Declare the output variables
        Mat dst = new Mat(), cdst = new Mat(), gray = new Mat();

//        Imgproc.cvtColor(input, gray, Imgproc.COLOR_BGR2GRAY);

        // Edge detection
        Imgproc.Canny(input, dst, threshold1, threshold2, apertureSize, false);

        // Copy edges to the images that will display the results in BGR
        Imgproc.cvtColor(dst, cdst, Imgproc.COLOR_GRAY2BGR);

        // Standard Hough Line Transform
        Mat lines = new Mat(); // will hold the results of the detection
//        LinkedList<LineParametric2D_F32> newLines = new LinkedList<>();
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

//            Imgproc.line(cdst, pt1, pt2, new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);

            pointList.add(new Object[]{new Point[]{pt1, pt2}, isHorizontal});
            Imgproc.line(input, pt1, pt2, new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
        }

//        // Combine overlapping lines into one closest to the center
//        if (pointList.size() > 1) {
//            Point center = new Point(input.cols() / 2.0, input.rows() / 2.0);
//            pointList = combineAndSelectLines(pointList, center);
//        }
//
////        Draw the final lines
//        for (Object[] lineInfo : pointList) {
//            Point[] points = (Point[]) lineInfo[0];
//            Imgproc.line(input, points[0], points[1], new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
//        }

        return pointList;
    }

    private static List<Object[]> combineAndSelectLines(List<Object[]> lines, Point center) {
        List<Object[]> combinedLines = new ArrayList<>();
        boolean[] merged = new boolean[lines.size()];

        for (int i = 0; i < lines.size(); i++) {
            if (merged[i]) continue;

            Point[] lineA = (Point[]) lines.get(i)[0];
            Point midpointA = getMidpoint(lineA);

            List<Point[]> similarLines = new ArrayList<>();
            similarLines.add(lineA);
            merged[i] = true;

            for (int j = i + 1; j < lines.size(); j++) {
                if (merged[j]) continue;

                Point[] lineB = (Point[]) lines.get(j)[0];
                Point midpointB = getMidpoint(lineB);

                // Calculate the distance between midpoints
                double distance = getDistance(midpointA, midpointB);

                // If the distance is below the threshold, merge the lines
                if (distance < 50) { // Example threshold, adjust as needed
                    similarLines.add(lineB);
                    merged[j] = true;
                }
            }

            // Combine the similar lines into one
            Point[] mergedLine = mergeLines(similarLines);

            combinedLines.add(new Object[]{mergedLine, true}); // Assuming horizontal lines for simplicity
        }
        // Sort lines by distance to the center and return the closest one
        combinedLines.sort((a, b) -> {
            Point[] lineA = (Point[]) a[0];
            Point[] lineB = (Point[]) b[0];

            double distA = distanceToCenter(lineA, center);
            double distB = distanceToCenter(lineB, center);

            return Double.compare(distA, distB);
        });

        return combinedLines.subList(0, 1); // Return only the closest line
    }

    // Helper function to get the midpoint of a line
    private static Point getMidpoint(Point[] line) {
        return new Point((line[0].x + line[1].x) / 2, (line[0].y + line[1].y) / 2);
    }

    // Helper function to calculate the distance between two points
    private static double getDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    // Helper function to merge multiple lines into one
    private static Point[] mergeLines(List<Point[]> lines) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

        for (Point[] line : lines) {
            minX = Math.min(minX, Math.min(line[0].x, line[1].x));
            minY = Math.min(minY, Math.min(line[0].y, line[1].y));
            maxX = Math.max(maxX, Math.max(line[0].x, line[1].x));
            maxY = Math.max(maxY, Math.max(line[0].y, line[1].y));
        }

        return new Point[]{
                new Point(minX, minY),
                new Point(maxX, maxY)
        };
    }

    // Function to calculate the distance of a line's midpoint to the center of the image
    private static double distanceToCenter(Point[] line, Point center) {
        Point midpoint = getMidpoint(line);
        return getDistance(midpoint, center);
    }
}
