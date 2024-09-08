package com.dji.sdk.sample.demo.accurateLandingController;

import android.content.Context;
import org.opencv.core.*;
import org.opencv.dnn.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class YoloDetector {

    private Net yoloNet;
    private Context context;

    public YoloDetector(Context context, String modelConfig, String modelWeights) {
        this.context = context;

        // Load the network
        String cfgFilePath = null;
        String weightsFilePath = null;
        try {
            cfgFilePath = copyAssetToFile(modelConfig);
            weightsFilePath = copyAssetToFile(modelWeights);
        } catch (IOException e) {
            e.printStackTrace();
        }

        yoloNet = Dnn.readNetFromDarknet(cfgFilePath, weightsFilePath);
        yoloNet.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV);
        yoloNet.setPreferableTarget(Dnn.DNN_TARGET_CPU);  // or DNN_TARGET_GPU if you want to use GPU
    }

    private String copyAssetToFile(String assetFileName) throws IOException {
        File file = new File(context.getFilesDir(), assetFileName);
        try (InputStream inputStream = context.getAssets().open(assetFileName);
             OutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
        return file.getAbsolutePath();
    }

    public List<Rect2d> detectObjects(Mat image, List<String> targetClasses, float confThreshold, float nmsThreshold) {
        List<Rect2d> detectedObjects = new ArrayList<>();

        // Convert image to 3 channels (RGB) if it has 4 channels (RGBA)
        if (image.channels() == 4) {
            Imgproc.cvtColor(image, image, Imgproc.COLOR_RGBA2RGB);
        } else if (image.channels() == 1) {
            Imgproc.cvtColor(image, image, Imgproc.COLOR_GRAY2RGB);
        }

        // Prepare the blob from the image
        Mat blob = Dnn.blobFromImage(image, 1 / 255.0, new Size(416, 416), new Scalar(0, 0, 0), true, false);

        // Set the input to the network
        yoloNet.setInput(blob);

        // Forward pass through the network to get the output
        List<Mat> outs = new ArrayList<>();
        List<String> outBlobNames = getOutputsNames(yoloNet);
        yoloNet.forward(outs, outBlobNames);

        // Process the network output
        List<Integer> classIds = new ArrayList<>();
        List<Float> confidences = new ArrayList<>();
        List<Rect2d> boxes = new ArrayList<>();

        for (Mat out : outs) {
            for (int i = 0; i < out.rows(); i++) {
                Mat row = out.row(i);
                Mat scores = row.colRange(5, out.cols());
                Core.MinMaxLocResult result = Core.minMaxLoc(scores);
                float confidence = (float) result.maxVal;
                Point classIdPoint = result.maxLoc;
                if (confidence > confThreshold) {
                    int centerX = (int) (row.get(0, 0)[0] * image.cols());
                    int centerY = (int) (row.get(0, 1)[0] * image.rows());
                    int width = (int) (row.get(0, 2)[0] * image.cols());
                    int height = (int) (row.get(0, 3)[0] * image.rows());
                    int left = centerX - width / 2;
                    int top = centerY - height / 2;

                    classIds.add((int) classIdPoint.x);
                    confidences.add(confidence);
                    boxes.add(new Rect2d(left, top, width, height));
                }
            }
        }

        // Apply non-maximum suppression only if there are any detections
        if (!confidences.isEmpty()) {
            MatOfFloat matConfidences = new MatOfFloat(Converters.vector_float_to_Mat(confidences));
            MatOfRect2d matBoxes = new MatOfRect2d(boxes.toArray(new Rect2d[0]));
            MatOfInt indices = new MatOfInt();
            Dnn.NMSBoxes(matBoxes, matConfidences, confThreshold, nmsThreshold, indices);

            // Filter the detections to include only the target classes (e.g., persons and trees)
            for (int idx : indices.toArray()) {
                int classId = classIds.get(idx);
                String className = getClassLabel(classId);
                if (targetClasses.contains(className)) {
                    detectedObjects.add(boxes.get(idx));
                }
            }
        } else {
            Log.d("YOLO", "No objects detected with confidence greater than threshold.");
        }

        return detectedObjects;
    }

    public List<DetectedObject> detectObjectsWithClass(Mat image, List<String> targetClasses, float confThreshold, float nmsThreshold) {
        List<DetectedObject> detectedObjects = new ArrayList<>();

        // Convert image to 3 channels (RGB) if it has 4 channels (RGBA)
        if (image.channels() == 4) {
            Imgproc.cvtColor(image, image, Imgproc.COLOR_RGBA2RGB);
        } else if (image.channels() == 1) {
            Imgproc.cvtColor(image, image, Imgproc.COLOR_GRAY2RGB);
        }

        // Prepare the blob from the image
        Mat blob = Dnn.blobFromImage(image, 1 / 255.0, new Size(416, 416), new Scalar(0, 0, 0), true, false);

        // Set the input to the network
        yoloNet.setInput(blob);

        // Forward pass through the network to get the output
        List<Mat> outs = new ArrayList<>();
        List<String> outBlobNames = getOutputsNames(yoloNet);
        yoloNet.forward(outs, outBlobNames);

        // Process the network output
        List<Integer> classIds = new ArrayList<>();
        List<Float> confidences = new ArrayList<>();
        List<Rect2d> boxes = new ArrayList<>();

        for (Mat out : outs) {
            for (int i = 0; i < out.rows(); i++) {
                Mat row = out.row(i);
                Mat scores = row.colRange(5, out.cols());
                Core.MinMaxLocResult result = Core.minMaxLoc(scores);
                float confidence = (float) result.maxVal;
                Point classIdPoint = result.maxLoc;
                if (confidence > confThreshold) {
                    int centerX = (int) (row.get(0, 0)[0] * image.cols());
                    int centerY = (int) (row.get(0, 1)[0] * image.rows());
                    int width = (int) (row.get(0, 2)[0] * image.cols());
                    int height = (int) (row.get(0, 3)[0] * image.rows());
                    int left = centerX - width / 2;
                    int top = centerY - height / 2;

                    classIds.add((int) classIdPoint.x);
                    confidences.add(confidence);
                    boxes.add(new Rect2d(left, top, width, height));
                }
            }
        }

        // Apply non-maximum suppression only if there are any detections
        if (!confidences.isEmpty()) {
            MatOfFloat matConfidences = new MatOfFloat(Converters.vector_float_to_Mat(confidences));
            MatOfRect2d matBoxes = new MatOfRect2d(boxes.toArray(new Rect2d[0]));
            MatOfInt indices = new MatOfInt();
            Dnn.NMSBoxes(matBoxes, matConfidences, confThreshold, nmsThreshold, indices);

            // Filter the detections to include only the target classes and return the class names and boxes
            for (int idx : indices.toArray()) {
                int classId = classIds.get(idx);
                String className = getClassLabel(classId);
                if (targetClasses.contains(className)) {
                    detectedObjects.add(new DetectedObject(boxes.get(idx), className));
                }
            }
        }

        return detectedObjects;
    }

    private List<String> getOutputsNames(Net net) {
        List<String> names = new ArrayList<>();
        List<Integer> outLayers = net.getUnconnectedOutLayers().toList();
        List<String> layersNames = net.getLayerNames();

        for (int i = 0; i < outLayers.size(); i++) {
            names.add(layersNames.get(outLayers.get(i) - 1));
        }
        return names;
    }

    private String getClassLabel(int classId) {
        // Load the class labels file and return the corresponding class name
        List<String> classes = Arrays.asList("person", "bicycle", "car", "motorbike", "aeroplane",
                "bus", "train", "truck", "boat", "traffic light", "fire hydrant", "stop sign",
                "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
                "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag",
                "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite",
                "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket",
                "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana",
                "apple", "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza",
                "donut", "cake", "chair", "sofa", "pottedplant", "bed", "diningtable",
                "toilet", "tvmonitor", "laptop", "mouse", "remote", "keyboard", "cell phone",
                "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock",
                "vase", "scissors", "teddy bear", "hair drier", "toothbrush");
        return classes.get(classId);
    }
}
