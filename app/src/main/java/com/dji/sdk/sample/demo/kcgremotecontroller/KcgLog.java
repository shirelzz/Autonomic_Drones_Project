package com.dji.sdk.sample.demo.kcgremotecontroller;

/*

All log lines should get here
 */


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class KcgLog {

    //data

    private DateFormat df= new SimpleDateFormat("dd/MM/yyyy , HH:mm:ss");
    private DecimalFormat dcF = new DecimalFormat("##.####");

    private Controller controller;
    private BufferedWriter logFile;

    private String header="TimeMS,date,time,Lat,Lon,Alt,HeadDirection,VelocityX,VelocityY,VelocityZ,yaw,pitch,roll,Throttle,UsAlt,GimbalPitch," +
                "batRemainingTime,batCharge,Real/kalman,MarkerX,MarkerY,MarkerZ,PitchOutput,RollOutput,ErrorX,ErrorY,Pp,Ip,Dp,Pr,Ir,Dr,Pt,It,Dt,MaxI,AutonomousMode";





    //constructor
    public KcgLog(Controller controller){

        this.controller = controller;
        initLogFile();
    }

    //functions
    private void initLogFile(){
        File log = new File("sdcard/droneLog"+System.currentTimeMillis()+".csv");

        try {
            logFile = new BufferedWriter(new FileWriter(log));
            logFile.write(header+"\r\n");
        }
        catch (IOException e){
            e.printStackTrace();

            controller.showToast(e.getMessage());
        }
    }


    public void closeLog(){
        try {
            logFile.flush();
            logFile.close();
            logFile = null;
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }


    //-----------------------

    public void appendLog(Map<String,Double> droneTelemetry,Map<String,Double> controlStatus){

        if (logFile == null){ return;}

        StringBuilder sb = new StringBuilder();
        Date date = new Date();

        sb.append(System.currentTimeMillis()+",");
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
            sb.append(format(controlStatus.get("Throttle")) + ",");

            sb.append(format(droneTelemetry.get("UsAlt")) + ",");

            sb.append(format(droneTelemetry.get("gimbalPitch")) + ",");

            sb.append(format(droneTelemetry.get("batRemainingTime")) + ",");
            sb.append(format(droneTelemetry.get("batCharge")) + ",");

            //control

            sb.append(format(controlStatus.get("saw_target")) + ",");
            sb.append(format(controlStatus.get("MarkerX")) + ",");
            sb.append(format(controlStatus.get("MarkerY")) + ",");
            sb.append(format(controlStatus.get("MarkerDist")) + ",");

            sb.append(format(controlStatus.get("PitchOutput")) + ",");
            sb.append(format(controlStatus.get("RollOutput")) + ",");

            sb.append(format(controlStatus.get("ErrorX")) + ",");
            sb.append(format(controlStatus.get("ErrorY")) + ",");

            sb.append(format(controlStatus.get("Pp")) + ",");
            sb.append(format(controlStatus.get("Ip")) + ",");
            sb.append(format(controlStatus.get("Dp")) + ",");
            sb.append(format(controlStatus.get("Pr")) + ",");
            sb.append(format(controlStatus.get("Ir")) + ",");
            sb.append(format(controlStatus.get("Dr")) + ",");
            sb.append(format(controlStatus.get("Pt")) + ",");
            sb.append(format(controlStatus.get("It")) + ",");
            sb.append(format(controlStatus.get("Dt")) + ",");
            sb.append(format(controlStatus.get("maxI")) + ",");

            sb.append(format(controlStatus.get("autonomous_mode")) + ",");



        }
        catch (Exception e){
            e.printStackTrace();
        }


        sb.append("\r\n");


        try {
            logFile.write(sb.toString());
        }
        catch (IOException e){
            e.printStackTrace();

            controller.showToast(e.getMessage());
        }
    }

    private String format(Double data){
        if (data == null){ return "n/a";}

        String ans = "n/a";
        try {
            ans = dcF.format(data);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return ans;

    }




}
