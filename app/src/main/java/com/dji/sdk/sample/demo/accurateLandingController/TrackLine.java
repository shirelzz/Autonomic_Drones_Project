package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.demo.accurateLandingController.ALRemoteControllerView.TAG;
import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

import static org.opencv.android.Utils.matToBitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrackLine {

    // Variables for storing the features to track
    MatOfPoint selectedLineFeatures;
    MatOfPoint2f prevPoints, currPoints;
    Mat previousImage = null;
    // Define tolerances for slope and distance
    private static final double TOLERANCE_SLOPE = 0.1; // Adjust as necessary
    private static final double TOLERANCE_DISTANCE = 5.0; // Adjust as necessary

    // Variables to store original line properties
    private double originalLineLength;
    private double originalSlope;
    private Context context;


    public TrackLine(Context context) {
        this.context = context;
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
        showToast(Arrays.toString(selectedLineFeatures.toArray()));
        // Convert features to a format suitable for optical flow
        prevPoints = new MatOfPoint2f(selectedLineFeatures.toArray());

        // Update previous image
        previousImage = image;
    }

    public void saveImage(Bitmap bitmap) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d(TAG,
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }
    private File getOutputMediaFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + context.getPackageName()
                + "/Files");

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        // Create a media file name
        File mediaFile;
        String mImageName = "mask_" + System.currentTimeMillis() + ".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }
    // Track the selected line in a new image - first try
    public Point[] trackSelectedLineUsingOpticalFlow(Mat newImage, Point[] originalLine) {
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

//            // Validate against the original line's properties
//            double currentSlope = calculateLineSlope(trackedLine);
//            double distanceToOriginal = calculateDistanceBetweenLines(trackedLine, originalLine);
//
//            // Check if the slope and distance are within acceptable bounds
//            if (Math.abs(currentSlope - originalSlope) > TOLERANCE_SLOPE ||
//                    distanceToOriginal > TOLERANCE_DISTANCE) {
//                // Reject this line and continue with previous points or reinitialize
//                return null; // or handle reinitialization logic
//            }

            // Update previous points for the next iteration
            prevPoints = validCurrMat;

            return trackedLine;
        }

        return null; // No valid line found
    }


    private double calculateLineSlope(Point[] line) {
        return (line[1].y - line[0].y) / (line[1].x - line[0].x);
    }

    private double calculateDistanceBetweenLines(Point[] line1, Point[] line2) {
        // Line1: passes through points (x1, y1) and (x2, y2)
        double x1 = line1[0].x;
        double y1 = line1[0].y;
        double x2 = line1[1].x;
        double y2 = line1[1].y;

        // Line2: passes through points (x3, y3) and (x4, y4)
        double x3 = line2[0].x;
        double y3 = line2[0].y;
        double x4 = line2[1].x;
        double y4 = line2[1].y;

        // Equation of Line1: Ax + By + C = 0
        double A1 = y2 - y1;
        double B1 = x1 - x2;
        double C1 = A1 * x1 + B1 * y1;

        // Equation of Line2: Ax + By + C = 0
        double A2 = y4 - y3;
        double B2 = x3 - x4;
        double C2 = A2 * x3 + B2 * y3;

        // Calculate distance between the two lines using formula:
        // d = |C2 - C1| / sqrt(A1^2 + B1^2)
        double numerator = Math.abs(C2 - C1);
        double denominator = Math.sqrt(A1 * A1 + B1 * B1);

        // If denominator is 0, lines are parallel and coincident
        if (denominator == 0) {
            return 0;
        }

        // Return the shortest distance between the two lines
        return numerator / denominator;
    }


    // Helper function to create a mask around the selected line
    private Mat createLineMask(Point[] selectedLine, Size imageSize) {
        Mat mask = Mat.zeros(imageSize, CvType.CV_8U);
        Imgproc.line(mask, selectedLine[0], selectedLine[1], new Scalar(255), 5); // Adjust thickness
        Bitmap frame = Bitmap.createBitmap((int) imageSize.width, (int) imageSize.height, Bitmap.Config.ARGB_8888);
        matToBitmap(mask, frame);

        saveImage(frame);
        return mask;
    }

    // Fit a line using tracked points
    private Point[] fitLineThroughPoints(MatOfPoint2f points) {
        if (points.rows() < 2) {
            throw new RuntimeException("Not enough points to fit a line");
        }

        Mat lineParams = new Mat();
        Imgproc.fitLine(points, lineParams, Imgproc.DIST_L2, 0, 0.001, 0.001);

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
