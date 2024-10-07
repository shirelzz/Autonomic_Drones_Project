package com.dji.sdk.sample.demo.accurateLandingController;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.List;

public class TrackLine {

    // Variables for storing the features to track
    MatOfPoint selectedLineFeatures;
    MatOfPoint2f prevPoints, currPoints;
    Mat previousImage = null;
    // Variables to store original line properties
    private double originalLineLength;
    private double originalSlope;

    public TrackLine() {
    }

    // Calculate and store original line length and slope when selecting the line
    public void storeOriginalLineProperties(Point[] selectedLine) {
        // Calculate the length of the selected line
        originalLineLength = Math.sqrt(Math.pow(selectedLine[1].x - selectedLine[0].x, 2) +
                Math.pow(selectedLine[1].y - selectedLine[0].y, 2));

        // Calculate the slope of the selected line
        originalSlope = (selectedLine[1].y - selectedLine[0].y) / (selectedLine[1].x - selectedLine[0].x);
    }

    // Detect features along the selected line
    public void detectLineFeatures(Point[] selectedLine, Mat image) {
        // Create a mask around the selected line
        Mat mask = createLineMask(selectedLine, image.size());

        // Detect good features (corners, edges) along the selected line
        selectedLineFeatures = new MatOfPoint();

        Mat grayImage = new Mat();
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
        Imgproc.goodFeaturesToTrack(grayImage, selectedLineFeatures, 200, 0.03, 5, mask);

        // Convert features to a format suitable for optical flow
        prevPoints = new MatOfPoint2f(selectedLineFeatures.toArray());

        // Update previous image
        previousImage = image;
    }

    // Track the selected line in a new image - first try
    public Point[] trackSelectedLineUsingOpticalFlow(Mat newImage) {
        if (prevPoints == null || prevPoints.rows() == 0) {
            return null; // No features to track
        }

        // Track the features using Optical Flow
        currPoints = new MatOfPoint2f();
        MatOfByte status = new MatOfByte();
        MatOfFloat err = new MatOfFloat();

        Video.calcOpticalFlowPyrLK(previousImage, newImage, prevPoints, currPoints, status, err);

        // Filter valid points
        List<Point> validPrevPoints = new ArrayList<>();
        List<Point> validCurrPoints = new ArrayList<>();
        byte[] statusArray = status.toArray();
        Point[] prevArray = prevPoints.toArray();
        Point[] currArray = currPoints.toArray();
        // Check for array size mismatches
        if (statusArray.length != prevArray.length || prevArray.length != currArray.length) {
            throw new RuntimeException("Mismatched array lengths: status=" + statusArray.length +
                    ", prevPoints=" + prevArray.length + ", currPoints=" + currArray.length);
        }

        for (int i = 0; i < statusArray.length; i++) {
            if (statusArray[i] == 1) {
                validPrevPoints.add(prevArray[i]);
                validCurrPoints.add(currArray[i]);
            }
        }
        previousImage = newImage;
        // Fit a line through the tracked points
        if (!validCurrPoints.isEmpty()) {
            MatOfPoint2f validCurrMat = new MatOfPoint2f();
            validCurrMat.fromList(validCurrPoints);

            // Fit a line using the tracked points
            Point[] trackedLine = fitLineThroughPoints(validCurrMat);

            // Update previous points for the next iteration
            prevPoints = validCurrMat;

            return trackedLine;
        }

        return null; // No valid line found
    }

    // Helper function to create a mask around the selected line
    private Mat createLineMask(Point[] selectedLine, Size imageSize) {
        Mat mask = Mat.zeros(imageSize, CvType.CV_8U);
        Imgproc.line(mask, selectedLine[0], selectedLine[1], new Scalar(255), 5); // Adjust thickness
        return mask;
    }

