package com.dji.sdk.sample.demo.kcgremotecontroller;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.video.KalmanFilter;

import static org.opencv.core.CvType.CV_32F;

public class MyKalmanFilter {

    private KalmanFilter kf;

    private double dt=1;
    public double processNoiseCov_noise=0.05;
    public double measurementNoiseCov_noise=0.003;

    private int measSize = 8;
    private int stateSize = 2*measSize;
    private int contrSize = 0;

    private Mat measurementMatrix;
    private Mat transitionMatrix;
    private Mat processNoiseCov;
    private Mat measurementNoiseCov;


//    public MyKalmanFilter(){
//        kf=new KalmanFilter(stateSize,measSize,contrSize, CvType.CV_32F);
//        set_processNoiseCov();
//        set_measurementMatrix();
//        set_transitionMatrix();
//        set_measurementNoiseCov();
//    }

    public MyKalmanFilter(int measurements_dim){
        this.measSize=measurements_dim;
        this.stateSize=measurements_dim*2;
        kf = new KalmanFilter(stateSize,measSize,contrSize, CvType.CV_32F);
        set_processNoiseCov();
        set_measurementMatrix();
        set_transitionMatrix();
        set_measurementNoiseCov();
    }

//    public MyKalmanFilter(double processNoiseCov_noise,double measurementNoiseCov_noise){
//        this.processNoiseCov_noise=processNoiseCov_noise;
//        this.measurementNoiseCov_noise=measurementNoiseCov_noise;
//
//        kf=new KalmanFilter(stateSize,measSize,contrSize, CvType.CV_32F);
//        set_processNoiseCov();
//        set_measurementMatrix();
//        set_transitionMatrix();
//        set_measurementNoiseCov();
//    }

    public Mat predict(){
        return kf.predict();
    }

    public Mat getErrorCovPost(){
        return kf.get_errorCovPost();
    }

//    public Mat getProcessNoiseCov(){
//        return kf.get_processNoiseCov();
//    }
//
//    public Mat getMeasurementNoiseCov(){
//        return  kf.get_measurementNoiseCov();
//    }
//
//    public Mat getTransitionMatrix(){
//        return kf.get_transitionMatrix();
//    }
//    public Mat getTran(){ return kf.get_transitionMatrix();}

    public Mat correct(double [] m){
        Mat measurement=new Mat(measSize,1,CV_32F);
        for (int i=0; i< measSize ;i++)
            measurement.put(i,0,m[i]);
        Mat retVal = kf.correct(measurement);
        return retVal;
    }

//    public  Mat correct2(double a,double b){
//        Mat measurement=new Mat(measSize,1,CV_32F);
//        measurement.put(0,0,a);
//        measurement.put(1,0,b);
//        Mat retVal = kf.correct(measurement);
//        return retVal;
//    }

    private void set_processNoiseCov(){
        processNoiseCov=new Mat(stateSize,stateSize, CV_32F, Scalar.all(0));
        for (int i=0;i<stateSize;i++)
            processNoiseCov.put(i,i,1);
        Core.multiply(processNoiseCov,new Scalar(processNoiseCov_noise),processNoiseCov);
        kf.set_processNoiseCov(processNoiseCov);
    }

    private void set_measurementNoiseCov(){
        measurementNoiseCov=new Mat(measSize,measSize, CV_32F, Scalar.all(0));
        for (int i=0;i<measSize;i++)
            measurementNoiseCov.put(i,i,1);
        Core.multiply(measurementNoiseCov,new Scalar(measurementNoiseCov_noise),measurementNoiseCov);
        kf.set_measurementNoiseCov(measurementNoiseCov);
    }


    private void set_measurementMatrix(){
        measurementMatrix=new Mat(measSize, stateSize, CV_32F, Scalar.all(0));
        for (int i=0;i<measSize;i++)
            measurementMatrix.put(i,i,1);
        kf.set_measurementMatrix(measurementMatrix);
    }

    public void set_transitionMatrix(){

        transitionMatrix=new Mat(stateSize, stateSize, CV_32F, Scalar.all(0));
        for (int i=0;i<stateSize;i++)
            transitionMatrix.put(i, i, 1);
        int c=0;
        for (int i=measSize; i<stateSize; i++) {
            transitionMatrix.put(c, i, dt);
            c++;
        }
//        c=0;
//        for (int i=2*measSize; i<stateSize; i++) {
//            transitionMatrix.put(c, i, 0.5*Math.pow(dt,2));
//            c++;
//        }

        kf.set_transitionMatrix(transitionMatrix);
    }

    public void set_dt(double dt) {
        this.dt = dt;
        set_transitionMatrix();
    }
}
