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

import dji.common.flightcontroller.adsb.AirSenseAirplaneState;

public class PlanesLog {

    //data

    private DateFormat df= new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private DecimalFormat dcF = new DecimalFormat("##.##");

    private Controller controller;
    private BufferedWriter logFile;

    private String header="timestamp,code,distance,heading,relativeDirection,warningLevel \r\n";



    //constructor
    public PlanesLog(Controller controller){

        this.controller = controller;
        initLogFile();
    }

    //functions
    private void initLogFile(){
        File log = new File("sdcard/planesLog"+System.currentTimeMillis()+".txt");
                
        try {
            logFile = new BufferedWriter(new FileWriter(log));
            logFile.write(header);
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

    public void appendLog(AirSenseAirplaneState[] planes){

        if (logFile == null){ return;}

        if (planes.length == 0){ return;}

        StringBuilder sb = new StringBuilder();
        Date date = new Date();

        sb.append(System.currentTimeMillis()+",");
        sb.append(df.format(date) + ",");

        try {

            for (int i = 0; i < planes.length;i++){
                sb.append(System.currentTimeMillis() + ",");
                sb.append(planes[i].getCode() + ",");
                sb.append(planes[i].getDistance() + ",");
                sb.append(planes[i].getHeading() + ",");
                sb.append(planes[i].getRelativeDirection() + ",");
                sb.append(planes[i].getWarningLevel() + ",");
                sb.append("\r\n");
            }

        }
        catch (Exception e){
            e.printStackTrace();
        }


        try {
            logFile.write(sb.toString());
        }
        catch (IOException e){
            e.printStackTrace();

            controller.showToast(e.getMessage());
        }
    }

//    private String format(Double data){
//        if (data == null){ return "n/a";}
//
//        String ans = "n/a";
//        try {
//            ans = dcF.format(data);
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
//
//        return ans;
//
//    }
//

}
