package com.dji.sdk.sample.demo.accurateLandingController;

import org.opencv.core.Mat;
import org.opencv.core.Rect2d;

import java.util.Arrays;
import java.util.List;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;
import org.opencv.core.Mat;
import org.opencv.core.Rect2d;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovementDetector {

    private YoloDetector yoloDetector;
    private Mat previousImage;

    public MovementDetector(YoloDetector yoloDetector) {
        this.yoloDetector = yoloDetector;
    }

    public void setOriginalImage(Mat image) {
        this.previousImage = image.clone();
    }

    public void detectMovement(Mat newImage) {
        // Define the list of classes you want to monitor for movement
        List<String> targetClasses = getTargetClasses();

        // Detect objects in the original image
        List<DetectedObject> previousObjects = yoloDetector.detectObjectsWithClass(previousImage, targetClasses, 0.5f, 0.4f);

        // Detect objects in the new image
        List<DetectedObject> newObjects = yoloDetector.detectObjectsWithClass(newImage, targetClasses, 0.5f, 0.4f);

        // Track movement and count objects that entered or moved
        Map<String, Integer> movementSummary = new HashMap<>();

        for (DetectedObject newObject : newObjects) {
            boolean foundMatchingObject = false;
            for (DetectedObject previousObject : previousObjects) {
                if (isSimilar(previousObject.getRect(), newObject.getRect())) {
                    foundMatchingObject = true;
                    break;
                }
            }
            if (!foundMatchingObject) {
                // New object has entered or existing object has moved
                movementSummary.put(newObject.getClassName(), movementSummary.getOrDefault(newObject.getClassName(), 0) + 1);
            }
        }

        if (!movementSummary.isEmpty()) {
            StringBuilder alertMessage = new StringBuilder("Movement detected: ");
            for (Map.Entry<String, Integer> entry : movementSummary.entrySet()) {
                alertMessage.append(entry.getValue()).append(" ").append(entry.getKey()).append(", ");
            }

            // Show toast with detected movement details
            showToast(alertMessage.toString());
        }

        // Update previous image with the current image for future comparisons
        previousImage = newImage.clone();
    }


//    public boolean detectMovement(Mat newImage) {
//        // Detect objects in the original image
//        List<DetectedObject> previousObjects = yoloDetector.detectObjectsWithLabels(previousImage, getTargetClasses(), 0.5f, 0.4f);
//
//        // Detect objects in the new image
//        List<DetectedObject> newObjects = yoloDetector.detectObjectsWithLabels(newImage, getTargetClasses(), 0.5f, 0.4f);
//
//        Map<String, Integer> newObjectCounts = new HashMap<>();
//        boolean movementDetected = false;
//
//        // Compare objects in the two images to detect movement
//        for (DetectedObject newObject : newObjects) {
//            boolean foundMatchingObject = false;
//            for (DetectedObject previousObject : previousObjects) {
//                if (isSimilar(previousObject.getRect(), newObject.getRect())) {
//                    foundMatchingObject = true;
//                    break;
//                }
//            }
//
//            if (!foundMatchingObject) {
//                // Movement detected: new object entered the scene
//                movementDetected = true;
//                String objectLabel = newObject.getLabel();
//
//                // Count the number of newly detected objects by type
//                newObjectCounts.put(objectLabel, newObjectCounts.getOrDefault(objectLabel, 0) + 1);
//            }
//        }
//
//        // Display toast if movement detected
//        if (movementDetected) {
//            StringBuilder movementMessage = new StringBuilder("System detected movement: ");
//            for (Map.Entry<String, Integer> entry : newObjectCounts.entrySet()) {
//                movementMessage.append(entry.getValue()).append(" ").append(entry.getKey()).append("(s), ");
//            }
//
//            // Trim the trailing comma and space
//            if (movementMessage.length() > 0) {
//                movementMessage.setLength(movementMessage.length() - 2);
//            }
//
//            // Show toast with the detected movement message
//            showToast(movementMessage.toString());
//        }
//
//        // Update previous image with the current image for future comparisons
//        previousImage = newImage.clone();
//
//        // Return whether movement was detected
//        return movementDetected;
//    }


//    public boolean detectMovement(Mat newImage) {
//        // Detect objects in the original image
//        List<Rect2d> previousObjects = yoloDetector.detectObjects(previousImage, getTargetClasses(), 0.5f, 0.4f);
//
//        // Detect objects in the new image
//        List<Rect2d> newObjects = yoloDetector.detectObjects(newImage, getTargetClasses(), 0.5f, 0.4f);
//
//        // Compare objects in the two images to detect movement
//        for (Rect2d newObject : newObjects) {
//            boolean foundMatchingObject = false;
//            for (Rect2d previousObject : previousObjects) {
//                if (isSimilar(previousObject, newObject)) {
//                    foundMatchingObject = true;
//                    break;
//                }
//            }
//            if (!foundMatchingObject) {
//                // New object has entered the scene or existing object has moved
//                System.out.println("Movement detected!");
//                return true;
//            }
//        }
//
//        // Update previous image with the current image for future comparisons
//        previousImage = newImage.clone();
//
//        // No movement detected
//        return false;
//    }

    private List<String> getTargetClasses() {
        // Define the list of classes you want to monitor for movement
        return Arrays.asList("person", "bicycle", "car", "motorbike", "bus", "train", "truck", "boat");
    }

    private boolean isSimilar(Rect2d r1, Rect2d r2) {
        // Check if two rectangles are close enough to be considered the same object
        double threshold = 0.5;
        double areaIntersection = r1.area() + r2.area() - r1.area() * r2.area();
        double areaUnion = r1.area() + r2.area();
        return (areaIntersection / areaUnion) > threshold;
    }
}

