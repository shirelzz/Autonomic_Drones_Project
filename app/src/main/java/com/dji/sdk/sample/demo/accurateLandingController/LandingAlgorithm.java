package com.dji.sdk.sample.demo.accurateLandingController;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import com.dji.sdk.sample.demo.kcgremotecontroller.VLD_PID;
import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;


public class LandingAlgorithm {

    private double targetDistance = 0.03;  // Target distance of 3 cm before the line
    private double currentVelocity = 0.01;  // Starting velocity
    private double maxVelocity = 1.0;  // Cap the maximum velocity

    private double cumulativeDistance = 0;  // Track the distance traveled
    private double minVelocity = 0.005;  // Minimum safe velocity
    private double accelerationRate = 0.02;  // Rate to increase velocity
    private double decelerationThreshold = 0.1;  // Distance where deceleration begins
    private double initialDistance = 0;  // Stores initial distance to the line

    private VLD_PID pitchPID;
    private VLD_PID throttlePID;

    // Constructor to initialize VLD_PID objects
    public LandingAlgorithm(VLD_PID pitchPID, VLD_PID throttlePID) {
        this.pitchPID = pitchPID;
        this.throttlePID = throttlePID;
    }


    public ControlCommand moveForward(double dt, Mat imgToProcess) {
        float pitchCommand = (float) pitchPID.update(0.05, dt, 1.0);  // Example constant speed forward
        Imgproc.putText(imgToProcess, "(1) Moving Forward", new Point(imgToProcess.cols() / 2.0, imgToProcess.rows() / 2.0),
                Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 0), 2);
        return new ControlCommand(pitchCommand, 0, 0);
    }

    public ControlCommand moveForwardByDistance(double distance, double dt, Mat imgToProcess) {

        // Decelerate as we approach the target
        if (distance > 0.03) {  // Stop when within a 5 cm tolerance
            float adjustedVelocity = (float) Math.min(distance, maxVelocity);
            showToast("adjustedVelocity: " + adjustedVelocity);
            pitchPID.reset();
            float pitchCommand = (float) adjustedVelocity;
            showToast("(2) Moving Forward" + distance);
            Imgproc.putText(imgToProcess, "(2) Moving Forward by " + distance, new Point(imgToProcess.cols() / 2.0, imgToProcess.rows() / 2.0),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 0), 2);
            return new ControlCommand(pitchCommand, 0, 0);
        } else {
            // Stop when within a 5 cm tolerance
            Imgproc.putText(imgToProcess, "(3) Stopping", new Point(imgToProcess.cols() / 2.0, imgToProcess.rows() / 2.0),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 0), 2);
            return new ControlCommand(0, 0, 0);
        }
    }

    public ControlCommand verticalAlign(double yOffset, double dt, Mat imgToProcess) {
        float throttleCommand = (float) throttlePID.update(yOffset, dt, 1.0);
        Imgproc.putText(imgToProcess, "Aligning Vertically", new Point(imgToProcess.cols() / 2.0, imgToProcess.rows() / 2.0),
                Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 0), 2);
        return new ControlCommand(0, 0, throttleCommand);
    }

    public ControlCommand approachLine(double dyReal, double dt, Mat imgToProcess) {

        if (initialDistance == 0) {
            initialDistance = Math.abs(dyReal);
        }

        double currentDistance = Math.abs(dyReal);
        if (currentDistance > targetDistance) {
            if (currentDistance > decelerationThreshold) {
                currentVelocity = Math.min(currentVelocity + accelerationRate, maxVelocity);
            } else {
                currentVelocity = Math.max(currentVelocity - accelerationRate, minVelocity);
            }

            float pitchCommand = (float) pitchPID.update(currentVelocity, dt, maxVelocity);
            float throttleCommand = (float) throttlePID.update(0, dt, maxVelocity);

            Imgproc.putText(imgToProcess, "Distance to Line: " + currentDistance, new Point(imgToProcess.cols() / 2.0, imgToProcess.rows() / 2.0),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 0), 2);

            return new ControlCommand(pitchCommand, 0, throttleCommand);
        } else {
            initialDistance = 0;  // Reset for next movement
            return new ControlCommand(0, 0, 0);  // Stop movement
        }
    }
}
