package com.dji.sdk.sample.demo.kcgremotecontroller;

/*
here we will call for image processing
and perform some action
 */


/*
V2 of flight control. main goals:

1) increase resolution to 640*480
2) remove intrinsic matrix (as no needed)
3) remove Kalman filter, as it make more noise than needed

4) implements states
    1) overlook - from 4 meters
    2) hover over big target, descent slowly
    3) find small target, centralize
    4) descent in front of small target




 */

import android.graphics.Bitmap;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dji.sdk.sample.internal.controller.DJISampleApplication;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import dji.common.flightcontroller.virtualstick.VerticalControlMode;


//zzz


public class FlightControll_v2 implements gimbelListener{

    //const
    public static final int BIG_ARUCO_ID = 10;
    public static final int SMALL_ARUCO_ID = 20;
    public static boolean flag = false;

    //data

    private MyKalmanFilter[] kf_array_id_10 = new MyKalmanFilter[4];
    private MyKalmanFilter[] kf_array_id_20 = new MyKalmanFilter[4];

    private Mat[] predictedPoints_10 = new Mat[4];
    private Mat[] predictedPoints_20 = new Mat[4];

    private Mat temp_mat;

//    private ImageCoordinates imageCoordinates;

    private long prevTime = System.currentTimeMillis();



    // todo: a bug patching.
    Double PP=0.5, II = 0.02, DD = 0.01, MAX_I = 0.5;

    private VLD_PID roll_pid = new VLD_PID(PP,II,DD,MAX_I);
    private VLD_PID pitch_pid = new VLD_PID(PP,II,DD,MAX_I);
    private VLD_PID yaw_pid = new VLD_PID(PP,II,DD,MAX_I);
    private VLD_PID throttle_pid = new VLD_PID(PP,II,DD,MAX_I);
    private float p,r,t,gp= 0;
    //**
    private double gimbelValue = 0;
    //**

    private int frameHeight,frameWidth;
    private double error_x,error_y,error_z,error_yaw,D;

    private int framesConfidence = 0;

    private float descentRate = 0;
    private Controller controller;

    private float lastP=0,lastR=0;

    private Map<String,Double> controlStatus = new HashMap<>();

    private int maxGimbalDegree = 1000;
    private int minGimbalDegree = -1000;

    //aruco detection
    private Dictionary dictionary;
    private List<Mat> corners;
    private ArrayList<ArucoMarker> arucos = new ArrayList<>();

    private Scalar red,green,blue;

    private Queue<Double> errorQx = new LinkedList<Double>();
    private Queue<Double> errorQy = new LinkedList<Double>();
    private Queue<Double> errorQz = new LinkedList<Double>();



    //constructor
    public FlightControll_v2(Controller controller,int frameWidth,int frameHeight, int maxGimbal, int minGimbal){
        OpenCVLoader.initDebug();
        dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_50);

        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;


        temp_mat = new Mat();

        this.controller = controller;

        maxGimbalDegree = maxGimbal;
        minGimbalDegree = minGimbal;

