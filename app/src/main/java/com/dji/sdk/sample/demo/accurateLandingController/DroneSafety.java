package com.dji.sdk.sample.demo.accurateLandingController;
import org.opencv.core.Mat;
import org.opencv.core.Rect2d;

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
}

