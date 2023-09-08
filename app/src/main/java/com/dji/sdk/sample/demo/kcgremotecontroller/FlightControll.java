package com.dji.sdk.sample.demo.kcgremotecontroller;

/*
here we will call for image processing
and perform some action
 */

import android.graphics.Bitmap;

import com.dji.sdk.sample.internal.controller.DJISampleApplication;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.HashMap;
import java.util.Map;

import dji.common.flightcontroller.virtualstick.VerticalControlMode;

public class FlightControll {

    //data

    private MyKalmanFilter[] kf_array_id_10 = new MyKalmanFilter[4];
    private MyKalmanFilter[] kf_array_id_20 = new MyKalmanFilter[4];

    private Mat[] predictedPoints_10 = new Mat[4];
    private Mat[] predictedPoints_20 = new Mat[4];

    private Mat temp_mat;

    private ImageCoordinates imageCoordinates;

    private long prevTime = System.currentTimeMillis();

    //pixel to distance
    private static final double PIX_AT_METER = 350.0;

    // todo: a bug patching.
    Double PP=0.5, II = 0.02, DD = 0.01, MAX_I = 1.5;

    private VLD_PID roll_pid = new VLD_PID(PP,II,DD,MAX_I);
    private VLD_PID pitch_pid = new VLD_PID(PP,II,DD,MAX_I);
    private VLD_PID throttle_pid = new VLD_PID(1,0,0,0);

    private float descentRate = -1f;
    private Controller controller;

    private float lastP=0,lastR=0;

    private Map<String,Double> controlStatus = new HashMap<>();

    private int maxGimbalDegree = 1000;
    private int minGimbalDegree = -1000;

    //constructor
    public FlightControll(Controller controller, int maxGimbal,int minGimbal){

        OpenCVLoader.initDebug();
        temp_mat = new Mat();

        this.controller = controller;

        maxGimbalDegree = maxGimbal;
        minGimbalDegree = minGimbal;

        initVision();
    }

    //functions

    public void initPIDs(double p,double i,double d,double max_i){

        if (roll_pid == null) {
            roll_pid = new VLD_PID(p, i, d, max_i);
            pitch_pid = new VLD_PID(p, i, d, max_i);
            throttle_pid = new VLD_PID(p, i, d, max_i);
        }
        else{
            roll_pid.setPID(p, i, d, max_i);
            pitch_pid.setPID(p, i, d, max_i);
            throttle_pid.setPID(p, i, d, max_i);
        }
    }

    private void initVision(){
        for(int i=0;i<kf_array_id_10.length;i++){
            kf_array_id_10[i] = new MyKalmanFilter(2);
        }
        for(int i=0;i<kf_array_id_20.length;i++){
            kf_array_id_20[i] = new MyKalmanFilter(2);
        }
        imageCoordinates =new ImageCoordinates(kf_array_id_10,kf_array_id_20);
    }

    public void setDescentRate(float descentRate){
        if(descentRate > 0 ){
            descentRate = - descentRate;
        }

        this.descentRate = descentRate;
    }

    public ControllCommand proccessImage(Bitmap frame, float aircraftHeight){
        Mat imgToProcess=new Mat();
        Utils.bitmapToMat(frame,imgToProcess);

        //#### put proc###################

        Mat a = imageCoordinates.MarkerFinder(imgToProcess,aircraftHeight);
        ControllCommand result = doKalman(aircraftHeight);

        //update the log with results
        updateLog(result);

        //TODO why this ??
        Utils.matToBitmap(imgToProcess,frame);

        return result;
        //#################################
    }

    //      pitch +  == forward
    //      pitch -  ==  back
    //      roll +  == right
    //      roll -  ==  left


