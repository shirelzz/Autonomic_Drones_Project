package com.dji.sdk.sample.demo.accurateLandingController;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;
import java.util.List;

public class DroneSafety {

    private YoloDetector yoloDetector;

    public DroneSafety(YoloDetector yoloDetector) {
        this.yoloDetector = yoloDetector;
    }

    public boolean checkForHazards(Mat image) {
        // Define the list of hazard classes (things you don't want the drone to land on)
        List<String> hazardClasses = Arrays.asList("person", "bicycle", "car", "motorbike", "bus",
                "train", "truck", "boat", "traffic light", "fire hydrant", "stop sign",
                "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
                "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag",
                "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball",
                "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
                "tennis racket", "bottle", "wine glass", "cup", "fork", "knife",
                "spoon", "bowl", "banana", "apple", "sandwich", "orange", "broccoli",
                "carrot", "hot dog", "pizza", "donut", "cake", "chair", "sofa",
                "pottedplant", "bed", "toilet", "tvmonitor",
                "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave",
                "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase",
                "scissors", "teddy bear", "hair drier", "toothbrush");
        // removed labels: "diningtable",

        // Use YOLO to detect objects in the image
        List<Rect2d> detectedHazards = yoloDetector.detectObjects(image, hazardClasses, 0.5f, 0.4f);

        // If any hazards are detected, return true
        if (!detectedHazards.isEmpty()) {
            System.out.println("Hazard detected! The drone should not land here.");
            return true;  // Hazard detected
        }

        // No hazards detected, safe to land
        return false;
    }

    public boolean checkForHazardsInRegion(Mat image, double altitude) {
        // Define the size of the region in real life (60 cm x 60 cm)
        double regionWidthCm = 60;
        double regionHeightCm = 60;

        // Calculate scale factor based on altitude
        double scaleFactor = altitude / 70.0;  // Using 70 cm case as the base

        // Calculate region dimensions in pixels
        double regionWidthPx = (regionWidthCm / (90 * scaleFactor)) * 640;
        double regionHeightPx = (regionHeightCm / (70 * scaleFactor)) * 480;

        // Define the center of the image
        double centerX = image.width() / 2;
        double centerY = image.height() / 2;

        // Define the region rectangle around the center
        Rect2d regionOfInterest = new Rect2d(
                centerX - regionWidthPx / 2,
                centerY - regionHeightPx / 2,
                regionWidthPx,
                regionHeightPx
        );

        // Draw the rectangle on the image for visualization
        Imgproc.rectangle(
                image,
                new Point(regionOfInterest.x, regionOfInterest.y),
                new Point(regionOfInterest.x + regionOfInterest.width, regionOfInterest.y + regionOfInterest.height),
                new Scalar(0, 255, 0),  // Green color
                2                        // Thickness of the rectangle border
        );

        // Detect hazards within the defined region
        List<String> hazardClasses = Arrays.asList("person", "bicycle", "car", "motorbike", "bus",
                "train", "truck", "boat", "traffic light", "fire hydrant", "stop sign",
                "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
                "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag",
                "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball",
                "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
                "tennis racket", "bottle", "wine glass", "cup", "fork", "knife",
                "spoon", "bowl", "banana", "apple", "sandwich", "orange", "broccoli",
                "carrot", "hot dog", "pizza", "donut", "cake", "chair", "sofa",
                "pottedplant", "bed", "toilet", "tvmonitor",
                "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave",
                "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase",
                "scissors", "teddy bear", "hair drier", "toothbrush");

        // Detect objects in the region of interest
        List<Rect2d> detectedHazards = yoloDetector.detectObjectsInRegion(image, hazardClasses, regionOfInterest, 0.5f, 0.4f);

        // Return true if any hazards are detected in the region
        return !detectedHazards.isEmpty();
    }
}

