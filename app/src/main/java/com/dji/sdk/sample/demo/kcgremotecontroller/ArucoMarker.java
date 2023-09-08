package com.dji.sdk.sample.demo.kcgremotecontroller;



import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import dji.internal.camera.P;

/*
this class helps me to store aruco data, in a more pleasent way than OpenCV Mat
 */
public class ArucoMarker {

    //const

    //pixel to distance
    private static final double PIX_AT_METER = 350.0;
    private static final double ARUCO_10_SIZE = 50;
    private static final double ARUCO_20_SIZE = 16;
    private static final double ARUCO_20_CENTER_TO_CORNER = 0.11313;//SQRT(16*16+16*16)/2
    private static final double FOCAL_LENGTH = 450;
    private static final double PIX_FACTOR = ARUCO_20_CENTER_TO_CORNER * FOCAL_LENGTH;
    private static final double PIX_angle_FACTOR = ARUCO_20_SIZE * FOCAL_LENGTH;
    //data
    public Point p1,p2,p3,p4,center;
    public int id;

    private double dx,dy,dist,dist2,dist3;

    //constructor
    public ArucoMarker(int id, Mat corners){
        this.id = id;

        p1 = new Point(corners.get(0,0)[0],corners.get(0,0)[1]);
        p2 = new Point(corners.get(0,1)[0],corners.get(0,1)[1]);
        p3 = new Point(corners.get(0,2)[0],corners.get(0,2)[1]);
        p4 = new Point(corners.get(0,3)[0],corners.get(0,3)[1]);


        center = new Point(0,0);
        for(int j=0;j<corners.width();j++){
            center.x += corners.get(0,j)[0] / corners.width();
            center.y += corners.get(0,j)[1] / corners.width();
        }

    }

    public String toString(){
        return "["+p1.x+","+p1.y+"]"+"["+p2.x+","+p2.y+"]"+"["+p3.x+","+p3.y+"]"+"["+p4.x+","+p4.y+"]    c: ["+center.x+","+center.y+"]";
    }

    //functions

    public double approximateDistance(){
        double sum = 0;

        dx = p1.x - center.x;
        dy = p1.y - center.y;
        dist = Math.sqrt(dx*dx+dy*dy);
        sum = sum + dist;

        dx = p2.x - center.x;
        dy = p2.y - center.y;
        dist = Math.sqrt(dx*dx+dy*dy);
        sum = sum + dist;

        dx = p3.x - center.x;
        dy = p3.y - center.y;
        dist = Math.sqrt(dx*dx+dy*dy);
        sum = sum + dist;

        dx = p4.x - center.x;
        dy = p4.y - center.y;
        dist = Math.sqrt(dx*dx+dy*dy);
        sum = sum + dist;

        sum = sum / 4;

        //dist = aruco_size_to_center_m*focal_length/dist_in_pixel

        return PIX_FACTOR/sum;

//        double dismM = 0;
//        if (id == 10){
//            dismM = PIX_AT_METER * ARUCO_10_SIZE;
//        }
//        else{
//            dismM = PIX_AT_METER * ARUCO_20_SIZE;
//        }
//
//        double factor = dist/dismM;
//        double dd = 1.0 / factor;
//
////
//        return dx;
    }
//    public double approximateAngle() {
//
//
//        dx = p1.x - p4.x;
//        dy = p1.y - p4.y;
//        dist = Math.sqrt(dx*dx+dy*dy);
//
//        dx = p2.x -  p3.x;
//        dy = p2.y - p3.y;
//        dist2 = Math.sqrt(dx*dx+dy*dy);
//
//        dist3= (dist2-dist);
//
//        return dist3;
//
//    }


}