    public ControllCommand doKalman(float aircraftHeight) {

        float usAircraftHeight = aircraftHeight;

        long currTime = System.currentTimeMillis();
        double dt = (currTime-prevTime) / 1000.0;
        prevTime = currTime;

        predictedPoints_10 = predictKF(dt,kf_array_id_10,predictedPoints_10);
        predictedPoints_20 = predictKF(dt,kf_array_id_20,predictedPoints_20);

        Mat ncMat = kf_array_id_10[0].getErrorCovPost();
        Mat ncMat2 = kf_array_id_20[0].getErrorCovPost();

        final double id_10_confidence = Core.norm(ncMat);
        final double id_20_confidence = Core.norm(ncMat2);

        double min_confidence = id_10_confidence;

        Mat[] points_array_to_use = predictedPoints_10;
        float size = 0.6f;
        if(id_20_confidence < id_10_confidence || id_20_confidence < 5) {
            points_array_to_use = predictedPoints_20;
            min_confidence = id_20_confidence;
            size = 0.1f;
        }


//        if(aircraftHeight > 4) aircraftHeight = 4;
        final Mat central_point = calcCentralPoint(points_array_to_use,aircraftHeight);
        double imageDistance = approximateDistance(points_array_to_use,central_point,size);
        float gimbalDegree = calcGimbalDegree(imageDistance);

        //final Mat target_vector = ImageCoordinates.invIntrinsicMatrix.mul(central_point.t());

        final Mat a = ImageCoordinates.invIntrinsicMatrix;
        final Mat b = central_point;


        Core.gemm(a,b,1,new Mat(),0,temp_mat);


        double x_error = temp_mat.get(0,0)[0];
        double y_error = -temp_mat.get(1,0)[0];
//        double z_error = temp_mat.get(2,0)[0];


        //------ up to here, done with image proessing ?? -------


        //Pitch and roll are swapped : https://developer.dji.com/mobile-sdk/documentation/introduction/component-guide-flightController.html#virtual-sticks
//        float p = (float) roll_pid.update(x_error,dt,5.0);
//        float r = (float) pitch_pid.update(y_error,dt,5.0);
/**
 * The aircraftHeight paramter is bounded to [0.5,4] meter as it is used for max velocity.
 * This is a BUG as even in low height the drone still needs to flow a moving targt.
 *
 * */
      //  if(aircraftHeight > 4) {aircraftHeight = 4;}
       // if(aircraftHeight < ) {aircraftHeight = 1;}
      //  double NNN = 1.0;
      //  double min_speed = 2, max_Speed=4;
        double maxSpeed = 2;//aircraftHeight;
        if(aircraftHeight<2 && aircraftHeight>0.3) {
            double ah = Math.max(0.5,aircraftHeight);
            double NN = 1;
            if(min_confidence<10) {NN = 2/ah;}
            x_error *= NN;
            y_error *= NN;
        }
       // if(maxSpeed>max_Speed) {maxSpeed = max_Speed;}
       // if(maxSpeed<min_speed) {maxSpeed = min_speed;}
        float p = (float) roll_pid.update(x_error,dt,maxSpeed);
        float r = (float) pitch_pid.update(y_error,dt,maxSpeed);
        float t = descentRate;//droneAlt;//
        float gp = gimbalDegree;

//        float y = droneTelemetry.get("yaw").floatValue();
//        float y = drone.getYaw();
        // (float) throttle_pid.update(z_error,dt);

// todo Please log the following:pitch, roll, P,I,D, lat, lon, SOG, CoG, target pixel.


        if((min_confidence > 1000 && aircraftHeight > 2) ){
            roll_pid.reset();
            pitch_pid.reset();
            t = aircraftHeight;  // is it 5 meter?? if so sould be ~8
            r = 0;
            p = 0;
            //gp = -90;

            ControllCommand ans = new ControllCommand(p,r,t,VerticalControlMode.POSITION,gp);
            ans.setErr(min_confidence,x_error,y_error,usAircraftHeight);
            ans.setPID(throttle_pid.getP(),throttle_pid.getI(),throttle_pid.getD(),pitch_pid.getP(),pitch_pid.getI(),pitch_pid.getD(),roll_pid.getP(),roll_pid.getI(),roll_pid.getD(),roll_pid.getMax_i());
            ans.setImageDistance(imageDistance);

            // todo Should change mode for "Stop" (not "autonomuse")
            return ans;
        }
        if((min_confidence > 1000 && aircraftHeight <=2 && aircraftHeight > 0.3) ){
            roll_pid.reset();
            pitch_pid.reset();
            t = aircraftHeight;
            r = 0;
            p = 0;
            //gp = -30;
            ControllCommand ans = new ControllCommand(p,r,t,VerticalControlMode.POSITION,gp);
            ans.setErr(min_confidence,x_error,y_error,usAircraftHeight);
            ans.setPID(throttle_pid.getP(),throttle_pid.getI(),throttle_pid.getD(),pitch_pid.getP(),pitch_pid.getI(),pitch_pid.getD(),roll_pid.getP(),roll_pid.getI(),roll_pid.getD(),roll_pid.getMax_i());
            ans.setImageDistance(imageDistance);

            return ans;
        }

        if (min_confidence > 5 && aircraftHeight <= 0.3){
            t = -3;
            r = lastR;
            p = lastP;
            //gp = 5;
            ControllCommand ans = new ControllCommand(p,r,t,VerticalControlMode.VELOCITY,gp);
            ans.setErr(min_confidence,x_error,y_error,usAircraftHeight);
            ans.setPID(throttle_pid.getP(),throttle_pid.getI(),throttle_pid.getD(),pitch_pid.getP(),pitch_pid.getI(),pitch_pid.getD(),roll_pid.getP(),roll_pid.getI(),roll_pid.getD(),roll_pid.getMax_i());
            ans.setImageDistance(imageDistance);
            return ans;

        }
        float PP = 0.01f;
        if(lastP==0 && lastR == 0) {
            lastP = p;
            lastR = r;
        }
        else {
            lastP = p*PP +lastP*(1-PP);
            lastR = r*PP +lastR*(1-PP);

        }
   //     ControllCommand ans = new ControllCommand(p,r,t,VerticalControlMode.VELOCITY);
        ControllCommand ans = new ControllCommand(p,r,t,VerticalControlMode.VELOCITY,gp);
        ans.setErr(min_confidence,x_error,y_error,usAircraftHeight);
        ans.setPID(throttle_pid.getP(),throttle_pid.getI(),throttle_pid.getD(),pitch_pid.getP(),pitch_pid.getI(),pitch_pid.getD(),roll_pid.getP(),roll_pid.getI(),roll_pid.getD(),roll_pid.getMax_i());
        ans.setImageDistance(imageDistance);
        // Boaz 20.2
        return ans;
    }

