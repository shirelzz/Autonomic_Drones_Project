//package com.dji.sdk.sample.demo.stitching;
//
//import boofcv.abst.tracker.ConfigComaniciu2003;
//import boofcv.abst.tracker.ConfigTrackerTld;
//import boofcv.abst.tracker.MeanShiftLikelihoodType;
//import boofcv.abst.tracker.TrackerObjectQuad;
//import boofcv.factory.tracker.FactoryTrackerObjectQuad;
//import boofcv.io.MediaManager;
//import boofcv.io.image.SimpleImageSequence;
//import boofcv.io.wrapper.DefaultMediaManager;
//import boofcv.struct.image.*;
//import georegression.struct.point.Point2D_F64;
//import georegression.struct.shapes.Quadrilateral_F64;
//
//public class Tracker {
//
//
//    private boolean gotFirstImage;
//    private final String ALG;
//    private TrackerObjectQuad tracker;
//    private int size;
//    private Quadrilateral_F64 location;
//    private Point2D_F64 screenCen;
//
//
//    public Tracker() {
//        this("sparseFlow",50);
//    }
//    public Tracker(String algorithm) {
//        this(algorithm,50);
//    }
//    public Tracker(String algorithm, int sizeInput) {
//        gotFirstImage = false;
//        ALG = algorithm;
//        size = sizeInput;
//    }
//    public Tracker(ImageBase image) {
//        this(image,"sparseFlow");
//    }
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
//            case "circulant" :
//                tracker = FactoryTrackerObjectQuad.circulant(null, GrayU8.class);
//            case "sparseFlow" :
//                tracker = FactoryTrackerObjectQuad.sparseFlow(null, GrayU8.class, null);
//            case "tld" :
//                tracker = FactoryTrackerObjectQuad.tld(new ConfigTrackerTld(false), GrayU8.class);
//            case "meanShiftComaniciu2003" :
//                    tracker = FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(), ImageType.pl(3, GrayU8.class));
//            case "meanShiftLikelihood" :
//                    tracker = FactoryTrackerObjectQuad.meanShiftLikelihood(30, 5, 255, MeanShiftLikelihoodType.HISTOGRAM, ImageType.pl(3, GrayU8.class));
//        }
//
//        Point2D_F64 p0 = new Point2D_F64(frame.getWidth()/2-size, frame.getHeight()/2-size);
//        Point2D_F64 p1 = new Point2D_F64(frame.getWidth()/2+size, frame.getHeight()/2-size);
//        Point2D_F64 p2 = new Point2D_F64(frame.getWidth()/2+size, frame.getHeight()/2+size);
//        Point2D_F64 p3 = new Point2D_F64(frame.getWidth()/2-size, frame.getHeight()/2+size);
//        location = new Quadrilateral_F64(p0,p1,p2,p3);
//        tracker.initialize(frame, location);
//        screenCen = new Point2D_F64(frame.getWidth()/2,frame.getHeight()/2);
//
//    }
//
//    public double[] process(ImageBase frame) {
//        if (!gotFirstImage) {
//            init(frame);
//            gotFirstImage = true;
//            return new double [] {0,0};
//        }
//
//        boolean visible = tracker.process(frame, location); // TODO: what happens if not visible
//
//        Point2D_F64 center = getCenter(location);
//        double dx = (center.x-screenCen.x);
//        double dy = (center.y-screenCen.y);
//
//        return new double[] {dx,dy};
//    }
//
//    // =-=-=-=-= Helper Functions =-=-=-=-=-=
//
//    public static Point2D_F64 getCenter(Quadrilateral_F64 points) {
//        double[] sum = new double [2];
//        for (int i = 0; i < 4; i++) {
//            sum[0] += points.get(i).x;
//            sum[1] += points.get(i).y;
//        }
//        return new Point2D_F64(sum[0]/4,sum[1]/4);
//    }
//
//    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
//
//    public static void main(String[] args) {
//        MediaManager media = DefaultMediaManager.INSTANCE;
//        SimpleImageSequence video = media.openVideo("Videos/DroneVideo2.mjpeg", FactoryTrackerObjectQuad.tld(null,GrayU8.class).getImageType());
//
//        Tracker tracking = new Tracker();
//        while (video.hasNext()) {
//            double[] vec = tracking.process(video.next());
//            System.out.println("dx: " + vec[0] + " dy: " + vec[1]);
//        }
//    }
//
//}