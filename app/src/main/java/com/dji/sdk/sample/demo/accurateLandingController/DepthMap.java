package com.dji.sdk.sample.demo.accurateLandingController;

import org.opencv.calib3d.Calib3d;
import org.opencv.calib3d.StereoBM;
import org.opencv.calib3d.StereoSGBM;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point3;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class DepthMap {

    private Mat imgLeft = null;
    private Mat imgRight = null;

    public DepthMap() {
        imgLeft = null;
        imgRight = null;
    }

    public boolean AddImage(Mat image) {
        if (this.imgLeft == null || this.imgLeft.empty()) {
            this.imgLeft = image;
            return false;
        }
        this.imgRight = image;

        if (this.imgLeft.empty() || this.imgRight.empty()) {
            throw new IllegalArgumentException("One or both images could not be read. Check file paths and formats.");
        }

        if (this.imgLeft.size() != this.imgRight.size()) {
            Imgproc.resize(this.imgLeft, this.imgLeft, this.imgRight.size());
        }
        return demoStereoSGBM();
    }

    public Mat computeDepthMapBM() {
        Mat disparity = new Mat();
        Mat leftForBM = new Mat();
        Mat rightForBM = new Mat();
        Imgproc.equalizeHist(imgLeft, leftForBM);
        Imgproc.equalizeHist(imgRight, rightForBM);

        StereoBM stereoBM = StereoBM.create(16 * 12, 21);
        stereoBM.compute(leftForBM, rightForBM, disparity);

        disparity.convertTo(disparity, CvType.CV_32F, 1.0 / 16.0);

        // Handle disparity map visualization using Android's ImageView
        return disparity;
    }

    public Mat computeDepthMapSGBM() {
        Mat disparity = new Mat();
        Mat leftForSGBM = new Mat();
        Mat rightForSGBM = new Mat();
        Imgproc.equalizeHist(imgLeft, leftForSGBM);
        Imgproc.equalizeHist(imgRight, rightForSGBM);

        StereoSGBM stereoSGBM = StereoSGBM.create(
                16, // minDisparity
                16 * 14 - 16, // numDisparities
                7 // blockSize
        );
        stereoSGBM.setP1(8 * 3 * 7 * 7);
        stereoSGBM.setP2(32 * 3 * 7 * 7);
        stereoSGBM.setDisp12MaxDiff(1);
        stereoSGBM.setUniquenessRatio(15);
        stereoSGBM.setSpeckleWindowSize(0);
        stereoSGBM.setSpeckleRange(2);
        stereoSGBM.setPreFilterCap(63);
        stereoSGBM.setMode(StereoSGBM.MODE_SGBM_3WAY);

        stereoSGBM.compute(leftForSGBM, rightForSGBM, disparity);

        disparity.convertTo(disparity, CvType.CV_32F, 1.0 / 16.0);

        // Handle disparity map visualization using Android's ImageView
        return disparity;
    }

    public List<Point3> computePointCloud(Mat disparity, Mat Q) {
        Mat points3D = new Mat();
        Calib3d.reprojectImageTo3D(disparity, points3D, Q);

        // Get the minimum disparity value
        Core.MinMaxLocResult minMaxLocResult = Core.minMaxLoc(disparity);
        double minDisparity = minMaxLocResult.minVal;

        List<Point3> pointList = new ArrayList<>();
        for (int i = 0; i < points3D.rows(); i++) {
            for (int j = 0; j < points3D.cols(); j++) {
                double[] vec = points3D.get(i, j);
                double disparityValue = disparity.get(i, j)[0];

                if (disparityValue > minDisparity &&
                        !Double.isInfinite(vec[0]) && !Double.isNaN(vec[0]) &&
                        !Double.isInfinite(vec[1]) && !Double.isNaN(vec[1]) &&
                        !Double.isInfinite(vec[2]) && !Double.isNaN(vec[2])) {
                    pointList.add(new Point3(vec[0], vec[1], vec[2]));
                }
            }
        }
        return pointList;
    }

    public boolean isPlane(List<Point3> points) {
        Ransac ransac = new Ransac(points);
        return ransac.isPlane(0.8);
    }

    public boolean demoStereoBM() {
        Mat disparity = computeDepthMapBM();
        Mat Q = Mat.eye(4, 4, CvType.CV_64F);
        Q.put(0, 3, -imgLeft.size().width / 2);
        Q.put(1, 3, imgLeft.size().height / 2);
        Q.put(2, 3, -1);
        Q.put(3, 2, 1);
        List<Point3> points3D = computePointCloud(disparity, Q);
        if (isPlane(points3D)) {
            System.out.println("The scene is a plane (StereoBM).");
            return true;
        } else {
            System.out.println("The scene is not a plane (StereoBM).");
            return false;
        }
    }

    public boolean demoStereoSGBM() {
        Mat disparity = computeDepthMapSGBM();
        Mat Q = Mat.eye(4, 4, CvType.CV_64F);
        Q.put(0, 3, -imgLeft.size().width / 2);
        Q.put(1, 3, imgLeft.size().height / 2);
        Q.put(2, 3, -1);
        Q.put(3, 2, 1);
        List<Point3> points3D = computePointCloud(disparity, Q);
        if (isPlane(points3D)) {
            System.out.println("The scene is a plane (StereoSGBM).");
            return true;
        } else {
            System.out.println("The scene is not a plane (StereoSGBM).");
            return false;
        }
    }

//    public static void main(String[] args) {
//        DepthMap depthMap = new DepthMap(true);
//        boolean value = depthMap.demoStereoBM();
//        value = depthMap.demoStereoSGBM();
//    }
}
