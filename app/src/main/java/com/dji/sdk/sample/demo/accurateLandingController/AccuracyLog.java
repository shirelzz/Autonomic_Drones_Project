package com.dji.sdk.sample.demo.accurateLandingController;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.dji.sdk.sample.internal.utils.ToastUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * Handles logging telemetry data and updating information on the screen.
 * Writes telemetry data to a CSV log file and displays relevant information on the provided TextView.
 */
public class AccuracyLog {

    private final String[] header = {"TimeMS", "date", "time", "Lat", "Lon", "Alt", "isUltrasonicBeingUsed", "altitudeBelow", "HeadDirection",/* "VelocityX", "VelocityY", "VelocityZ",*/ "yaw", "pitch", "roll", "GimbalPitch",
            "satelliteCount", "gpsSignalLevel", "batRemainingTime", "batCharge",
            "saw_target", "edgeX", "edgeY", "edgeDist", "PitchOutput", "RollOutput", "ThrottleOutput",
//            "ErrorX","ErrorY","Pp","Ip","Dp","Pr","Ir","Dr","Pt","It","Dt","MaxI",
            "AutonomousMode", "EdgeDetectionMode", "GuardianMode", "movementDetectionMessage", "LandingSteps", "isLanding"};
    @SuppressLint("SimpleDateFormat")
    private final DateFormat df = new SimpleDateFormat("dd/MM/yyyy , HH:mm:ss");
    private final DecimalFormat dcF = new DecimalFormat("##.####");
    TextView textViewLog, text;
    private BufferedWriter logFile;
    private final Context context;

    /**
     * Constructor initializing the log file and setting up the TextView for displaying information.
     *
     * @param textViewLog TextView to display telemetry information.
     */
    public AccuracyLog(TextView textViewLog, TextView text, Context context) {
        this.textViewLog = textViewLog;
        this.text = text;
        this.context = context;
        initLogFile();
    }

    /**
     * Initializes the log file by creating a new CSV file for logging telemetry data.
     */
    private void initLogFile() {
        File externalStorage = context.getExternalFilesDir(null);
        assert externalStorage != null;
        String fileName = externalStorage.getAbsolutePath() + "/DroneLog" + System.currentTimeMillis() + ".csv";
        File log = new File(fileName);

        try {
            if (!log.exists()) {
                boolean data = log.createNewFile();
                Log.i("AccuracyLog:", String.valueOf(data));
            }
        } catch (IOException e) {
            ToastUtils.showToast(e.getMessage());
            e.printStackTrace();
        }
        try {

            logFile = new BufferedWriter(new FileWriter(log.getAbsoluteFile()));
            logFile.write(String.join(",", header) + "\r\n");

        } catch (IOException e) {
            e.printStackTrace();
            text.setText(e.getMessage());

        }
    }

    public void closeLog() {
        try {
            logFile.flush();
            logFile.close();
            logFile = null;
            ToastUtils.showToast("Close Log");
        } catch (IOException e) {
            e.printStackTrace();
            ToastUtils.showToast(e.getMessage());
        }
    }

    public void appendLog(Map<String, Double> droneTelemetry, Map<String, Double> controlStatus, boolean isEdgeDetectionMode, boolean isMovementDetectionRunning, String movementDetectionMessage, boolean isLanding) {

        if (logFile == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        Date date = new Date();

        sb.append(System.currentTimeMillis()).append(",");
        sb.append(df.format(date)).append(",");

        try {
            //telemetry
            sb.append(droneTelemetry.get("lat")).append(",");
            sb.append(droneTelemetry.get("lon")).append(",");
            sb.append(droneTelemetry.get("alt")).append(",");
            sb.append(droneTelemetry.get("isUltrasonicBeingUsed")).append(",");
            sb.append(droneTelemetry.get("altitudeBelow")).append(",");
            sb.append(droneTelemetry.get("HeadDirection")).append(",");

            sb.append(format(droneTelemetry.get("yaw"))).append(",");
            sb.append(format(droneTelemetry.get("pitch"))).append(",");
            sb.append(format(droneTelemetry.get("roll"))).append(",");

            sb.append(format(droneTelemetry.get("gimbalPitch"))).append(",");

            sb.append(format(droneTelemetry.get("satelliteCount"))).append(",");
            sb.append(format(droneTelemetry.get("gpsSignalLevel"))).append(",");

            sb.append(format(droneTelemetry.get("batRemainingTime"))).append(",");
            sb.append(format(droneTelemetry.get("batCharge"))).append(",");
            sb.append(format(droneTelemetry.get("signalQuality")));

            if (controlStatus != null) {
                sb.append(format(controlStatus.get("saw_target"))).append(",");
                sb.append(format(controlStatus.get("edgeX"))).append(",");
                sb.append(format(controlStatus.get("edgeY"))).append(",");
                sb.append(format(controlStatus.get("edgeDist"))).append(",");

                sb.append(format(controlStatus.get("PitchOutput"))).append(",");
                sb.append(format(controlStatus.get("RollOutput"))).append(",");
                sb.append(format(controlStatus.get("ThrottleOutput"))).append(",");

                sb.append(format(controlStatus.get("AutonomousMode"))).append(",");
                sb.append(isEdgeDetectionMode).append(",");
                sb.append(isMovementDetectionRunning).append(",");
                sb.append(movementDetectionMessage).append(",");
                sb.append(format(controlStatus.get("LandingSteps"))).append(",");
                sb.append(isLanding).append(",");

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        sb.append("\r\n");

        try {
            logFile.write(sb.toString());
        } catch (IOException e) {
            text.setText(e.getMessage());
            e.printStackTrace();
        }
    }

    private String format(Double data) {
        if (data == null) {
            return "n/a";
        }

        String ans = "n/a";
        try {
            ans = dcF.format(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ans;
    }

    @SuppressLint("DefaultLocale")
    private void dataOnScreen(Map<String, Double> droneTelemetry) {
        StringBuilder debug = new StringBuilder();
        for (String key : droneTelemetry.keySet()) {
            debug.append(key).append(": ");
            if (Objects.equals(key, "lon") || Objects.equals(key, "lat") || Objects.equals(key, "alt")) {
                debug.append(droneTelemetry.get(key));
            } else {
                debug.append(String.format("%.01f", droneTelemetry.get(key)));
            }
            debug.append("  ,  ");
        }
        if (this.textViewLog != null)
            textViewLog.setText(debug.toString());
    }

    public void updateData(Map<String, Double> droneTelemetry, Map<String, Double> controlStatus, boolean isEdgeDetectionMode, boolean isMovementDetectionRunning, String movementDetectionMessage, boolean isLanding) {
        dataOnScreen(droneTelemetry);
        appendLog(droneTelemetry, controlStatus, isEdgeDetectionMode, isMovementDetectionRunning, movementDetectionMessage, isLanding);
    }

}
