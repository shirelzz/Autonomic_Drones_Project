package com.dji.sdk.sample.demo.stitching;

import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.abst.sfm.d2.ImageMotion2D;
import boofcv.abst.sfm.d2.PlToGrayMotion2D;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.sfm.d2.StitchingFromMotion2D;

import boofcv.factory.sfm.FactoryMotion2D;
import boofcv.factory.tracker.FactoryPointTracker;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Vector;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import boofcv.struct.image.*;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Quadrilateral_F64;

/**
 * Stitching in real time app
 *
 * @author Adi Peisach
 */
public class Stitching {


    private final int MAX_DISTANCE; // The maximum distance in pixels between the current image and the center image. Default = 50.
    private StitchingFromMotion2D<Planar<GrayF32>, Homography2D_F64> stitch;
    private Point2D_I32 center;
    private Point2D_I32 originalCenter;
    private Planar<GrayF32> centerImage;
    private boolean gotFirstImage;
    private int width;
    private int height;

    SensorManager sensorManager;

    Vector<String> vector_dx = new Vector<>();
    Vector<String> vector_dy = new Vector<>();


    public Stitching(Planar<GrayF32> image) {
        init(image);
        MAX_DISTANCE = 25;
        gotFirstImage = true;
    }
    public Stitching(Planar<GrayF32> image, int maxDistance) {
        init(image);
        MAX_DISTANCE = maxDistance;
        gotFirstImage = true;
    }
    public Stitching(SensorManager mysensorManager) {
        MAX_DISTANCE = 25;
        gotFirstImage = false;
        sensorManager = mysensorManager;
        if (sensorManager != null) {
            Sensor accleroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accleroSensor != null) {
                sensorManager.registerListener((SensorEventListener) this, accleroSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        } else {

        }
    }

    public Stitching() {
        MAX_DISTANCE = 50;
        gotFirstImage = false;
    }

    /**
     * Stitches the current image and finds the vector between the center image and current image.
     * @param image current image
     * @return vector from current point to center
     */
    public int[] process(Planar<GrayF32> image) {
        if (!gotFirstImage) {
            init(image);
            gotFirstImage = true;
            return new int[]{0, 0};
        }

        stitch.reset();
        stitch.process(centerImage);

        boolean success = stitch.process(image);
        if (!success) {
            reset(image);
        }

        Quadrilateral_F64 corners = stitch.getImageCorners(image.width, image.height, null);
        Point2D_I32 newCenter = getCenter(corners);

        if (center.distance(newCenter) > MAX_DISTANCE) {
            center = newCenter;
            centerImage = deepCopyPlanar(image);
        }

        vector_dx.add(String.valueOf(center.x - originalCenter.x));
        vector_dy.add(String.valueOf(center.y - originalCenter.y));

        return new int[]{center.x - newCenter.x, center.y - newCenter.y};
    }


    public int[] process(Bitmap bitmapImage) {
        return process(ConvertBitmapToBoof.bitmapToPlanarF32(bitmapImage));
    }

    private void init(Planar<GrayF32> image) {
        // Configure the feature detector
        ConfigPointDetector configDetector = new ConfigPointDetector();
        configDetector.type = PointDetectorTypes.SHI_TOMASI;
        configDetector.general.maxFeatures = 300;
        configDetector.general.radius = 3;
        configDetector.general.threshold = 1;

        // Use a KLT tracker
        PointTracker<GrayF32> tracker = FactoryPointTracker.klt(4, configDetector, 3, GrayF32.class, GrayF32.class);

        // This estimates the 2D image motion
        // An Affine2D_F64 model also works quite well.
        ImageMotion2D<GrayF32, Homography2D_F64> motion2D =
                FactoryMotion2D.createMotion2D(220, 3, 2, 30, 0.6, 0.5, false, tracker, new Homography2D_F64());

        // wrap it so it output color images while estimating motion from gray
        ImageMotion2D<Planar<GrayF32>, Homography2D_F64> motion2DColor =
                new PlToGrayMotion2D<>(motion2D, GrayF32.class);

        // This fuses the images together
        stitch = FactoryMotion2D.createVideoStitch(0.5, motion2DColor, ImageType.pl(3, GrayF32.class));

        // shrink the input image and center it
        Homography2D_F64 shrink = new Homography2D_F64(0.5, 0, image.width / 4, 0, 0.5, image.height / 4, 0, 0, 1);
        shrink = shrink.invert(null);

        // The mosaic will be larger in terms of pixels but the image will be scaled down.
        // To change this into stabilization just make it the same size as the input with no shrink.
        stitch.configure(image.width, image.height, shrink);
        stitch.process(image);

        Quadrilateral_F64 cornersFirst = stitch.getImageCorners(image.width, image.height, null);
        width = image.getWidth();
        height = image.getHeight();
        center = getCenter(cornersFirst);
        originalCenter = new Point2D_I32(center);
        centerImage = deepCopyPlanar(image);
    }

    public void reset(Planar<GrayF32> image) {
        stitch.reset();

        stitch.process(image);

        Quadrilateral_F64 corners = stitch.getImageCorners(image.width, image.height, null);
        center = getCenter(corners);
        centerImage = deepCopyPlanar(image);
    }

    public Bitmap getImage() {
        // Create a bitmap with the same dimensions as your stitch.getStitchedImage()
        Bitmap ret = Bitmap.createBitmap(stitch.getStitchedImage().getWidth(), stitch.getStitchedImage().getHeight(), Bitmap.Config.ARGB_8888);
//        ConvertBitmap.planarToBitmap(stitch.getStitchedImage(), ret, null);

        // Create a Canvas to draw on the bitmap
        Canvas canvas = new Canvas(ret);

        // Create a Paint object for drawing shapes
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);

        // Draw a quadrilateral around the current frame in the mosaic
        Quadrilateral_F64 corners = stitch.getImageCorners(width, height, null);
        canvas.drawLine((float) corners.a.x, (float) corners.a.y, (float) corners.b.x, (float) corners.b.y, paint);
        canvas.drawLine((float) corners.b.x, (float) corners.b.y, (float) corners.c.x, (float) corners.c.y, paint);
        canvas.drawLine((float) corners.c.x, (float) corners.c.y, (float) corners.d.x, (float) corners.d.y, paint);
        canvas.drawLine((float) corners.d.x, (float) corners.d.y, (float) corners.a.x, (float) corners.a.y, paint);

        // Draw the line between centers and update the current center if needed
        Point2D_I32 curr = getCenter(corners);

        // Create a Paint object for drawing the center and line
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);

