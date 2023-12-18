package com.dji.sdk.sample.demo.accurateLandingController;

import android.annotation.SuppressLint;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles logging telemetry data and updating information on the screen.
 * Writes telemetry data to a CSV log file and displays relevant information on the provided TextView.
 */
public class AccuracyLog {

    private DateFormat df = new SimpleDateFormat("dd/MM/yyyy , HH:mm:ss");
    private DecimalFormat dcF = new DecimalFormat("##.####");
    private BufferedWriter logFile;

    private volatile String mode;

    private String header = "TimeMS,date,time,Lat,Lon,Alt,HeadDirection,VelocityX,VelocityY,VelocityZ,yaw,pitch,roll,GimbalPitch," + "batRemainingTime,batCharge"
//            + ",Real/kalman,MarkerX,MarkerY,MarkerZ,PitchOutput,RollOutput,ErrorX,ErrorY,Pp,Ip,Dp,Pr,Ir,Dr,Pt,It,Dt,MaxI,AutonomousMode"
            ;

    TextView textViewLog;

    /**
     * Constructor initializing the log file and setting up the TextView for displaying information.
     *
     * @param textViewLog TextView to display telemetry information.
     */
    public AccuracyLog(TextView textViewLog) {
        this.textViewLog = textViewLog;
        initLogFile();
    }

    /**
     * Initializes the log file by creating a new CSV file for logging telemetry data.
     */
    private void initLogFile() {
        File log = new File("sdcard/droneLog" + System.currentTimeMillis() + ".csv");

        try {
            logFile = new BufferedWriter(new FileWriter(log));
            logFile.write(header + "\r\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the operating mode for logging.
     *
     * @param new_mode Mode to be set for logging.
     */
    public void setMode(String new_mode) {
        try {
            mode = new_mode;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeLog() {
        try {
            logFile.flush();
            logFile.close();
            logFile = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //-----------------------

    public void appendLog(Map<String, Double> droneTelemetry
//            , Map<String,Double> controlStatus
    ) {

        if (logFile == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        Date date = new Date();

        sb.append(System.currentTimeMillis() + ",");
        sb.append(df.format(date) + ",");

        try {
            //telemetry
            sb.append(droneTelemetry.get("lat") + ",");
            sb.append(droneTelemetry.get("lon") + ",");
            sb.append(droneTelemetry.get("alt") + ",");
            sb.append(droneTelemetry.get("HeadDirection") + ",");

            sb.append(format(droneTelemetry.get("velX")) + ",");
            sb.append(format(droneTelemetry.get("velY")) + ",");
            sb.append(format(droneTelemetry.get("velZ")) + ",");

            sb.append(format(droneTelemetry.get("yaw")) + ",");
            sb.append(format(droneTelemetry.get("pitch")) + ",");
            sb.append(format(droneTelemetry.get("roll")) + ",");
//            sb.append(format(controlStatus.get("Throttle")) + ",");

            sb.append(format(droneTelemetry.get("gimbalPitch")) + ",");

            sb.append(format(droneTelemetry.get("batRemainingTime")) + ",");
            sb.append(format(droneTelemetry.get("batCharge")) + ",");

        } catch (Exception e) {
            e.printStackTrace();
        }
        sb.append("\r\n");

        try {
            logFile.write(sb.toString());
        } catch (IOException e) {
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
            debug.append(key).append(": ").append(String.format("%.01f", droneTelemetry.get(key)));
        }

        textViewLog.setText(debug.toString());
    }

    public void updateData(Map<String, Double> droneTelemetry) {
        dataOnScreen(droneTelemetry);
        appendLog(droneTelemetry);
    }

}