    private Mat[] predictKF(double dt, MyKalmanFilter[] kf_array, Mat[] predictedPoints){

        for(int j=0 ; j < kf_array.length ; j++){
            kf_array[j].set_dt(dt);
            predictedPoints[j] = kf_array[j].predict();
        }
        return predictedPoints;
    }

    public Mat calcCentralPoint(Mat[] points_array, double distance){
        double x=0,y=0;

        Mat avg = new Mat(3,1, CvType.CV_32F);

        for(Mat point : points_array){
            x+= point.get(0,0)[0];
            y+= point.get(1,0)[0];
        }
        x/=points_array.length;
        y/=points_array.length;

        avg.put(0,0,x*distance);
        avg.put(1,0,y*distance);
        avg.put(2,0,distance);

        return avg;
    }

    public double approximateDistance(Mat points_array_to_use[],Mat central_point,float targetSizeM){
        double x0 = points_array_to_use[1].get(0,0)[0];
        double y0 = points_array_to_use[1].get(1,0)[0];
        double x1 = points_array_to_use[2].get(0,0)[0];
        double y1 = points_array_to_use[2].get(1,0)[0];

      //  double x1 = central_point.get(0,0)[0];
       // double y1 = central_point.get(1,0)[0];

        double dx = x1 - x0;
        double dy = y1 - y0;
        double dist = Math.sqrt(dx*dx+dy*dy);
       // return dist;
        double dismM = PIX_AT_METER * targetSizeM; // the expected number of pixels @ 1 meter
        double factor = dist/dismM;
        double dd = 1.0 / factor;
//
        return dd;
    }

    public float calcGimbalDegree(double distance2targetM){
        int min = minGimbalDegree;
        int max = maxGimbalDegree;
        double deltaAngle = maxGimbalDegree - minGimbalDegree;
        if (distance2targetM >= 2 ){ return minGimbalDegree;}
        if (distance2targetM <= 1 ){ return maxGimbalDegree;}
        double nr = distance2targetM - 1; // /(2-1); // (0,1)
        double d_angle = nr * deltaAngle;
        double angle = maxGimbalDegree - d_angle;
        return (float)angle;
    }

    private void updateLog(ControllCommand control){

//        MarkerX,MarkerY,MarkerZ,PitchOutput,RollOutput,ErrorX,ErrorY,P,I,D,MaxI

        controlStatus.put("saw_target",(double)(imageCoordinates.saw_target()? 1 : 0));
        controlStatus.put("MarkerX",imageCoordinates.getCoords()[0] / 100);
        controlStatus.put("MarkerY",imageCoordinates.getCoords()[1] / 100);
        controlStatus.put("MarkerZ",imageCoordinates.getCoords()[2] / 100);

        controlStatus.put("PitchOutput",(double)control.getPitch());
        controlStatus.put("RollOutput",(double)control.getRoll());

        controlStatus.put("ErrorX",control.xError);
        controlStatus.put("ErrorY",control.yError);

        controlStatus.put("Pp",control.p_pitch);
        controlStatus.put("Ip",control.i_pitch);
        controlStatus.put("Dp",control.d_pitch);
        controlStatus.put("Pr",control.p_roll);
        controlStatus.put("Ir",control.i_roll);
        controlStatus.put("Dr",control.d_roll);
        controlStatus.put("Pt",control.p_Throttle);
        controlStatus.put("It",control.i_Throttle);
        controlStatus.put("Dt",control.d_Throttle);
        controlStatus.put("maxI",control.maxI);

        boolean autonomous_mode = DJISampleApplication.getAircraftInstance().getFlightController().isVirtualStickControlModeAvailable();
        controlStatus.put("autonomous_mode",(double)(autonomous_mode? 1 : 0));

        controller.addControlLog(controlStatus);
    }
}