        // Draw a rectangle for the center
        canvas.drawRect(curr.x - 3, curr.y - 3, curr.x + 3, curr.y + 3, paint);

        // Draw a line from curr to center
        canvas.drawLine(curr.x, curr.y, center.x, center.y, paint);

        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        for (int i = 1; vector_dx.size() == vector_dy.size() && i < vector_dx.size(); i++) {
            canvas.drawLine(center.x + Float.parseFloat(vector_dx.get(i)), center.y + Float.parseFloat(vector_dy.get(i)),
                    center.x + Float.parseFloat(vector_dx.get(i - 1)), center.y + Float.parseFloat(vector_dy.get(i - 1)), paint);
        }

        return ret;
    }

    // =-=-=-= Helper Functions =-=-=-=

    private static Point2D_I32 getCenter(Quadrilateral_F64 corners) {
        double x = (corners.a.x + corners.b.x + corners.c.x + corners.d.x) / 4;
        double y = (corners.a.y + corners.b.y + corners.c.y + corners.d.y) / 4;
        return new Point2D_I32((int) x, (int) y);
    }

    public static Planar<GrayF32> deepCopyPlanar(Planar<GrayF32> input) {
        Planar<GrayF32> output = new Planar<GrayF32>(GrayF32.class, input.width, input.height, input.getNumBands());

        for (int i = 0; i < input.getNumBands(); i++) {
            output.bands[i] = deepCopyGrayF32(input.bands[i]);
        }

        return output;
    }

    public static GrayF32 deepCopyGrayF32(GrayF32 input) {
        int width = input.width;
        int height = input.height;
        GrayF32 output = new GrayF32(width, height);

        // Copy pixel values from input image to output image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float value = input.get(x, y);
                output.set(x, y, value);
            }
        }

        return output;
    }

    public Planar<GrayF32> getStitchedImage() {
        return stitch.getStitchedImage();
    }

    public void clear_dx_dy() {
        vector_dx.clear();
        vector_dy.clear();
    }

    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

//    public static void main(String[] args) {
//        MediaManager media = DefaultMediaManager.INSTANCE;
//        SimpleImageSequence<Planar<GrayF32>> video =
//                media.openVideo("Videos/Rope3min.mjpeg", ImageType.pl(3, GrayF32.class));
//        int count = 1;
//
//        Stitching stitch = new Stitching(video.next());
//        while (video.hasNext()) {
//            count++;
//            Planar<GrayF32> frame = video.next();
//            int[] vec = stitch.process(frame);
//            System.out.println("dx: " + vec[0] + " dy: " + vec[1]);
//        }
//    }

}