        red = new Scalar(255,0,0);
        green = new Scalar(0,255,0);
        blue = new Scalar(0,0,255);
// gimbel assaf
        //gimbel2 = new GimbelController2(this);
//        initVision();
    }

    //functions

    public void initPIDs(double p,double i,double d,double max_i,String type){

        if (type.equals("roll")){
            if (roll_pid == null) {
                roll_pid = new VLD_PID(p, i, d, max_i);
            }
            else{
                roll_pid.setPID(p, i, d, max_i);
            }
        }

        if (type.equals("pitch")){
            if (pitch_pid == null) {
                pitch_pid = new VLD_PID(p, i, d, max_i);
            }
            else{
                pitch_pid.setPID(p, i, d, max_i);
            }
        }

        if (type.equals("throttle")){
            if (throttle_pid == null) {
                throttle_pid = new VLD_PID(p, i, d, max_i);
            }
            else{
                throttle_pid.setPID(p, i, d, max_i);
            }
        }


//        if (roll_pid == null) {
//            roll_pid = new VLD_PID(p, i, d, max_i);
//            pitch_pid = new VLD_PID(p, i, d, max_i);
//            throttle_pid = new VLD_PID(p, i, d, max_i);
//        }
//        else{
//            roll_pid.setPID(p, i, d, max_i);
//            pitch_pid.setPID(p, i, d, max_i);
//            throttle_pid.setPID(p, i, d, max_i);
//        }
    }

    public double[] getPIDs(String type){
        double[] ans = {-1,-1,-1};
        if (type.equals("roll")){
            if (roll_pid == null) {
               return ans;
            }
            else{
                ans[0] = roll_pid.getP();
                ans[1] = roll_pid.getI();
                ans[2] = roll_pid.getD();
                return ans;
            }
        }

        if (type.equals("pitch")){
            if (pitch_pid == null) {
                return ans;
            }
            else{
                ans[0] = pitch_pid.getP();
                ans[1] = pitch_pid.getI();
                ans[2] = pitch_pid.getD();
                return ans;
            }
        }

        if (type.equals("throttle")){
            if (throttle_pid == null) {
                return ans;
            }
            else{
                ans[0] = throttle_pid.getP();
                ans[1] = throttle_pid.getI();
                ans[2] = throttle_pid.getD();
                return ans;
            }
        }

        return ans;
    }

//    private void initVision(){
//        for(int i=0;i<kf_array_id_10.length;i++){
//            kf_array_id_10[i] = new MyKalmanFilter(2);
//        }
//        for(int i=0;i<kf_array_id_20.length;i++){
//            kf_array_id_20[i] = new MyKalmanFilter(2);
//        }
//        imageCoordinates =new ImageCoordinates(kf_array_id_10,kf_array_id_20);
//    }

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

//        Mat a = imageCoordinates.MarkerFinder(imgToProcess,aircraftHeight);
        ArrayList arucos = MarkerFinder(imgToProcess);

