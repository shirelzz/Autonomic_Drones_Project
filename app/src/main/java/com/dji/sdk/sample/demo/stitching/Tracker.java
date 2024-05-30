//package com.dji.sdk.sample.demo.stitching;
//
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//
//import boofcv.abst.tracker.ConfigComaniciu2003;
//import boofcv.abst.tracker.ConfigTrackerTld;
//import boofcv.abst.tracker.MeanShiftLikelihoodType;
//import boofcv.abst.tracker.TrackerObjectQuad;
//import boofcv.factory.tracker.FactoryTrackerObjectQuad;
//import boofcv.struct.image.*;
//import dji.common.flightcontroller.virtualstick.FlightControlData;
//import dji.sdk.flightcontroller.FlightController;
//import georegression.struct.point.Point2D_F64;
//import georegression.struct.shapes.Quadrilateral_F64;
//
//public class Tracker {
//
//
//    private static final double PITCH_SCALING_FACTOR = 0.01;
//    private static final double ROLL_SCALING_FACTOR = 0.01;
//    private boolean gotFirstImage;
//    private final String ALG;
//    private TrackerObjectQuad tracker;
//    private int size;
//    private Quadrilateral_F64 location;
//    private Point2D_F64 screenCen;
//    private boolean isPaused;
//    private double initialLatitude;
//    private double initialLongitude;
//    private FlightController flightController;
//
//    public Tracker() {
//        this("sparseFlow", 50);
//    }
//
//    public Tracker(String algorithm) {
//        this(algorithm, 50);
//    }
//
//    public Tracker(String algorithm, int sizeInput) {
//        gotFirstImage = false;
//        ALG = algorithm;
//        size = sizeInput;
//    }
//
//    public Tracker(ImageBase image) {
//        this(image, "sparseFlow");
//    }
//
//    public Tracker(ImageBase image, String algorithm) {
//        ALG = algorithm;
//        gotFirstImage = true;
//        init(image);
//    }
//
//
//    private void init(ImageBase frame) {
//        tracker = FactoryTrackerObjectQuad.sparseFlow(null, GrayU8.class, null);
//
//        switch (ALG) {
//            case "circulant":
//                tracker = FactoryTrackerObjectQuad.circulant(null, GrayU8.class);
//                break;
//            case "sparseFlow":
//                tracker = FactoryTrackerObjectQuad.sparseFlow(null, GrayU8.class, null);
//                break;
//            case "tld":
//                tracker = FactoryTrackerObjectQuad.tld(new ConfigTrackerTld(false), GrayU8.class);
//                break;
//            case "meanShiftComaniciu2003":
//                tracker = FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(), ImageType.pl(3, GrayU8.class));
//                break;
//            case "meanShiftLikelihood":
//                tracker = FactoryTrackerObjectQuad.meanShiftLikelihood(30, 5, 255, MeanShiftLikelihoodType.HISTOGRAM, ImageType.pl(3, GrayU8.class));
//                break;
//        }
//
//        // Ensure the frame dimensions match the expectations of the tracker
//        int width = frame.getWidth();
//        int height = frame.getHeight();
//
//        // Reshape the frame to match the desired dimensions
//        if (width % 2 == 1) {
//            width--; // Ensure even width for better compatibility
//        }
//
//        if (height % 2 == 1) {
//            height--; // Ensure even height for better compatibility
//        }
//
//        frame.reshape(width, height);
//
//        Point2D_F64 p0 = new Point2D_F64(frame.getWidth() / 2.0 - size, frame.getHeight() / 2.0 - size);
//        Point2D_F64 p1 = new Point2D_F64(frame.getWidth() / 2.0 + size, frame.getHeight() / 2.0 - size);
//        Point2D_F64 p2 = new Point2D_F64(frame.getWidth() / 2.0 + size, frame.getHeight() / 2.0 + size);
//        Point2D_F64 p3 = new Point2D_F64(frame.getWidth() / 2.0 - size, frame.getHeight() / 2.0 + size);
//        location = new Quadrilateral_F64(p0, p1, p2, p3);
//        tracker.initialize(frame, location);
//        screenCen = new Point2D_F64(frame.getWidth() / 2.0, frame.getHeight() / 2.0);
//    }
//
//    public void setInitialLocation(double latitude, double longitude) {
//        this.initialLatitude = latitude;
//        this.initialLongitude = longitude;
//    }
//
//    public void pause() {
//        isPaused = true;
//    }
//
//    public void resume() {
//        isPaused = false;
//    }
//
//    public boolean isPaused() {
//        return isPaused;
//    }
//
//    // latitude: north or south
//    // longitude east or west
//    // Pitch: upward - backward
//    // roll: right - left
//    // Throttle altitude or vertical movement
//    private void moveToInitialLocation() {
//        if (flightController != null) {
//            double targetLatitude = initialLatitude;
//            double targetLongitude = initialLongitude;
//
//            // Convert the target latitude and longitude to horizontal movement commands
//            float pitch = (float) ((targetLatitude - initialLatitude) * PITCH_SCALING_FACTOR);
//            float roll = (float) ((targetLongitude - initialLongitude) * ROLL_SCALING_FACTOR);
//
//            // Send Virtual Stick commands to move the drone
//            flightController.sendVirtualStickFlightControlData(
//                    new FlightControlData(roll, pitch, 0, 0),
//                    null
//            );
//        }
//    }
//
//    public double[] process(ImageBase frame) {
//        if (!gotFirstImage) {
//            init(frame);
//            gotFirstImage = true;
//            return new double[]{0, 0};
//        }
//
//        if (isPaused) {
//            // Returns: true if the target was found and 'location' updated.
////            boolean visible = tracker.process(frame, location);
////
////            if (visible) {
//            moveToInitialLocation();
////            }
//
//        }
//
//        Point2D_F64 center = getCenter(location);
//        double dx = (center.x - screenCen.x);
//        double dy = (center.y - screenCen.y);
//        return new double[]{dx, dy};
//
//    }
//
////    public double[] process(ImageBase frame) {
////        if (!gotFirstImage) {
////            init(frame);
////            gotFirstImage = true;
////            return new double[]{0, 0};
////        }
////
////        boolean visible = tracker.process(frame, location); // TODO: what happens if not visible
////
////        Point2D_F64 center = getCenter(location);
////        double dx = (center.x - screenCen.x);
////        double dy = (center.y - screenCen.y);
////
////        return new double[]{dx, dy};
////    }
//
////    public double[] process(Bitmap bitmapImage) {
////        return process(ConvertBitmapToBoof.bitmapToPlanarU8(bitmapImage));
////    }
//
//    public double[] process(Bitmap bitmapImage) {
//        GrayU8 result = new GrayU8(bitmapImage.getWidth(), bitmapImage.getHeight());
//        ConvertBitmapToBoof.bitmapToGray(bitmapImage, result, null);
//
//        // Debugging prints
//        System.out.println("Bitmap dimensions: " + bitmapImage.getWidth() + " x " + bitmapImage.getHeight());
//        System.out.println("GrayU8 dimensions: " + result.getWidth() + " x " + result.getHeight());
//
////        // Example resizing
////        GrayU8 resizedFrame = new GrayU8(newWidth, newHeight);
////        BoofMiscOps.copyInside(frame, resizedFrame);
//
//        return process(result);
//    }
//
//    // =-=-=-=-= Helper Functions =-=-=-=-=-=
//
//    public static Point2D_F64 getCenter(Quadrilateral_F64 points) {
//        double[] sum = new double[2];
//        for (int i = 0; i < 4; i++) {
//            sum[0] += points.get(i).x;
//            sum[1] += points.get(i).y;
//        }
//        return new Point2D_F64(sum[0] / 4, sum[1] / 4);
//    }
//
//    public void setFlightController(FlightController flightController) {
//        this.flightController = flightController;
//    }
//}