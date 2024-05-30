package com.dji.sdk.sample.demo.accurateLandingController;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class ObjectTracking {
    private boolean firstFrame = true;
    private boolean modeFlag;
    private DayMode mode;
    private Point position;
    private Mat frame;
    private Rect lastTimeCenter;
    private Rect lastTarget;
    private Boolean colorDetection;
    private String color;

    public ObjectTracking(Boolean colorDetection, String color) {
        this.colorDetection = colorDetection;
        this.color = color;
    }

    public Point track(Mat fr, int state) {
        this.frame = fr;

        if (firstFrame) {
            modeFlag = nightModeCheck();
            firstFrame = false;
        }


        position = mode.dayAction(frame, state);

        GUI(state);

        return position;
    }

    private boolean nightModeCheck() {
        Mat blur = new Mat();
        Imgproc.blur(frame, blur, new org.opencv.core.Size(5, 5));
        double[] mean = Core.mean(blur).val;
        mode = new DayMode(colorDetection, color);
        System.out.println("Day Mode");
        return false;
    }

    private void GUI(int state) {
        int centerX = (int) position.x;
        int centerY = (int) position.y;
        int hc = frame.rows();
        int wc = frame.cols();

        int fX = wc / 2;
        int fY = hc / 2;

        if (centerX >= 15 && centerY >= 15) {
            Rect box = mode.getBox();
            drawBox(box);
            boolean limit = checkArrowBound(box.tl(), box.br(), new org.opencv.core.Point(fX, fY));
            Imgproc.putText(frame, "TRACKING!", new org.opencv.core.Point(5, 45), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5,
                    new Scalar(0, 255, 0));

            lastTarget = new Rect(centerX, centerY, 0, 0);

            if (!limit) {
                Imgproc.circle(frame, new org.opencv.core.Point(fX, fY), 2, new Scalar(0, 0, 255), 4);
                int X = (int) (centerX + (fX - centerX) * 0.5);
                int Y = (int) (centerY + (fY - centerY) * 0.5);
                Rect center = new Rect(X, Y, 0, 0);
                lastTimeCenter = center;
                Imgproc.arrowedLine(frame, new org.opencv.core.Point(fX, fY), new org.opencv.core.Point(centerX, centerY),
                        new Scalar(0, 0, 255), 2, 0, 1);
                suggestDirection(fX, fY, centerX, centerY);
                if (state == 90 || state == 122) {
                    zoomInObject(box, wc);
                }
            } else {
                Imgproc.putText(frame, "ON TARGET!", new org.opencv.core.Point(5, 70), Imgproc.FONT_HERSHEY_SIMPLEX,
                        0.5, new Scalar(255, 0, 0));
                zoomInObject(box, wc);
            }
        } else {
            if (lastTimeCenter != null) {
                Imgproc.arrowedLine(frame, new org.opencv.core.Point(fX, fY),
                        new org.opencv.core.Point(lastTimeCenter.x, lastTimeCenter.y), new Scalar(0, 0, 255), 2, 0, 1);
                if (lastTarget != null) {
                    suggestDirection(fX, fY, lastTarget.x, lastTarget.y);
                }
            }
            Imgproc.putText(frame, "LOST!", new org.opencv.core.Point(5, 45), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5,
                    new Scalar(0, 0, 255));
        }
    }

    private boolean checkArrowBound(org.opencv.core.Point bl, org.opencv.core.Point tr, org.opencv.core.Point p) {
        return bl.x < p.x && p.x < tr.x && bl.y < p.y && p.y < tr.y;
    }

    private void drawBox(Rect box) {
        Imgproc.rectangle(frame, box.tl(), box.br(), new Scalar(0, 255, 0), 3, 3);
    }

    private void suggestDirection(int fX, int fY, int centerX, int centerY) {
        if (centerX < fX - 10 && centerY < fY - 10) {
            Imgproc.putText(frame, "Up-Left", new org.opencv.core.Point(5, 70), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5,
                    new Scalar(0, 0, 255));
        }
        // Add more direction suggestions similarly
    }

    private void zoomInObject(Rect box, int wc) {
        Rect cropRect = new Rect(box.tl(), box.br());
        Mat cropFrame = new Mat(frame, cropRect);

        double scalePercent = 300;
        int width = (int) (cropFrame.cols() * scalePercent / 100);
        int height = (int) (cropFrame.rows() * scalePercent / 100);

        Mat resizedFrame = new Mat();
        Imgproc.resize(cropFrame, resizedFrame, new org.opencv.core.Size(width, height));

        Mat foreground = Mat.ones(new org.opencv.core.Size(width, height), resizedFrame.type());
        double alpha = 1.0;

        Mat addedImage = new Mat();
        Core.addWeighted(resizedFrame.submat(0, height, 0, width), alpha,
                foreground.submat(0, height, 0, width), 1 - alpha, 0, addedImage);

        addedImage.copyTo(frame.submat(0, height, wc - width, wc + width));
    }
}
