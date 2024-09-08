package com.dji.sdk.sample.demo.accurateLandingController;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect2d;

import java.util.Arrays;
import java.util.List;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

import android.graphics.Bitmap;

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

