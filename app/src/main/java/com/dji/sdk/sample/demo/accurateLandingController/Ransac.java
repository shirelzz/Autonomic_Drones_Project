package com.dji.sdk.sample.demo.accurateLandingController;

import org.opencv.core.Point3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Ransac {

    private List<Point3> points;

    public Ransac(List<Point3> points) {
        this.points = points;
    }

    public boolean isPlane(double threshold) {
        int maxInliers = 0;
        List<Point3> bestInliers = new ArrayList<>();

        Random rand = new Random();

        int iterations = 1000;  // Number of iterations
        double distanceThreshold = 0.01;  // Distance threshold for inliers

        for (int i = 0; i < iterations; i++) {
            List<Point3> sample = getRandomSample(points, 3);

            // Fit a plane to the sample
            double[] plane = fitPlane(sample);

            // Find inliers
            List<Point3> inliers = new ArrayList<>();
            for (Point3 point : points) {
                if (distanceToPlane(point, plane) < distanceThreshold) {
                    inliers.add(point);
                }
            }

            // Update best inliers
            if (inliers.size() > maxInliers) {
                maxInliers = inliers.size();
                bestInliers = inliers;
            }
        }

        double inlierRatio = (double) maxInliers / points.size();
        System.out.println("Inlier Ratio: " + inlierRatio);

        // Visualization of inliers and outliers
        // You can use Android's Canvas or other visualization libraries to plot the inliers and outliers

        return inlierRatio > threshold;
    }

    private List<Point3> getRandomSample(List<Point3> points, int sampleSize) {
        List<Point3> sample = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < sampleSize; i++) {
            sample.add(points.get(rand.nextInt(points.size())));
        }
        return sample;
    }

    private double[] fitPlane(List<Point3> sample) {
        // Fit a plane to the sample points
        // Plane equation: Ax + By + Cz + D = 0
        Point3 p1 = sample.get(0);
        Point3 p2 = sample.get(1);
        Point3 p3 = sample.get(2);

        double A = (p2.y - p1.y) * (p3.z - p1.z) - (p2.z - p1.z) * (p3.y - p1.y);
        double B = (p2.z - p1.z) * (p3.x - p1.x) - (p2.x - p1.x) * (p3.z - p1.z);
        double C = (p2.x - p1.x) * (p3.y - p1.y) - (p2.y - p1.y) * (p3.x - p1.x);
        double D = -(A * p1.x + B * p1.y + C * p1.z);

        return new double[]{A, B, C, D};
    }

    private double distanceToPlane(Point3 point, double[] plane) {
        double A = plane[0];
        double B = plane[1];
        double C = plane[2];
        double D = plane[3];
        return Math.abs(A * point.x + B * point.y + C * point.z + D) / Math.sqrt(A * A + B * B + C * C);
    }
}