    // Fit a line using tracked points
    private Point[] fitLineThroughPoints(MatOfPoint2f points) {
        if (points.rows() < 2) {
            throw new RuntimeException("Not enough points to fit a line");
        }

        Mat lineParams = new Mat();
        Imgproc.fitLine(points, lineParams, Imgproc.DIST_L2, 0, 0.01, 0.01);

        // lineParams contains [vx, vy, x0, y0]
        // Extract these values from the matrix
        double vx = lineParams.get(0, 0)[0];
        double vy = lineParams.get(1, 0)[0];
        double x0 = lineParams.get(2, 0)[0];
        double y0 = lineParams.get(3, 0)[0];
        if(Math.abs(vy) > Math.abs(vx)){
            vy = 0;
            vx = 1;
        }
        // Create a start and end point based on the direction (vx, vy) and a point on the line (x0, y0)
        Point startPoint = new Point(x0 - 1000 * vx, y0 - 1000 * vy); // Extend the line backward
        Point endPoint = new Point(x0 + 1000 * vx, y0 + 1000 * vy);   // Extend the line forward

        return new Point[]{startPoint, endPoint}; // Return the new tracked line
    }

    // Extract the region around the selected line
    private Mat extractLineRegion(Mat image, Point[] selectedLine) {
        // Create a bounding box around the line
        int padding = 10;
        int x1 = (int) Math.max(0, Math.min(selectedLine[0].x, selectedLine[1].x) - padding);
        int y1 = (int) Math.max(0, Math.min(selectedLine[0].y, selectedLine[1].y) - padding);
        int x2 = (int) Math.min(image.cols(), Math.max(selectedLine[0].x, selectedLine[1].x) + padding);
        int y2 = (int) Math.min(image.rows(), Math.max(selectedLine[0].y, selectedLine[1].y) + padding);

        Rect boundingBox = new Rect(x1, y1, x2 - x1, y2 - y1);
        return new Mat(image, boundingBox); // Extract the region
    }

    // Track the selected line using template matching
    private Point[] trackSelectedLineUsingTemplateMatching(Mat newImage, Point[] selectedLine) {
        if (selectedLine == null) {
            return null;
        }

        // Extract the template from the previous image (around the selected line)
        Mat template = extractLineRegion(previousImage, selectedLine);

        // Perform template matching in the new image
        Mat result = new Mat();
        Imgproc.matchTemplate(newImage, template, result, Imgproc.TM_CCOEFF_NORMED);

        // Find the best match location
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
        Point matchLoc = mmr.maxLoc;

        // Calculate the new line position based on the match location
        Point[] trackedLine = new Point[]{
                new Point(matchLoc.x, matchLoc.y),
                new Point(matchLoc.x + selectedLine[1].x - selectedLine[0].x, matchLoc.y + selectedLine[1].y - selectedLine[0].y)
        };
        previousImage = newImage;
        return trackedLine;
    }

//    // Re-detect lines using Hough Transform and find the closest one
//    private Point[] trackSelectedLineUsingHoughTransform(Mat newImage) {
//        List<Point[]> lines = detectLinesInImage(newImage); // Detect lines in the new image
//
//        Point[] closestLine = null;
//        double minDistance = Double.MAX_VALUE;
//
//        for (Point[] line : lines) {
//            double distance = calculateDistanceBetweenLines(selectedLine, line);
//            if (distance < minDistance) {
//                minDistance = distance;
//                closestLine = line;
//            }
//        }
//
//        return closestLine; // Return the closest line
//    }

//    // Calculate distance between two lines represented by points
//    private double calculateDistanceBetweenLines(Point[] line1, Point[] line2) {
//        double distance1 = calculatePointToLineDistance(line1[0], line2);
//        double distance2 = calculatePointToLineDistance(line1[1], line2);
//        double distance3 = calculatePointToLineDistance(line2[0], line1);
//        double distance4 = calculatePointToLineDistance(line2[1], line1);
//
//        return Math.min(Math.min(distance1, distance2), Math.min(distance3, distance4));
//    }
//
//    // Calculate the distance from a point to the nearest point on a line
//    private double calculatePointToLineDistance(Point point, Point[] line) {
//        double x0 = point.x;
//        double y0 = point.y;
//        double x1 = line[0].x;
//        double y1 = line[0].y;
//        double x2 = line[1].x;
//        double y2 = line[1].y;
//        double numerator = Math.abs((y2 - y1) * x0 - (x2 - x1) * y0 + x2 * y1 - y2 * x1);
//        double denominator = Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2));
//        return numerator / denominator;
//    }
}
