package com.dji.sdk.sample.demo.accurateLandingController;

import org.opencv.core.Core;
import org.opencv.core.Mat;

public class DericheEdgeDetection {

    private static final double ALPHA = 1.0; // Example alpha value

    // Recursive Deriche filter in horizontal direction
    private static Mat applyHorizontalDeriche(Mat input, double alpha) {
        Mat output = Mat.zeros(input.size(), input.type());
        int rows = input.rows();
        int cols = input.cols();

        for (int i = 0; i < rows; i++) {
            double[] previous = input.get(i, 0);
            double[] current = new double[cols];

            for (int j = 0; j < cols; j++) {
                if (j == 0) {
                    current[j] = previous[0];
                } else {
                    double[] prevValue = input.get(i, j - 1);
                    current[j] = alpha * prevValue[0] + (1 - alpha) * previous[j];
                }
            }
            output.put(i, 0, current);
        }
        return output;
    }

    // Recursive Deriche filter in vertical direction
    private static Mat applyVerticalDeriche(Mat input, double alpha) {
        Mat output = Mat.zeros(input.size(), input.type());
        int rows = input.rows();
        int cols = input.cols();

        for (int j = 0; j < cols; j++) {
            double[] previous = input.get(0, j);
            double[] current = new double[rows];

            for (int i = 0; i < rows; i++) {
                if (i == 0) {
                    current[i] = previous[0];
                } else {
                    double[] prevValue = input.get(i - 1, j);
                    current[i] = alpha * prevValue[0] + (1 - alpha) * previous[i];
                }
            }
            output.put(0, j, current);
        }
        return output;
    }

    // Full Deriche edge detection process
    public static Mat detectEdges(Mat input, double alpha) {
        // Step 1: Apply Deriche filter in horizontal direction
        Mat horizontalFiltered = applyHorizontalDeriche(input, alpha);

        // Step 2: Apply Deriche filter in vertical direction
        Mat verticalFiltered = applyVerticalDeriche(horizontalFiltered, alpha);

        // Step 3: Compute Gradient Magnitude (hypot function)
        Mat gradientMagnitude = new Mat();
        Core.magnitude(horizontalFiltered, verticalFiltered, gradientMagnitude);

        // Return the final edge-detected image
        return gradientMagnitude;
    }
}
