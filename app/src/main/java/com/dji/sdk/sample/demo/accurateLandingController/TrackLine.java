package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.demo.accurateLandingController.ALRemoteControllerView.TAG;
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

    // Variables for storing the features to track
    MatOfPoint selectedLineFeatures, originalLineFeatures;
    MatOfPoint2f prevPoints, currPoints;
    Mat previousImage = null;
    // Variables to store original line properties
    private Point[] currentLine;
    private Point[] prevLine;
    private static final double CROP_SIZE_CM = 60.0;

    private double originalSlope, originalLineLength;
    private Context context;
    private static final String TAG = "TrackLine";
    private static final double SLOPE_THRESHOLD = 0.1;
    private static final int MAX_CORNERS = 100;
    private static final double QUALITY_LEVEL = 0.01;
    private static final double MIN_DISTANCE = 10;
    private static final int FEATURE_DISTANCE = 10;

    private static final int MAX_FRAMES_WITHOUT_DETECTION = 10;

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
        this.currentLine = selectedLine;

        // Initialize or update the feature points along the line
        selectedLineFeatures = new MatOfPoint();
        Mat grayImage = new Mat();
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);

        // Detect initial good features for tracking
        Imgproc.goodFeaturesToTrack(grayImage, selectedLineFeatures, MAX_CORNERS, QUALITY_LEVEL, MIN_DISTANCE, mask);
        prevPoints = new MatOfPoint2f(selectedLineFeatures.toArray());
        originalLineFeatures = new MatOfPoint(selectedLineFeatures.toArray());

        // Save the image as the previous frame for future optical flow calculations
        previousImage = grayImage.clone();
    }

    public Point[] trackSelectedLineUsingOpticalFlow(Mat newImage) {
        if (currentLine == null || previousImage == null || originalLineFeatures.empty()) {
            return null;
        }

        Mat grayFrame = new Mat();
        Imgproc.cvtColor(newImage, grayFrame, Imgproc.COLOR_BGR2GRAY);

        MatOfPoint2f nextFeatures = new MatOfPoint2f();
        MatOfByte status = new MatOfByte();
        MatOfFloat err = new MatOfFloat();

        Video.calcOpticalFlowPyrLK(previousImage, grayFrame, prevPoints, nextFeatures, status, err);

        List<Point> goodPrev = new ArrayList<>();
        List<Point> goodNext = new ArrayList<>();

        for (int i = 0; i < status.toArray().length; i++) {
            if (status.toArray()[i] == 1) {
                goodPrev.add(originalLineFeatures.toArray()[i]);
                goodNext.add(nextFeatures.toArray()[i]);
            }
        }

        if (goodPrev.size() >= 4 && goodNext.size() >= 4) {
            // Fit a line directly from the good points
            MatOfPoint2f goodNextMat = new MatOfPoint2f();
            goodNextMat.fromList(goodNext);
            currentLine = fitLineThroughPoints(goodNextMat);

            // Enforce slope constraint directly in this method
            double currentSlope = (currentLine[1].y - currentLine[0].y) / (currentLine[1].x - currentLine[0].x);
            if (Math.abs(currentSlope - originalSlope) > SLOPE_THRESHOLD) {
                currentLine = adjustSlopeToMatchOriginal(currentLine);
            }
        }

        detectLineFeatures(currentLine, newImage);
        newImage.copyTo(previousImage);
        prevPoints.fromArray(selectedLineFeatures.toArray());  // Update prevPoints for next optical flow

        return currentLine;
    }

    // Adjust the current line’s slope to match the original slope
    private Point[] adjustSlopeToMatchOriginal(Point[] line) {
        if (line == null || line.length < 2) {
            return line; // Return as is if there's an issue with the line array
        }

        // Calculate the midpoint of the line
        double midX = (line[0].x + line[1].x) / 2;
        double midY = (line[0].y + line[1].y) / 2;

        // Calculate new endpoints to match the original slope
        double dx = 100; // Arbitrary distance along the x-axis for slope calculation
        double dy = originalSlope * dx; // Calculate the y component based on the original slope

        // Create adjusted start and end points based on the midpoint
        Point newStartPoint = new Point(midX - dx, midY - dy);
        Point newEndPoint = new Point(midX + dx, midY + dy);

        return new Point[]{newStartPoint, newEndPoint};
    }



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


    // [ [(x1, y1), (x2, y2)], [(x3, y3), (x4, y4)]]
    // current line [(x5, y5), (x6, y6)] --------px1--------px2-------


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
        if (Math.abs(currentSlope - originalSlope) > 0) {  // Adjust the tolerance based on your needs
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

    public Point[] updateLineUsingDetectedLines(List<Object[]> detectedLines, Point[] currentLine) {

        if (detectedLines == null) { //  || detectedLines.isEmpty()
            showToast("detectedLines == null");
            return null;
        }

        double x1 = currentLine[0].x + (currentLine[1].x - currentLine[0].x) / 3;      // One-third point
        double x2 = currentLine[0].x + 2 * (currentLine[1].x - currentLine[0].x) / 3;  // Two-thirds point

        // Calculate y-values at x1 and x2 for the current line
        double y1Current = calculateY(currentLine, x1);
        double y2Current = calculateY(currentLine, x2);

        // Variables to store the best match
        Point[] bestLine = currentLine;
        double minDifference = Double.MAX_VALUE;

        // Define the maximum acceptable difference threshold
        final double MAX_ACCEPTABLE_DIFFERENCE = 150.0; // Adjust based on your needs


        // Loop through detected lines and calculate y-values at x1 and x2
        for (Object[] lineObj : detectedLines) {
            Point[] line = (Point[]) lineObj[0];
            double y1Detected = calculateY(line, x1);
            double y2Detected = calculateY(line, x2);

            // Calculate the difference in y-values at x1 and x2
            double difference = Math.abs(y1Detected - y1Current) + Math.abs(y2Detected - y2Current);

            // Update if the current line has the smallest difference
            if (difference < minDifference) {
                minDifference = difference;
                bestLine = line;
            }
        }

        // Return currentLine if the minimum difference exceeds the acceptable threshold
        return (minDifference <= MAX_ACCEPTABLE_DIFFERENCE) ? bestLine : null;
    }

    // Calculate the y-value of a line at a given x position
    private double calculateY(Point[] line, double x) {
        double slope = (line[1].y - line[0].y) / (line[1].x - line[0].x);
        return line[0].y + slope * (x - line[0].x);
    }


}