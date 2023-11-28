package com.dji.sdk.sample.demo.kcgremotecontroller;

/*

here I want all the logic of the app to be
or at least the main managment

 */

import android.graphics.Bitmap;
import android.util.Log;

import com.dji.sdk.sample.demo.stitching.Stitching;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import dji.common.flightcontroller.adsb.AirSenseAirplaneState;

public class Controller implements gimbelListener {

    //data
    private DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private DecimalFormat dcF = new DecimalFormat("##.##");
    private double gimbelValue = 0;
    private GimbelController gimbelController;

    private KcgRemoteControllerView mainView;

    private FPVControll drone;

    private FlightControll_v2 flightControll; // הדבר המרכזי שמעניין אותנו

    //log vars
    private KcgLog log;
    private Map<String, Double> droneTelemetry;
    private Map<String, Double> controlStatus;

    private PlanesLog planesLog;


    private long t = System.currentTimeMillis();//used for fps count
    private int frameCounter = 0;
    private int displayFps = 0;

    private boolean isRecording = false;

    //constructor
    public Controller(KcgRemoteControllerView mainView) {
        this.mainView = mainView;
        //init log
        log = new KcgLog(this);
        planesLog = new PlanesLog(this);

        //init fpv
        drone = new FPVControll(this);
        drone.initPreviewer();

        //ini imbel ?
        gimbelController = new GimbelController();

//            flightControll = new FlightControll(this,drone.getMaxGimbalDegree(),drone.getMinGimbalDegree());
        flightControll = new FlightControll_v2(this, 640, 480, drone.getMaxGimbalDegree(), drone.getMinGimbalDegree());

        gimbelController.addListener(this);
        gimbelController.addListener(flightControll);
    }

    //functions
    public void showToast(String msg) {
        mainView.doToast(msg);
    }

    public void setVideoData(byte[] videoBuffer, int size) {

        //TODO here we should send the raw data to openCV
        mainView.setVideoData(videoBuffer, size);
    }

    public void setDescentRate(float descentRate) {
        flightControll.setDescentRate(descentRate);
    }

    public void setBitmapFrame(Bitmap bitmap) {

        if (t + 1000 < System.currentTimeMillis()) {
            t = System.currentTimeMillis();
            Log.i("arrk", "fps " + frameCounter);
            displayFps = frameCounter;
            frameCounter = 0;
        } else {
            frameCounter++;
        }

        float droneHeight = drone.getAlt();

        ControllCommand command = flightControll.proccessImage(bitmap, droneHeight);
        // החזירה פקודה
        if (command != null) {
            //display on screen data
            final String debug = "" + String.format("%.01f", command.confidence) + "," + displayFps + "," + droneHeight + "\n"
                    + "Err: " + String.format("%.01f", command.yError) + "," + String.format("%.01f", command.xError) + "," + String.format("%.01f", command.zError) + " || "
                    + "PRT: " + String.format("%.01f", command.getPitch()) + "," + String.format("%.01f", command.getRoll()) + "," + String.format("%.01f", command.getVerticalThrottle()) + "\n"
                    //+"PIDm: "+String.format("%.02f", command.p)+","+String.format("%.02f", command.i)+","+String.format("%.02f", command.d)+","+String.format("%.02f", command.maxI)+"\n"
                    //+"Auto: "+ DJISampleApplication.getAircraftInstance().getFlightController().isVirtualStickControlModeAvailable()+"\n"
                    + "Gimbal!: " + String.format("%.01f", gimbelValue) + " ImgD: " + String.format("%.01f", command.imageDistance);
//            command.getGimbalPitch()


            drone.setControlCommand(command); //This is the command that start the drone

            mainView.setDebugData(debug);
            //ugly way to overcome RunOnUiThread
            mainView.setRecIconVisibility(isRecording);
        }
    }

    public void setBitmapFrame(Bitmap bitmap, Stitching stitching) {

        if (t + 1000 < System.currentTimeMillis()) {
            t = System.currentTimeMillis();
            Log.i("arrk", "fps " + frameCounter);
            displayFps = frameCounter;
            frameCounter = 0;
        } else {
            frameCounter++;
        }

        float droneHeight = drone.getAlt();

        ControllCommand command = flightControll.proccessImage(bitmap, droneHeight, stitching);
        // החזירה פקודה
        if (command != null) {
            //display on screen data
            final String debug = "" + String.format("%.01f", command.confidence) + "," + displayFps + "," + droneHeight + "\n"
                    + "Err: " + String.format("%.01f", command.yError) + "," + String.format("%.01f", command.xError) + "," + String.format("%.01f", command.zError) + " || "
                    + "PRT: " + String.format("%.01f", command.getPitch()) + "," + String.format("%.01f", command.getRoll()) + "," + String.format("%.01f", command.getVerticalThrottle()) + "\n"
                    //+"PIDm: "+String.format("%.02f", command.p)+","+String.format("%.02f", command.i)+","+String.format("%.02f", command.d)+","+String.format("%.02f", command.maxI)+"\n"
                    //+"Auto: "+ DJISampleApplication.getAircraftInstance().getFlightController().isVirtualStickControlModeAvailable()+"\n"
                    + "Gimbal!: " + String.format("%.01f", gimbelValue) + " ImgD: " + String.format("%.01f", command.imageDistance);
//            command.getGimbalPitch()


            drone.setControlCommand(command); //This is the command that start the drone

            mainView.setDebugData(debug);
            //ugly way to overcome RunOnUiThread
            mainView.setRecIconVisibility(isRecording);
        }
    }

    public void initPIDs(double p, double i, double d, double max_i, String type) {
        flightControll.initPIDs(p, i, d, max_i, type);
    }

    //this function used by UI
    public double[] getPIDs(String type) {
        return flightControll.getPIDs(type);
    }

    /*
    Looks like this called when drone done some task,and may get error here
     */
    public void onDroneCompletionCallback(String text) {

        //TODO if there is an error (or maybe in each case), add this to some log
        mainView.onDroneCompletionCallback(text);
    }


    public void addPlanesLog(AirSenseAirplaneState[] planes) {
        planesLog.appendLog(planes);
    }

    public void addControlLog(Map<String, Double> controlStatus) {
        this.controlStatus = controlStatus;

        log.appendLog(droneTelemetry, controlStatus);
    }

    /*
    this is called by FPVControll, 10 times in sec
     */
    public void addTelemetryLog(Map<String, Double> droneTelemetry) {

        this.droneTelemetry = droneTelemetry;
//        writeLog();
    }

    public void updateRecordingStatus(boolean isRecording) {
        this.isRecording = isRecording;
    }

    public void allowControl() {
        drone.setControlAllowed(true);
    }

    public void stopOnPlace() {
        drone.stopOnPlace();
    }

    //TODO why this not called ?
    public void finish() {
        log.closeLog();
        planesLog.closeLog();

        drone.finish();
    }

    @Override
    public void updateGimbel(float gimbelValue) {
        this.gimbelValue = gimbelValue;
    }
}