//        ControllCommand result = doKalman(arucos,aircraftHeight);
        ControllCommand result = flightLogic(arucos,aircraftHeight);



        //update the log with results
       updateLog(result,arucos);

        //TODO why this ??
        Utils.matToBitmap(imgToProcess,frame);

        return result;
        //#################################
    }

    //      pitch +  == forward
    //      pitch -  ==  back
    //      roll +  == right
    //      roll -  ==  left

    @RequiresApi(api = Build.VERSION_CODES.N)
    public ControllCommand flightLogic(ArrayList<ArucoMarker> arucos,float aircraftHeight){

        long currTime = System.currentTimeMillis();
        double dt = (currTime-prevTime) / 1000.0;
        prevTime = currTime;


         /*
            if no aruco - stay in place

            if only 10 (big) - hover above it

            if 20 - hover over 20

            land A - get close to 20 (1-2m)
            land B - descent to 20
            land C - fall


         */
        if (arucos.isEmpty()){ //nothing detected
            lowerConfidence(10);
            return stayOnPlace(aircraftHeight);
        }

        if (arucos.size() == 1 && arucos.get(0).id == BIG_ARUCO_ID){// only big seen
            framesConfidence = 30;
            return approachBig(arucos.get(0),dt,aircraftHeight);
        }

//        if (arucos.size() == 2 && (arucos.get(0).id == BIG_ARUCO_ID || arucos.get(1).id == SMALL_ARUCO_ID))//add dist to big
//        {
//            ArucoMarker small,big;
//            if (arucos.get(0).id == BIG_ARUCO_ID){
//                small = arucos.get(1);
//                big = arucos.get(0);
//            }
//            else{
//                small = arucos.get(0);
//                big = arucos.get(1);
//            }
//
//            upperConfidence(1);
//
//            if (framesConfidence < 85){
//                approachBig(big,dt,aircraftHeight);
//            }
//            else{
//                fixOnSmall(small,dt,aircraftHeight);
//            }
//        }

        if (arucos.size() == 1 && arucos.get(0).id == SMALL_ARUCO_ID)//add dist to big
        { // small seen
            framesConfidence = 100;
            return fixOnSmall(arucos.get(0),dt,aircraftHeight);

            //seek for 1m distance
            //move gimbel up to 0
            //center aruco in frame

            //when you get gimbal on 0-5 degrees
            //check your distance to ground, if less thatn 10cm - land
        }

        //default state


        return stayOnPlace(aircraftHeight);

    }

    public ControllCommand doKalman(ArrayList<ArucoMarker> arucos,float aircraftHeight) {


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
//        float size = 0.6f;
//        if(id_20_confidence < id_10_confidence || id_20_confidence < 5) {
//            points_array_to_use = predictedPoints_20;
//            min_confidence = id_20_confidence;
//            size = 0.1f;
//        }




//        if(aircraftHeight > 4) aircraftHeight = 4;
//        final Mat central_point = calcCentralPoint(points_array_to_use,aircraftHeight);
//        double imageDistance = approximateDistance(points_array_to_use,central_point,size);

        float gimbalDegree = calcGimbalDegree(arucos.get(0).approximateDistance());

        //final Mat target_vector = ImageCoordinates.invIntrinsicMatrix.mul(central_point.t());

        final Mat a = ImageCoordinates.invIntrinsicMatrix;
//        final Mat b = central_point;


//        Core.gemm(a,b,1,new Mat(),0,temp_mat);


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
//            ans.setImageDistance(imageDistance);

            // todo Should change mode for "Stop" (not "autonomuse")
            return ans;
        }
        if((min_confidence > 1000 && aircraftHeight <=2 && aircraftHeight > 0.3) ){
            roll_pid.reset();
            pitch_pid.reset();
            t = aircraftHeight;
            r = 0;
            p = 0;
//            y = 0;
            //gp = -30;
            ControllCommand ans = new ControllCommand(p,r,t,VerticalControlMode.POSITION,gp);
            ans.setErr(min_confidence,x_error,y_error,usAircraftHeight);
            ans.setPID(throttle_pid.getP(),throttle_pid.getI(),throttle_pid.getD(),pitch_pid.getP(),pitch_pid.getI(),pitch_pid.getD(),roll_pid.getP(),roll_pid.getI(),roll_pid.getD(),roll_pid.getMax_i());
//            ans.setImageDistance(imageDistance);

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
//            ans.setImageDistance(imageDistance);
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
//        ans.setImageDistance(imageDistance);
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

    public ArrayList<ArucoMarker> MarkerFinder(Mat image){

        Mat temp = image.clone();
        Imgproc.cvtColor(temp, temp, Imgproc.COLOR_BGR2GRAY);

        corners = new ArrayList<Mat>();
        Mat arucoIDs = new Mat();

        Aruco.detectMarkers(temp, dictionary,corners,arucoIDs);

        arucos.clear();

        // KF correction
        for(int i=0;i<arucoIDs.height();i++){
            ArucoMarker tmp = new ArucoMarker((int)arucoIDs.get(i,0)[0],corners.get(i));

            if (tmp.id == 10){
                Imgproc.drawMarker(image,tmp.center,green);
                Imgproc.line(image,tmp.p1,tmp.p2,green);
                Imgproc.line(image,tmp.p2,tmp.p3,green);
                Imgproc.line(image,tmp.p3,tmp.p4,green);
                Imgproc.line(image,tmp.p4,tmp.p1,green);
            }

            if (tmp.id == 20){
                Imgproc.drawMarker(image,tmp.center,blue);
                Imgproc.line(image,tmp.p1,tmp.p2,blue);
                Imgproc.line(image,tmp.p2,tmp.p3,blue);
                Imgproc.line(image,tmp.p3,tmp.p4,blue);
                Imgproc.line(image,tmp.p4,tmp.p1,blue);
            }

            Imgproc.line(image,new Point(320,0),new Point(320,480),blue);
            Imgproc.line(image,new Point(0,240),new Point(640,240),blue);

            Imgproc.putText(image, Double.toString(tmp.id),
                    tmp.center,
                    Imgproc.FONT_HERSHEY_PLAIN, 5, red);

            arucos.add(tmp);
        }
        return arucos;
    }

    private void updateLog(ControllCommand control,ArrayList<ArucoMarker> arucos){
    //TODO fix the log, uncoment what is missing

//        MarkerX,MarkerY,MarkerZ,PitchOutput,RollOutput,ErrorX,ErrorY,P,I,D,MaxI

//        controlStatus.put("saw_target",(double)(imageCoordinates.saw_target()? 1 : 0));
        controlStatus.put("saw_target",(double)arucos.size());
        if (arucos.size()> 0){
            controlStatus.put("MarkerX",arucos.get(0).center.x);
            controlStatus.put("MarkerY",arucos.get(0).center.y);
            controlStatus.put("MarkerDist",arucos.get(0).approximateDistance());
        }

        controlStatus.put("PitchOutput",(double)control.getPitch());
        controlStatus.put("RollOutput",(double)control.getRoll());

        controlStatus.put("ErrorX",control.xError);
        controlStatus.put("ErrorY",control.yError);

//        controlStatus.put("P",control.p);
//        controlStatus.put("I",control.i);
//        controlStatus.put("D",control.d);
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

        controlStatus.put("Throttle",(double)control.getVerticalThrottle());

        boolean autonomous_mode = DJISampleApplication.getAircraftInstance().getFlightController().isVirtualStickControlModeAvailable();
        controlStatus.put("autonomous_mode",(double)(autonomous_mode? 1 : 0));

        controller.addControlLog(controlStatus);
    }

    private void lowerConfidence(int num){
        framesConfidence = framesConfidence - num;
        framesConfidence = Math.max(0,framesConfidence);
    }

    private void upperConfidence(int num){
        framesConfidence = framesConfidence + num;
        framesConfidence = Math.min(100,framesConfidence);
    }

    private ControllCommand stayOnPlace(float aircraftHeight){
        roll_pid.reset();
        pitch_pid.reset();
        t = 0;
        r = 0;
        p = 0;
//        y = 0;
//        need to set another gp
//        set first gimbal angle
       gp = (float) gimbelValue;
        ControllCommand ans = new ControllCommand(p,r,t,VerticalControlMode.VELOCITY,gp);
        ans.setErr(1000,0,0,0);
        ans.setPID(throttle_pid.getP(),throttle_pid.getI(),throttle_pid.getD(),pitch_pid.getP(),pitch_pid.getI(),pitch_pid.getD(),roll_pid.getP(),roll_pid.getI(),roll_pid.getD(),roll_pid.getMax_i());
        ans.setImageDistance(-1);

        return ans;
    }

    //pitch = -10 => move reverse

    //y axis in frame - means pitch axis on drone

    private ControllCommand approachBig(ArucoMarker aruco, double dt, float aircraftHeight){
        double maxSpeed = 2;

        error_y = (frameHeight/2.0 - aruco.center.y)/100;
        error_x = (aruco.center.x - frameWidth/2.0)/100;

        p = (float) pitch_pid.update(error_y,dt,maxSpeed);
        r = (float) roll_pid.update(error_x,dt,maxSpeed);

//        gp = -200;
//        if (aircraftHeight > 1.5){
//            t = descentRate;//droneAlt;//
//        }
//        else{
//            t = 0;
//        }

        t = descentRate;


        ControllCommand ans = new ControllCommand(p,r,t,VerticalControlMode.VELOCITY,gp);
        ans.setErr(1000,error_x,error_y,aircraftHeight);
        ans.setPID(throttle_pid.getP(),throttle_pid.getI(),throttle_pid.getD(),pitch_pid.getP(),pitch_pid.getI(),pitch_pid.getD(),roll_pid.getP(),roll_pid.getI(),roll_pid.getD(),roll_pid.getMax_i());
        ans.setImageDistance(aruco.approximateDistance());
        return ans;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private ControllCommand fixOnSmall(ArucoMarker aruco, double dt, float aircraftHeight){
        double maxSpeed = 3;//aircraftHeight;
        double Distance;

        Distance = aruco.approximateDistance();
        if (flag) {
            D=1.0;}
        else{
            D=1.5;}
        error_x = (aruco.center.x - frameWidth/2.0)/100;
        error_z = (frameHeight/2.0 - aruco.center.y)/100;
        error_y = (Distance-D)*10;

//        error_yaw = (aruco.approximateDistance())/100;
        errorQx.add((error_x));
        errorQz.add((error_z));
        errorQy.add((error_y));
        while(errorQx.size() > 10 && errorQz.size() > 10 && errorQy.size() > 10){
            errorQx.poll();
            errorQz.poll();
            errorQy.poll();}

        double sum_x = errorQx.stream().reduce((double) 0,Double::sum)/ errorQx.size();
        double sum_z = errorQz.stream().reduce((double) 0,Double::sum)/ errorQz.size();
        double sum_y = errorQy.stream().reduce((double) 0,Double::sum)/ errorQy.size();

        if ( gp < -5 && sum_x < 1 && sum_z < 1 && sum_y < 2 ){
            gp = (float) (gp + 0.2);
           // error_z=error_z-(Distance*0.12/5)*2;
        }

        if (sum_x < 1 && sum_z < 1 && sum_y < 2 &&(gp < 20) && flag){
            gp = (float) (gp + 1);
            error_z=error_z-0.12;
            error_y=error_y-0.07;

        }

        r = (float) roll_pid.update(error_x,dt,maxSpeed);
        t = (float) throttle_pid.update(error_z,dt,maxSpeed);
        p = (float) pitch_pid.update(error_y,dt,maxSpeed);

//       if (Math.abs(error_y) < 0.3 && Math.abs(error_x) < 0.3 && Math.abs(error_z) < 0.3 && gp < 20){
//               gp = gp +1;
//
//       }


        if (aircraftHeight <= 0.3  && flag) {
            t = -3;
        }

//           DJISampleApplication.getAircraftInstance().getFlightController().startLanding(new CommonCallbacks.CompletionCallback() {
//               @Override
//               public void onResult(DJIError djiError) {
////                   DialogUtils.showDialogBasedOnError(getContext(), djiError);
//                   Log.i("ark",djiError.getDescription());
//               }
//           });
//       }


//        y = (float) yaw_pid.update(error_yaw,dt,maxSpeed);
//        if (descentRate == 0){
//            //we are in hover mode
//            t = (float) throttle_pid.update(error_z,dt,maxSpeed);
//        }
//        else{
//            //t = descentRate;
//            if (Math.abs(error_y) < 0.2 && gp < 5){
//                gp = gp +1;
//            }
//            else{
//                t = descentRate;
//            }
//        }




        ControllCommand ans = new ControllCommand(p,r,t,VerticalControlMode.VELOCITY,gp);
        ans.setErr(1000,error_x,error_y,error_z);
        ans.setPID(throttle_pid.getP(),throttle_pid.getI(),throttle_pid.getD(),pitch_pid.getP(),pitch_pid.getI(),pitch_pid.getD(),roll_pid.getP(),roll_pid.getI(),roll_pid.getD(),roll_pid.getMax_i());
        ans.setImageDistance(aruco.approximateDistance());
        return ans;
    }

    @Override
    public void updateGimbel(float gimbelValue) {
        this.gimbelValue = gimbelValue;
    }
}
