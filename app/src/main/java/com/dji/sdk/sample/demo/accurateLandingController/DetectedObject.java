package com.dji.sdk.sample.demo.accurateLandingController;

import org.opencv.core.Rect2d;

public class DetectedObject {

    private Rect2d rect;
    private String className;

    public DetectedObject(Rect2d rect, String className) {
        this.rect = rect;
        this.className = className;
    }

    public Rect2d getRect() {
        return rect;
    }

    public String getClassName() {
        return className;
    }
}


