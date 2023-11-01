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
import android.graphics.Color;
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
    private boolean gotFirstImage;


    public Stitching(Planar<GrayF32> image) {
        init(image);
        MAX_DISTANCE = 50;
        gotFirstImage = true;
    }
    public Stitching(Planar<GrayF32> image, int maxDistance) {
        init(image);
        MAX_DISTANCE = maxDistance;
        gotFirstImage = true;
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
            return null;
        }

        boolean success = stitch.process(image);
        if (!success) {
            reset(image);
        }

        Quadrilateral_F64 corners = stitch.getImageCorners(image.width, image.height, null);
        Point2D_I32 newCenter = getCenter(corners);

        if (center.distance(newCenter) > MAX_DISTANCE)
            center = newCenter;

        return new int[] {center.x - newCenter.x,  center.y - newCenter.y};
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
        Homography2D_F64 shrink = new Homography2D_F64(0.5, 0, image.width/4, 0, 0.5, image.height/4, 0, 0, 1);
        shrink = shrink.invert(null);

        // The mosaic will be larger in terms of pixels but the image will be scaled down.
        // To change this into stabilization just make it the same size as the input with no shrink.
        stitch.configure(image.width, image.height, shrink);
        stitch.process(image);

        Quadrilateral_F64 cornersFirst = stitch.getImageCorners(image.width, image.height, null);
        center = getCenter(cornersFirst);
    }

    public void reset(Planar<GrayF32> image) {
        stitch.reset();
        stitch.process(image);
        Quadrilateral_F64 corners = stitch.getImageCorners(image.width, image.height, null);
        center = getCenter(corners);
    }

    // =-=-=-= Helper Functions =-=-=-=

    private static Point2D_I32 getCenter(Quadrilateral_F64 corners) {
        double x = (corners.a.x+corners.b.x+corners.c.x+corners.d.x)/4;
        double y = (corners.a.y+corners.b.y+corners.c.y+corners.d.y)/4;
        return new Point2D_I32((int)x,(int)y);
    }

    public static Planar<GrayF32> convert(Bitmap bitmap) {
        // Get the dimensions of the Bitmap
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Create a Planar<GrayF32> with the same dimensions
        Planar<GrayF32> planarImage = new Planar<>(GrayF32.class, width, height, 3);

        // Convert the Bitmap to Planar<GrayF32>
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);

                // Extract the red, green, and blue components
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);

                // Set the pixel values in the Planar channels
                planarImage.getBand(0).set(x, y, red);
                planarImage.getBand(1).set(x, y, green);
                planarImage.getBand(2).set(x, y, blue);
            }
        }

        return planarImage;
    }

//    public static Planar<GrayF32> convert(BufferedImage bufferedImage) {
//        // Create a Planar<GrayF32> with the same dimensions as the BufferedImage
//        int width = bufferedImage.getWidth();
//        int height = bufferedImage.getHeight();
//        Planar<GrayF32> planarImage = new Planar<>(GrayF32.class, width, height, 3);
//
//        // Loop through the pixels and copy the RGB values into the Planar channels
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                int rgb = bufferedImage.getRGB(x, y);
//
//                // Extract the red, green, and blue components
//                int red = (rgb >> 16) & 0xFF;
//                int green = (rgb >> 8) & 0xFF;
//                int blue = rgb & 0xFF;
//
//                // Set the pixel values in the Planar channels
//                planarImage.getBand(0).set(x, y, red);
//                planarImage.getBand(1).set(x, y, green);
//                planarImage.getBand(2).set(x, y, blue);
//            }
//        }
//
//        return planarImage;
//    }

    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

//    public static void main(String[] args) {
//        Bitmap loadedBitmap1 = BitmapFactory.decodeFile("images/image1.JPG");
//        Planar<GrayF32> image1 = convert(loadedBitmap1);
//
//        Bitmap loadedBitmap2 = BitmapFactory.decodeFile("images/image2.JPG");
//        Planar<GrayF32> image2 = convert(loadedBitmap2);
//
//        Stitching stitch = new Stitching(image1);
//            int[] vec = stitch.process(image2);
//            System.out.println("dx: " + vec[0] + " dy: " + vec[1]);
//
////        MediaManager media = DefaultMediaManager.INSTANCE;
////        SimpleImageSequence<Planar<GrayF32>> video =
////                media.openVideo("Videos/DroneVideo2.mjpeg", ImageType.pl(3, GrayF32.class));
////        int count = 1;
////
////        Stitching stitch = new Stitching(video.next());
////        while (video.hasNext()) {
////            count++;
////            Planar<GrayF32> frame = video.next();
////            int[] vec = stitch.process(frame);
////            System.out.println("dx: " + vec[0] + " dy: " + vec[1]);
////            if (count % 100 == 0) {
////                System.out.println("reset");
////                stitch.reset(frame);
////            }
////        }
//    }
}