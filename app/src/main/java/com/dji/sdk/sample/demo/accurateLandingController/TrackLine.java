package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;
import static org.opencv.android.Utils.matToBitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.opencv.calib3d.Calib3d;
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
import java.util.List;

public class TrackLine {

    private static final double CROP_SIZE_CM = 60.0;
    private static final String TAG = "TrackLine";
    private static final double SLOPE_THRESHOLD = 0.1;
    private static final int MAX_CORNERS = 100;
    private static final double QUALITY_LEVEL = 0.01;
    private static final double MIN_DISTANCE = 10;
    private static final int FEATURE_DISTANCE = 10;
    private static final int MAX_FRAMES_WITHOUT_DETECTION = 10;
    // Variables for storing the features to track
    MatOfPoint selectedLineFeatures, originalLineFeatures;
    MatOfPoint2f prevPoints, currPoints;
    Mat previousImage = null;
    // Variables to store original line properties
    private Point[] currentLine;
    private Point[] prevLine;
    private double originalSlope, originalLineLength;
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

    public Point[] getPrevLine() {
        return prevLine;
    }

    // Method to define the ROI around the previous line
    Point[] defineROI(Point[] previousLine, int margin) {
        // Calculate the points that define the rectangle ROI based on the line position and margin
        Point topLeft = new Point(previousLine[0].x - margin, previousLine[0].y - margin);
        Point bottomRight = new Point(previousLine[1].x + margin, previousLine[1].y + margin);
        return new Point[]{topLeft, bottomRight};
    }

    // Detect features along the selected line
    public void detectLineFeatures(Point[] selectedLine, Mat image) {
        // Create a mask around the selected line
        Mat mask = createLineMask(selectedLine, image.size());
        this.prevLine = this.currentLine;
        this.currentLine = selectedLine;
        // Detect good features (corners, edges) along the selected line
        selectedLineFeatures = new MatOfPoint();
        storeOriginalLineProperties(selectedLine);
        Mat grayImage = new Mat();
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
        Imgproc.goodFeaturesToTrack(grayImage, selectedLineFeatures, MAX_CORNERS, QUALITY_LEVEL, MIN_DISTANCE, mask);

        // Convert features to a format suitable for optical flow
        prevPoints = new MatOfPoint2f(selectedLineFeatures.toArray());
        originalLineFeatures = new MatOfPoint(selectedLineFeatures.toArray()); // Store original features for comparison

        // Update previous image
        previousImage = new Mat();
        image.copyTo(previousImage);
    }

