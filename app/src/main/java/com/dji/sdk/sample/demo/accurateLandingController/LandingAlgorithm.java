package com.dji.sdk.sample.demo.accurateLandingController;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import com.dji.sdk.sample.demo.kcgremotecontroller.VLD_PID;
import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;


public class LandingAlgorithm {

    private double maxVelocity = 1.0;  // Cap the maximum velocity

    private double initialDistance = 0;  // Stores initial distance to the line

    private VLD_PID pitchPID;

    public LandingAlgorithm(VLD_PID pitchPID) {
        this.pitchPID = pitchPID;
    }

    public ControlCommand moveForward(double dt) {
        float pitchCommand = (float) pitchPID.update(0.05, dt, 1.0);  // Constant speed forward
        return new ControlCommand(pitchCommand, 0, 0);
    }

    public ControlCommand moveForwardByDistance(double distance) {

        if (distance > 0.03) {  // Stop when within a 3 cm tolerance
            float adjustedVelocity = (float) Math.min(distance, maxVelocity);
            pitchPID.reset();
            return new ControlCommand(adjustedVelocity, 0, 0);
        } else {
            return new ControlCommand(0, 0, 0);
        }
    }

    public void resetDistance() {
        initialDistance = 0;
    }
}
