package org.example;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.abst.sfm.d2.ImageMotion2D;
import boofcv.abst.sfm.d2.PlToGrayMotion2D;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PixelTransformHomography_F32;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.sfm.d2.StitchingFromMotion2D;
import boofcv.factory.feature.associate.ConfigAssociateGreedy;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.factory.sfm.FactoryMotion2D;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ImageGridPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.MediaManager;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.border.BorderType;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.*;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.struct.shapes.RectangleLength2D_I32;
import georegression.transform.homography.HomographyPointOps_F64;
import gnu.trove.impl.sync.TSynchronizedShortObjectMap;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastAccess;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * Stitching in real time app
 *
 * @author Adi Peisach
 */
public class Stitching {


    private final int MAX_DISTANCE; // The maximum distance in pixels between the current image and the center image. Default = 50.
    private StitchingFromMotion2D<Planar<GrayF32>, Homography2D_F64> stitch;
    private Point2D_I32 center;


    public Stitching(Planar<GrayF32> image) {
        init(image);
        MAX_DISTANCE = 50;
    }
    public Stitching(Planar<GrayF32> image, int maxDistance) {
        init(image);
        MAX_DISTANCE = maxDistance;
    }

    /**
     * Stitches the current image and finds the vector between the center image and current image.
     * @param image current image
     * @return vector from current point to center
     */
    public int[] process(Planar<GrayF32> image) {
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

    public static Planar<GrayF32> convert(BufferedImage bufferedImage) {
        // Create a Planar<GrayF32> with the same dimensions as the BufferedImage
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        Planar<GrayF32> planarImage = new Planar<>(GrayF32.class, width, height, 3);

        // Loop through the pixels and copy the RGB values into the Planar channels
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = bufferedImage.getRGB(x, y);

                // Extract the red, green, and blue components
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                // Set the pixel values in the Planar channels
                planarImage.getBand(0).set(x, y, red);
                planarImage.getBand(1).set(x, y, green);
                planarImage.getBand(2).set(x, y, blue);
            }
        }

        return planarImage;
    }

    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=


    public static void main(String[] args) {

        Planar<GrayF32> image = convert(UtilImageIO.loadImageNotNull("Images/DJI_0021.JPG"));

        MediaManager media = DefaultMediaManager.INSTANCE;
        SimpleImageSequence<Planar<GrayF32>> video =
                media.openVideo("Videos/DroneVideo2.mjpeg", ImageType.pl(3, GrayF32.class));
        int count = 1;

        Stitching stitch = new Stitching(video.next());
        while (video.hasNext()) {
            count++;
            Planar<GrayF32> frame = video.next();
            int[] vec = stitch.process(frame);
            System.out.println("dx: " + vec[0] + " dy: " + vec[1]);
            if (count % 100 == 0) {
                System.out.println("reset");
                stitch.reset(frame);
            }
        }
    }
}