package com.dji.sdk.sample.demo.kcgremotecontroller;

import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;


public class ImageCoordinates {

    private boolean saw_target=false;

    private List<Mat> corners;
    private boolean coocrect_kfPR=false;

    private double [] outCoords = new double[3];

    //    private Dictionary dictionary_big = Aruco.getPredefinedDictionary(Aruco.DICT_ARUCO_ORIGINAL);
    private Dictionary dictionary_small= Aruco.getPredefinedDictionary(Aruco.DICT_4X4_50);
    private Dictionary dictionary;

    public static double intrinsicMatrix_data[]={227.55056977,0.,160.6201764,
            0.,228.10869767,122.56785622,
            0.,0.,1.};
    public static double[] distCoeffs_data = {-0.02095438,-0.0278308,0.00145305,0.00061406,0.0414025};

    public static Mat intrinsicMatrix =null,invIntrinsicMatrix=null;
    private MatOfDouble distCoeffs=null;

    private MyKalmanFilter[] kf_array_id_10;
    private MyKalmanFilter[] kf_array_id_20;

    public ImageCoordinates(){

        dictionary = dictionary_small;

        initOpenCV();
        initIntrinsicMatrix();
    }

    public ImageCoordinates(MyKalmanFilter[] kf_array_id_10, MyKalmanFilter[] kf_array_id_20) {
        this();
        this.kf_array_id_10 = kf_array_id_10;
        this.kf_array_id_20 = kf_array_id_20;
    }


    private void initOpenCV(){
        if (OpenCVLoader.initDebug()){}
        else{}
    }

    private void initIntrinsicMatrix(){
        intrinsicMatrix = new Mat(3,3, CvType.CV_32FC1);
        intrinsicMatrix.put(0,0, intrinsicMatrix_data[0], intrinsicMatrix_data[1], intrinsicMatrix_data[2],
                intrinsicMatrix_data[3], intrinsicMatrix_data[4], intrinsicMatrix_data[5],
                intrinsicMatrix_data[6], intrinsicMatrix_data[7], intrinsicMatrix_data[8]);

        invIntrinsicMatrix = intrinsicMatrix.inv();
        distCoeffs = new MatOfDouble();
        distCoeffs.fromArray(distCoeffs_data);
    }

    public boolean saw_target(){ return saw_target; }

    public boolean saw_real_target(){return coocrect_kfPR;}

    public double [] getCoords(){
        return outCoords;
    }

    private void predictAndPrintKF(Mat image,MyKalmanFilter[] kf_array){
        try {
            for (int j = 1; j < kf_array.length + 1; j++) {
                int idx_1 = (j - 1) % kf_array.length;
                int idx_2 = (j) % kf_array.length;
                Point p1 = new Point(kf_array[idx_1].predict().get(0, 0)[0],
                        kf_array[idx_1].predict().get(1, 0)[0]);
                Point p2 = new Point(kf_array[idx_2].predict().get(0, 0)[0],
                        kf_array[idx_2].predict().get(1, 0)[0]);

                Imgproc.line(image, p1, p2, new Scalar(0, 255, 0));
            }
        }catch (Exception e){}
    }

    public Mat MarkerFinder(Mat image,double height){

        Log.d("ark","image size: "+image.width()+" , "+image.height());

        Mat temp = image.clone();
        corners = new ArrayList<Mat>();

        Mat arucoIDs = new Mat();

        Imgproc.cvtColor(temp, temp, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(image,image,Imgproc.COLOR_RGBA2RGB);

        Mat undistoredImage = new Mat(image.height(),image.width(),image.type());

        Calib3d.undistort(image,undistoredImage,intrinsicMatrix,distCoeffs);

        //image = undistoredImage;

        undistoredImage.copyTo(image);

        predictAndPrintKF(image,kf_array_id_10);
        predictAndPrintKF(image,kf_array_id_20);

        Aruco.detectMarkers(temp, dictionary,corners,arucoIDs);

        // KF correction
        for(int i=0;i<arucoIDs.height();i++){
            saw_target=true;
            double id = arucoIDs.get(i,0)[0];
            double x=0,y=0;
            for(int j=0;j<corners.get(i).width();j++){
                x += corners.get(i).get(0,j)[0] / corners.get(i).width();
                y += corners.get(i).get(0,j)[1] / corners.get(i).width();
                if(id == 10){
                    kf_array_id_10[j].correct(corners.get(i).get(0,j));
                }
                if(id == 20){
                    kf_array_id_20[j].correct(corners.get(i).get(0,j));
                }
            }


            if (id == 10){
                Imgproc.drawMarker(image,new Point(x,y),new Scalar(0,255,0));
            }
            else{
                Imgproc.drawMarker(image,new Point(x,y),new Scalar(0,0,255));
            }
//            Imgproc.drawMarker(image,new Point(x,y),new Scalar(0,255,0));
            Imgproc.putText(image, Double.toString(id),
                    new Point(x,y),
                    Imgproc.FONT_HERSHEY_PLAIN, 2, new Scalar(255,0,0));
        }


//        if(corners.size() > 0 )
//            return corners.get(0);
//        return new Mat();
//        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);

        return arucoIDs;
    }



}