    public Object[] findLineInRegion(Mat imageToProcess, double altitude) {
        showToast("findLineInRegion");
        Mat croppedImage = cropImage(imageToProcess, altitude);
        List<Object[]> allEdges = EdgeDetection.detectLines(croppedImage, altitude, true, 0);
        for (Object[] points : allEdges) {
            Imgproc.line(imageToProcess, (Point) points[0], (Point) points[1], new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
        }
        showToast(String.valueOf(allEdges.size()));

        if (allEdges.size() > 0) {
            return allEdges.get(0);
        }
        return null;
    }

    public Mat cropImage(Mat image, double altitude) {
        // Calculate real-world dimensions
        double[] realDimensions = calculateRealDimensions(altitude);
        double realWidth = realDimensions[0];
        double realHeight = realDimensions[1];

        // Calculate pixels per centimeter
        double pixelsPerCmWidth = (double) image.width() / realWidth;
        double pixelsPerCmHeight = (double) image.height() / realHeight;

        // Calculate crop size in pixels (60x60 cm)
        int cropSizePixels = (int) (CROP_SIZE_CM * Math.min(pixelsPerCmWidth, pixelsPerCmHeight));

        // Calculate starting point for centered crop
        int left = (image.width() - cropSizePixels) / 2;
        int top = (image.height() - cropSizePixels) / 2;

        // Define the region of interest (ROI)
        Rect roi = new Rect(left, top, cropSizePixels, cropSizePixels);

        // Perform the crop

        return new Mat(image, roi);

    }

    private double[] calculateRealDimensions(double altitude) {
        // Based on the given cases, we can derive a linear relationship
        // between altitude and real-world dimensions
        double widthToAltitudeRatio = 1.3333; // Derived from case 2 and 3
        double heightToAltitudeRatio = 1.0; // Derived from all cases

        double realWidth = altitude * widthToAltitudeRatio;
        double realHeight = altitude * heightToAltitudeRatio;

        return new double[]{realWidth, realHeight};
    }

    // Track the selected line in a new image - first try
//    public Point[] trackSelectedLineUsingOpticalFlow(Mat newImage) {
//        if (prevPoints == null || prevPoints.rows() == 0) {
//            return null; // No features to track
//        }
//
//        // Track the features using Optical Flow
//        currPoints = new MatOfPoint2f();
//        MatOfByte status = new MatOfByte();
//        MatOfFloat err = new MatOfFloat();
//
//        Video.calcOpticalFlowPyrLK(previousImage, newImage, prevPoints, currPoints, status, err);
//
//        // Filter valid points
//        List<Point> validPrevPoints = new ArrayList<>();
//        List<Point> validCurrPoints = new ArrayList<>();
//        byte[] statusArray = status.toArray();
//        Point[] prevArray = prevPoints.toArray();
//        Point[] currArray = currPoints.toArray();
//        // Check for array size mismatches
//        if (statusArray.length != prevArray.length || prevArray.length != currArray.length) {
//            throw new RuntimeException("Mismatched array lengths: status=" + statusArray.length +
//                    ", prevPoints=" + prevArray.length + ", currPoints=" + currArray.length);
//        }
//
//        for (int i = 0; i < statusArray.length; i++) {
//            if (statusArray[i] == 1) {
//                validPrevPoints.add(prevArray[i]);
//                validCurrPoints.add(currArray[i]);
////                               // Filter points that deviate too far from the original line
////                Point prevPoint = prevArray[i];
////                Point currPoint = currArray[i];
////                double distance = Math.abs((currPoint.y - prevPoint.y) - originalSlope * (currPoint.x - prevPoint.x));
////
////                if (distance < 10) {  // Adjust this threshold based on your specific use case
////                    validPrevPoints.add(prevPoint);
////                    validCurrPoints.add(currPoint);
////                }
//            }
//        }
//        previousImage = newImage;
//        // Fit a line through the tracked points
//        if (!validCurrPoints.isEmpty()) {
//            MatOfPoint2f validCurrMat = new MatOfPoint2f();
//            validCurrMat.fromList(validCurrPoints);
//
//            // Fit a line using the tracked points
//            Point[] trackedLine = fitLineThroughPoints(validCurrMat);
//
//            // Update previous points for the next iteration
//            prevPoints = validCurrMat;
//
//            return trackedLine;
//        }
////         // Check if the new line deviates too much from the original position
////            double distance = Math.sqrt(Math.pow(trackedLine[0].x - currentLine[0].x, 2) +
////                    Math.pow(trackedLine[0].y - currentLine[0].y, 2));
////
////            if (distance < 50) {  // Adjust based on your requirements
////                prevPoints = validCurrMat;
////                this.currentLine = trackedLine;
////                return trackedLine;
////            } else {
////                return null;  // Line has drifted too much, discard it
////            }
//        return null; // No valid line found
//    }

    public Point[] trackSelectedLineUsingOpticalFlow(Mat newImage) {
        if (currentLine == null || previousImage == null || originalLineFeatures.empty()) {
            return null;
        }

        Mat grayFrame = new Mat();
        Imgproc.cvtColor(newImage, grayFrame, Imgproc.COLOR_BGR2GRAY);

        MatOfPoint2f nextFeatures = new MatOfPoint2f();
        MatOfByte status = new MatOfByte();
        MatOfFloat err = new MatOfFloat();

        Mat grayPrev = new Mat();
        Imgproc.cvtColor(previousImage, grayPrev, Imgproc.COLOR_BGR2GRAY);

        Video.calcOpticalFlowPyrLK(grayPrev, grayFrame, prevPoints, nextFeatures, status, err);

        List<Point> goodPrev = new ArrayList<>();
        List<Point> goodNext = new ArrayList<>();

        for (int i = 0; i < status.toArray().length; i++) {
            if (status.toArray()[i] == 1) {
                goodPrev.add(originalLineFeatures.toArray()[i]);
                goodNext.add(nextFeatures.toArray()[i]);
            }
        }

        if (goodPrev.size() >= 4 && goodNext.size() >= 4) {
            MatOfPoint2f goodPrevMat = new MatOfPoint2f();
            MatOfPoint2f goodNextMat = new MatOfPoint2f();
            goodPrevMat.fromList(goodPrev);
            goodNextMat.fromList(goodNext);

            Mat homography = Calib3d.findHomography(goodPrevMat, goodNextMat, Calib3d.RANSAC, 3);

            if (!homography.empty()) {
                MatOfPoint2f initialLineMat = new MatOfPoint2f(currentLine);
                MatOfPoint2f transformedPoints = new MatOfPoint2f();
                Core.perspectiveTransform(initialLineMat, transformedPoints, homography);

                Point[] transformedArray = transformedPoints.toArray();
                if (transformedArray.length >= 2) {
                    currentLine = new Point[]{transformedArray[0], transformedArray[1]};
                }
            }
        }

        detectLineFeatures(currentLine, newImage);
        newImage.copyTo(previousImage);

        return currentLine;
    }


//    // Compare detected lines with the original line features and select the best line
//    public Point[] findBestMatchingLine(Mat image, double height) {
//        // Get horizontal lines with the same slope
//        List<Object[]> horizontalLines = EdgeDetection.detectLines(image, height,true, originalSlope);
//
//        if (horizontalLines.isEmpty()) {
//            return null; // No matching lines found
//        }
//
//        Point[] bestLine = null;
//        double bestScore = Double.MAX_VALUE;
//        showToast("horizontalLines: "+ horizontalLines.size());
//        // Compare each line to the original line features
//        for (Object[] entry : horizontalLines) {
//            Point[] line = (Point[]) entry;
//
//            // Calculate how close the line is to the original line (Euclidean distance)
//            double distance = Math.sqrt(Math.pow(line[0].x - currentLine[0].x, 2) + Math.pow(line[0].y - currentLine[0].y, 2));
//
//            // Compare line features with the original line features
//            Mat mask = createLineMask(line, image.size());
//            MatOfPoint newLineFeatures = new MatOfPoint();
//            Mat grayImage = new Mat();
//            Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
//            Imgproc.goodFeaturesToTrack(grayImage, newLineFeatures, 200, 0.03, 5, mask);
//
//            // Compare the features (count matches)
//            int matchingFeatures = countMatchingFeatures(newLineFeatures, selectedLineFeatures);
//
//            // Combine distance and feature matching to determine the best line (lower score is better)
//            double score = distance - matchingFeatures; // You can adjust the weights of distance and matchingFeatures
//
//            if (score < bestScore) {
//                bestScore = score;
//                bestLine = line;
//            }
//        }
//        return bestLine;
//    }
//
//    // Count how many features match between two sets of line features
//    private int countMatchingFeatures(MatOfPoint features1, MatOfPoint features2) {
//        Point[] points1 = features1.toArray();
//        Point[] points2 = features2.toArray();
//
//        int matchCount = 0;
//        for (Point p1 : points1) {
//            for (Point p2 : points2) {
//                double distance = Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
//                if (distance < 5) { // Adjust matching threshold as needed
//                    matchCount++;
//                }
//            }
//        }
//        return matchCount;
//    }

    // Helper function to create a mask around the selected line
//    private Mat createLineMask(Point[] selectedLine, Size imageSize) {
//        Mat mask = Mat.zeros(imageSize, CvType.CV_8U);
//        Imgproc.line(mask, selectedLine[0], selectedLine[1], new Scalar(255), 5); // Adjust thickness
//        Bitmap savedImage = Bitmap.createBitmap((int) imageSize.width, (int) imageSize.height, Bitmap.Config.ARGB_8888);
//        matToBitmap(mask, savedImage);
//        saveImage(savedImage);
//        return mask;
//    }

    private Mat createLineMask(Point[] selectedLine, Size imageSize) {
        Mat mask = Mat.zeros(imageSize, CvType.CV_8UC1);
        Point direction = new Point(selectedLine[1].x - selectedLine[0].x, selectedLine[1].y - selectedLine[0].y);
        double length = Math.sqrt(direction.x * direction.x + direction.y * direction.y);
        Point unitDirection = new Point(direction.x / length, direction.y / length);
        Point perpendicular = new Point(-unitDirection.y, unitDirection.x);

        List<Point> maskPoints = new ArrayList<>();
        for (int i = -2; i <= 2; i++) {
            Point offset = new Point(perpendicular.x * i * FEATURE_DISTANCE, perpendicular.y * i * FEATURE_DISTANCE);
            maskPoints.add(new Point(selectedLine[0].x + offset.x, selectedLine[0].y + offset.y));
            maskPoints.add(new Point(selectedLine[1].x + offset.x, selectedLine[1].y + offset.y));
        }

        MatOfPoint maskContour = new MatOfPoint();
        maskContour.fromList(maskPoints);
        List<MatOfPoint> contours = new ArrayList<>();
        contours.add(maskContour);
        Imgproc.fillPoly(mask, contours, new Scalar(255));

        Bitmap savedImage = Bitmap.createBitmap((int) imageSize.width, (int) imageSize.height, Bitmap.Config.ARGB_8888);
        matToBitmap(mask, savedImage);
        saveImage(savedImage);

        return mask;
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
        String mImageName = "MI_" + System.currentTimeMillis() + ".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
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
//        if(Math.abs(vy) > Math.abs(vx)){
//            vy = 0;
//            vx = 1;
//        }
        // Enforce the original slope
        double currentSlope = vy / vx;
        if (Math.abs(currentSlope - originalSlope) > 0.1) {  // Adjust the tolerance based on your needs
            vy = originalSlope * vx; // Adjust the new slope to match the original
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