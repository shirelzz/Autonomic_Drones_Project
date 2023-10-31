/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.examples.geometry;

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
 * Exampling showing how to combines two images together by finding the best fit image transform with point
 * features. Algorithm Steps:
 *
 * <ol>
 * <li>Detect feature locations</li>
 * <li>Compute feature descriptors</li>
 * <li>Associate features together</li>
 * <li>Use robust fitting to find transform</li>
 * <li>Render combined image</li>
 * </ol>
 *
 * @author Adi Peisach
 */
public class ExampleImageStitching {
    Scanner input = new Scanner(System.in);
    private static final boolean SHOW_VECTOR = false;
    private static final int MAX_DISTANCE = 400;
    /**
     * Using abstracted code, find a transform which minimizes the difference between corresponding features
     * in both images. This code is completely model independent and is the core algorithms.
     */
    public static <T extends ImageGray<T>, TD extends TupleDesc<TD>> Homography2D_F64
    computeTransform( T imageA, T imageB,
                      DetectDescribePoint<T, TD> detDesc,
                      AssociateDescription<TD> associate,
                      ModelMatcher<Homography2D_F64, AssociatedPair> modelMatcher ) {
        // get the length of the description
        List<Point2D_F64> pointsA = new ArrayList<>();
        DogArray<TD> descA = UtilFeature.createArray(detDesc, 100);
        List<Point2D_F64> pointsB = new ArrayList<>();
        DogArray<TD> descB = UtilFeature.createArray(detDesc, 100);

        // extract feature locations and descriptions from each image
        describeImage(imageA, detDesc, pointsA, descA);
        describeImage(imageB, detDesc, pointsB, descB);

        // Associate features between the two images
        associate.setSource(descA);
        associate.setDestination(descB);
        associate.associate();

        // create a list of AssociatedPairs that tell the model matcher how a feature moved
        FastAccess<AssociatedIndex> matches = associate.getMatches();
        List<AssociatedPair> pairs = new ArrayList<>();

        for (int i = 0; i < matches.size(); i++) {
            AssociatedIndex match = matches.get(i);

            Point2D_F64 a = pointsA.get(match.src);
            Point2D_F64 b = pointsB.get(match.dst);

            pairs.add(new AssociatedPair(a, b, false));
        }

        // find the best fit model to describe the change between these images
        if (!modelMatcher.process(pairs))
            throw new RuntimeException("Model Matcher failed!");

        // return the found image transform
        return modelMatcher.getModelParameters().copy();
    }

    /**
     * Detects features inside the two images and computes descriptions at those points.
     */
    private static <T extends ImageGray<T>, TD extends TupleDesc<TD>>
    void describeImage( T image,
                        DetectDescribePoint<T, TD> detDesc,
                        List<Point2D_F64> points,
                        DogArray<TD> listDescs ) {
        detDesc.detect(image);

        listDescs.reset();
        for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
            points.add(detDesc.getLocation(i).copy());
            listDescs.grow().setTo(detDesc.getDescription(i));
        }
    }

    /**
     * Given two input images create and display an image where the two have been overlayed on top of each other.
     */
    public static <T extends ImageGray<T>>
    BufferedImage stitch( BufferedImage imageA, BufferedImage imageB, Class<T> imageType ) {
        // convert images from bufferedImage to class T
        T inputA = ConvertBufferedImage.convertFromSingle(imageA, null, imageType);
        T inputB = ConvertBufferedImage.convertFromSingle(imageB, null, imageType);

        // Detect using the standard SURF feature descriptor and describer
        DetectDescribePoint detDesc = FactoryDetectDescribe.surfStable(
                new ConfigFastHessian(1, 2, 200, 1, 9, 4, 4), null, null, imageType);
        ScoreAssociation<TupleDesc_F64> scorer = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class, true);
        AssociateDescription<TupleDesc_F64> associate = FactoryAssociation.greedy(new ConfigAssociateGreedy(true, 2), scorer);

        // fit the images using a homography. This works well for rotations and distant objects.
        ModelMatcher<Homography2D_F64, AssociatedPair> modelMatcher =
                FactoryMultiViewRobust.homographyRansac(null, new ConfigRansac(60, 3));

        Homography2D_F64 H = computeTransform(inputA, inputB, detDesc, associate, modelMatcher);
        //System.out.println(H);

        return renderStitching(imageA, imageB, H);
    }

    /**
     * Renders and displays the stitched together images
     */
    public static BufferedImage renderStitching( BufferedImage imageA, BufferedImage imageB,
                                        Homography2D_F64 fromAtoB ) {
        // specify size of output image
        double scale = 0.5;

        // Convert into a BoofCV color format
        Planar<GrayF32> colorA =
                ConvertBufferedImage.convertFromPlanar(imageA, null, true, GrayF32.class);
        Planar<GrayF32> colorB =
                ConvertBufferedImage.convertFromPlanar(imageB, null, true, GrayF32.class);

        // Where the output images are rendered into
        Planar<GrayF32> work = colorA.createSameShape();

        // Adjust the transform so that the whole image can appear inside of it
        Homography2D_F64 fromAToWork = new Homography2D_F64(scale, 0, colorA.width/4, 0, scale, colorA.height/4, 0, 0, 1);
        Homography2D_F64 fromWorkToA = fromAToWork.invert(null);

        // Used to render the results onto an image
        PixelTransformHomography_F32 model = new PixelTransformHomography_F32();
        InterpolatePixelS<GrayF32> interp = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.ZERO);
        ImageDistort<Planar<GrayF32>, Planar<GrayF32>> distort =
                DistortSupport.createDistortPL(GrayF32.class, model, interp, false);
        distort.setRenderAll(false);

        // Render first image
        model.setTo(fromWorkToA);
        distort.apply(colorA, work);

        // Render second image
        Homography2D_F64 fromWorkToB = fromWorkToA.concat(fromAtoB, null);
        model.setTo(fromWorkToB);
        distort.apply(colorB, work);

        // Convert the rendered image into a BufferedImage
        BufferedImage output = new BufferedImage(work.width, work.height, imageA.getType());
        ConvertBufferedImage.convertTo(work, output, true);

        Graphics2D g2 = output.createGraphics();

        // draw lines around the distorted image to make it easier to see
        Homography2D_F64 fromBtoWork = fromWorkToB.invert(null);
        Point2D_I32 corners[] = new Point2D_I32[4];
        corners[0] = renderPoint(0, 0, fromBtoWork);
        corners[1] = renderPoint(colorB.width, 0, fromBtoWork);
        corners[2] = renderPoint(colorB.width, colorB.height, fromBtoWork);
        corners[3] = renderPoint(0, colorB.height, fromBtoWork);

        // draw the boarder of
        g2.setColor(Color.YELLOW);
        g2.setStroke(new BasicStroke(4));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawLine(corners[0].x, corners[0].y, corners[1].x, corners[1].y);
        g2.drawLine(corners[1].x, corners[1].y, corners[2].x, corners[2].y);
        g2.drawLine(corners[2].x, corners[2].y, corners[3].x, corners[3].y);
        g2.drawLine(corners[3].x, corners[3].y, corners[0].x, corners[0].y);

        Point2D_I32 corners2[] = new Point2D_I32[4];
        corners2[0] = renderPoint(0, 0, fromAToWork);
        corners2[1] = renderPoint(colorA.width, 0, fromAToWork);
        corners2[2] = renderPoint(colorA.width, colorA.height, fromAToWork);
        corners2[3] = renderPoint(0, colorA.height, fromAToWork);

        g2.setColor(Color.BLUE);
        g2.setStroke(new BasicStroke(4));
        Point2D_I32 center1 = getCenter(corners);
        g2.drawRect(center1.x-5, center1.y-5, 10,10);
        Point2D_I32 center2 = getCenter(corners2);
        g2.drawRect(center2.x-5, center2.y-5, 10,10);

        g2.setColor(Color.RED);
        g2.drawLine(center1.x, center1.y, center2.x, center2.y);
        Point2D_I32 vector = new Point2D_I32(center1.x - center2.x,center1.y-center2.y);
        distance = center1.distance(center2);

        if (SHOW_VECTOR) {
            for (int i = 0; i < 35; i++) {
                System.out.println();
            }
            System.out.println("center1: " + center1);
            System.out.println("center2: " + center2);
            System.out.println("vector: " + vector);
        }

        //ShowImages.showWindow(output, "Stitched Images", true);
        return output;
    }

    private static Point2D_I32 renderPoint( int x0, int y0, Homography2D_F64 fromBtoWork ) {
        Point2D_F64 result = new Point2D_F64();
        HomographyPointOps_F64.transform(fromBtoWork, new Point2D_F64(x0, y0), result);
        return new Point2D_I32((int)result.x, (int)result.y);
    }

    public static <T extends ImageGray<T>>
    BufferedImage stitch(List<BufferedImage> images, Class<T> imageType) {
        if (images.size() < 2) {
            throw new IllegalArgumentException("At least two images are required for stitching.");
        }

        List<T> inputs = new ArrayList<>();

        // Convert input images to BoofCV format
        for (BufferedImage image : images) {
            T input = ConvertBufferedImage.convertFromSingle(image, null, imageType);
            inputs.add(input);
        }

        List<Homography2D_F64> homographies = new ArrayList<>();
        DetectDescribePoint detDesc = FactoryDetectDescribe.surfStable(
                new ConfigFastHessian(1, 2, 200, 1, 9, 4, 4), null, null, imageType);
        ScoreAssociation<TupleDesc_F64> scorer = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class, true);
        AssociateDescription<TupleDesc_F64> associate = FactoryAssociation.greedy(new ConfigAssociateGreedy(true, 2), scorer);

        // fit the images using a homography. This works well for rotations and distant objects.
        ModelMatcher<Homography2D_F64, AssociatedPair> modelMatcher =
                FactoryMultiViewRobust.homographyRansac(null, new ConfigRansac(60, 3));

        // Find homography for each consecutive pair of images
        for (int i = 0; i < inputs.size() - 1; i++) {
            Homography2D_F64 H = computeTransform(inputs.get(i), inputs.get(i + 1), detDesc, associate, modelMatcher);
            homographies.add(H);
        }

        // Apply the transformations to stitch the images
        BufferedImage resultImage = images.get(0);
        for (int i = 0; i < homographies.size(); i++) {
            //resultImage = renderStitching(resultImage, images.get(i + 1), homographies.get(i));
            //showImage(resultImage);
        }


        return resultImage;
    }

    public static BufferedImage stitch( GrayF32 imageA, GrayF32 imageB) {

        // Detect using the standard SURF feature descriptor and describer
        DetectDescribePoint detDesc = FactoryDetectDescribe.surfStable(
                new ConfigFastHessian(1, 2, 200, 1, 9, 4, 4), null, null, GrayF32.class);
        ScoreAssociation<TupleDesc_F64> scorer = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class, true);
        AssociateDescription<TupleDesc_F64> associate = FactoryAssociation.greedy(new ConfigAssociateGreedy(true, 2), scorer);

        // fit the images using a homography. This works well for rotations and distant objects.
        ModelMatcher<Homography2D_F64, AssociatedPair> modelMatcher =
                FactoryMultiViewRobust.homographyRansac(null, new ConfigRansac(60, 3));

        Homography2D_F64 H = computeTransform(imageA, imageB, detDesc, associate, modelMatcher);

        return renderStitching(ConvertBufferedImage.convertTo(imageA, null), ConvertBufferedImage.convertTo(imageB, null), H);
    }

    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    // =-=-=-= Helper Function =-=-=-=-=

    public static void showImages(Planar<GrayF32> frame) {
        for (int i = 0; i < frame.getNumBands(); i++) {
            showImage(ConvertBufferedImage.convertTo(frame.getBand(i), null));
        }
    }
    public static void showImages(Planar<GrayF32> frame, String title) {
        for (int i = 0; i < frame.getNumBands(); i++) {
            showImage(ConvertBufferedImage.convertTo(frame.getBand(i), null), title + ": " + i);
        }
    }
    private static void showImage(BufferedImage im) { ShowImages.showWindow(im, "Stitched Images", false); }
    private static void showImage(BufferedImage im, String title) { ShowImages.showWindow(im, title, false); }
    private static void showImage(Planar<GrayF32> im, String title) {
        ShowImages.showWindow(ConvertBufferedImage.convertTo(im.getBand(0), null), title, false);
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

    private static Point2D_I32 getCenter(Point2D_I32[] corners) {
        int sumX = 0; int sumY = 0;
        for (int i = 0; i < corners.length; i++) {
            sumX += corners[i].x;
            sumY += corners[i].y;
        }
        return new Point2D_I32(sumX/4,sumY/4);
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
    private static boolean nearBorder( Point2D_F64 p, StitchingFromMotion2D<?, ?> stitch ) {
        int r = 15;
        if (p.x < r || p.y < r)
            return true;
        if (p.x >= stitch.getStitchedImage().width - r)
            return true;
        if (p.y >= stitch.getStitchedImage().height - r)
            return true;

        return false;
    }
    private static boolean nearBorder( Quadrilateral_F64 corners, StitchingFromMotion2D<?, ?> stitch ) {
        return nearBorder(corners.a, stitch) || nearBorder(corners.b, stitch) ||
                nearBorder(corners.c, stitch) || nearBorder(corners.d, stitch);
    }
    private static Point2D_I32 getCenter(Quadrilateral_F64 corners) {
        double x = (corners.a.x+corners.b.x+corners.c.x+corners.d.x)/4;
        double y = (corners.a.y+corners.b.y+corners.c.y+corners.d.y)/4;
        return new Point2D_I32((int)x,(int)y);
    }

    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    private static void analyseVideo(String fileName) {
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
        StitchingFromMotion2D<Planar<GrayF32>, Homography2D_F64>
                stitch = FactoryMotion2D.createVideoStitch(0.5, motion2DColor, ImageType.pl(3, GrayF32.class));

        // Load an image sequence
        MediaManager media = DefaultMediaManager.INSTANCE;
        SimpleImageSequence<Planar<GrayF32>> video =
                media.openVideo(fileName, ImageType.pl(3, GrayF32.class));

        int cropPixels = 8;

        Planar<GrayF32> frame = video.next();
        frame = frame.subimage(cropPixels, cropPixels, frame.width - cropPixels, frame.height - cropPixels);


        // shrink the input image and center it
        Homography2D_F64 shrink = new Homography2D_F64(0.5, 0, frame.width/4, 0, 0.5, frame.height/4, 0, 0, 1);
        shrink = shrink.invert(null);

        // The mosaic will be larger in terms of pixels but the image will be scaled down.
        // To change this into stabilization just make it the same size as the input with no shrink.
        stitch.configure(frame.width, frame.height, shrink);
        // process the first frame
        stitch.process(frame);

        // Create the GUI for displaying the results + input image
        ImageGridPanel gui = new ImageGridPanel(1, 2);
        gui.setImage(0, 1, new BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB));
        gui.setImage(0, 0, new BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB));
        gui.setPreferredSize(new Dimension(3 * frame.width, frame.height * 2));
        ShowImages.showWindow(gui, "Example Mosaic", true);

        Quadrilateral_F64 cornersFirst = stitch.getImageCorners(frame.width, frame.height, null);
        Point2D_I32 center = getCenter(cornersFirst);

        // process the video sequence one frame at a time
        while (video.hasNext()) {
            //for (int i = 0; i < 5; i ++) {
                frame = video.next();
            //}

            // Crop the image to remove the specified number of pixels from each side
            frame = frame.subimage(cropPixels, cropPixels, frame.width - cropPixels, frame.height - cropPixels);

            boolean success = stitch.process(frame);

            if (!success) {
                stitch.reset();
                stitch.process(frame);
                continue;
            }

            // if the current image is close to the image border recenter the mosaic
            Quadrilateral_F64 corners = stitch.getImageCorners(frame.width, frame.height, null);
            if (nearBorder(corners,stitch)) {
                stitch.setOriginToCurrent();
                corners = stitch.getImageCorners(frame.width, frame.height, null);
            }
            // display the mosaic
            ConvertBufferedImage.convertTo(frame, gui.getImage(0, 1), true);
            ConvertBufferedImage.convertTo(stitch.getStitchedImage(), gui.getImage(0, 0), true);

            // draw a quadrilateral around the current frame in the mosaic
            Graphics2D g2 = gui.getImage(0, 0).createGraphics();
            g2.setColor(Color.BLUE);
            /*g2.drawLine((int)corners.a.x, (int)corners.a.y, (int)corners.b.x, (int)corners.b.y);
            g2.drawLine((int)corners.b.x, (int)corners.b.y, (int)corners.c.x, (int)corners.c.y);
            g2.drawLine((int)corners.c.x, (int)corners.c.y, (int)corners.d.x, (int)corners.d.y);
            g2.drawLine((int)corners.d.x, (int)corners.d.y, (int)corners.a.x, (int)corners.a.y);*/

            // Draw the line between centers and update the current center if needed
            Point2D_I32 curr = getCenter(corners);
            g2.drawRect(curr.x-3,curr.y-3,6,6);

            if (center.distance(curr) > 50) {
                center = curr;
            }

            g2.setColor(Color.RED);
            g2.drawLine(curr.x,curr.y, center.x, center.y);

            gui.repaint();

            // throttle the speed just in case it's on a fast computer
            //BoofMiscOps.pause(50);
        }
    }

    private static void analyseVideo2Images(String fileName) {
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
        StitchingFromMotion2D<Planar<GrayF32>, Homography2D_F64>
                stitch = FactoryMotion2D.createVideoStitch(0.5, motion2DColor, ImageType.pl(3, GrayF32.class));

        // Load an image sequence
        MediaManager media = DefaultMediaManager.INSTANCE;
        SimpleImageSequence<Planar<GrayF32>> video =
                media.openVideo(fileName, ImageType.pl(3, GrayF32.class));

        int cropPixels = 8;

        Planar<GrayF32> frame = video.next();
        frame = frame.subimage(cropPixels, cropPixels, frame.width - cropPixels, frame.height - cropPixels);


        // shrink the input image and center it
        Homography2D_F64 shrink = new Homography2D_F64(0.5, 0, frame.width/4, 0, 0.5, frame.height/4, 0, 0, 1);
        shrink = shrink.invert(null);

        // The mosaic will be larger in terms of pixels but the image will be scaled down.
        // To change this into stabilization just make it the same size as the input with no shrink.
        stitch.configure(frame.width, frame.height, shrink);
        // process the first frame
        stitch.process(frame);

        // Create the GUI for displaying the results + input image
        ImageGridPanel gui = new ImageGridPanel(1, 2);
        gui.setImage(0, 1, new BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB));
        gui.setImage(0, 0, new BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB));
        gui.setPreferredSize(new Dimension(3 * frame.width, frame.height * 2));
        ShowImages.showWindow(gui, "Example Mosaic", true);

        //boolean enlarged = false;

        Quadrilateral_F64 cornersFirst = stitch.getImageCorners(frame.width, frame.height, null);
        Point2D_I32 center = getCenter(cornersFirst);
        Planar<GrayF32> centerImage = deepCopyPlanar(frame);

        // process the video sequence one frame at a time
        while (video.hasNext()) {
            frame = video.next();

            // Crop the image to remove the specified number of pixels from each side
            frame = frame.subimage(cropPixels, cropPixels, frame.width - cropPixels, frame.height - cropPixels);

            long startTime = System.currentTimeMillis();
            boolean success = stitch.process(frame);
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println("elapsed time: " + elapsedTime + " milliseconds");

            if (!success) {
                stitch.reset();
                stitch.process(frame);
            }

            // if the current image is close to the image border recenter the mosaic
            Quadrilateral_F64 corners = stitch.getImageCorners(frame.width, frame.height, null);
            if (nearBorder(corners,stitch)) {
                stitch.setOriginToCurrent();
                corners = stitch.getImageCorners(frame.width, frame.height, null);
            }
            // display the mosaic
            ConvertBufferedImage.convertTo(frame, gui.getImage(0, 1), true);
            ConvertBufferedImage.convertTo(stitch.getStitchedImage(), gui.getImage(0, 0), true);

            // draw a quadrilateral around the current frame in the mosaic
            Graphics2D g2 = gui.getImage(0, 0).createGraphics();
            g2.setColor(Color.BLUE);
            g2.drawLine((int)corners.a.x, (int)corners.a.y, (int)corners.b.x, (int)corners.b.y);
            g2.drawLine((int)corners.b.x, (int)corners.b.y, (int)corners.c.x, (int)corners.c.y);
            g2.drawLine((int)corners.c.x, (int)corners.c.y, (int)corners.d.x, (int)corners.d.y);
            g2.drawLine((int)corners.d.x, (int)corners.d.y, (int)corners.a.x, (int)corners.a.y);

            // Draw the line between centers and update the current center if needed
            Point2D_I32 curr = getCenter(corners);
            g2.drawRect(curr.x-3,curr.y-3,6,6);

            if (center.distance(curr) > 40) {
                center = curr;
                centerImage = deepCopyPlanar(frame);
                //showImage(centerImage,"new center image");
            }

            g2.setColor(Color.RED);
            g2.drawLine(curr.x,curr.y, center.x, center.y);

            gui.repaint();
            stitch.reset();
            stitch.process(centerImage);
        }
    }

    private static ArrayList<Point2D_I32> analyseVideo2(String fileName) {
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
        StitchingFromMotion2D<Planar<GrayF32>, Homography2D_F64>
                stitch = FactoryMotion2D.createVideoStitch(0.5, motion2DColor, ImageType.pl(3, GrayF32.class));

        // Load an image sequence
        MediaManager media = DefaultMediaManager.INSTANCE;
        SimpleImageSequence<Planar<GrayF32>> video =
                media.openVideo(fileName, ImageType.pl(3, GrayF32.class));

        int cropPixels = 8;

        Planar<GrayF32> frame = video.next();
        frame = frame.subimage(cropPixels, cropPixels, frame.width - cropPixels, frame.height - cropPixels);


        // shrink the input image and center it
        Homography2D_F64 shrink = new Homography2D_F64(0.5, 0, frame.width/4, 0, 0.5, frame.height/4, 0, 0, 1);
        shrink = shrink.invert(null);

        // The mosaic will be larger in terms of pixels but the image will be scaled down.
        // To change this into stabilization just make it the same size as the input with no shrink.
        stitch.configure(frame.width, frame.height, shrink);
        // process the first frame
        stitch.process(frame);

        // Create the GUI for displaying the results + input image
        ImageGridPanel gui = new ImageGridPanel(1, 3);
        gui.setImage(0, 2, new BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB));
        gui.setImage(0, 1, new BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB));
        gui.setImage(0, 0, new BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB));
        gui.setPreferredSize(new Dimension(3 * frame.width, frame.height * 2));
        ShowImages.showWindow(gui, "Example Mosaic", true);

        //boolean enlarged = false;

        Quadrilateral_F64 cornersFirst = stitch.getImageCorners(frame.width, frame.height, null);
        Point2D_I32 center = getCenter(cornersFirst);

        // process the video sequence one frame at a time
        while (video.hasNext()) {

            frame = video.next();

            // Crop the image to remove the specified number of pixels from each side
            frame = frame.subimage(cropPixels, cropPixels, frame.width - cropPixels, frame.height - cropPixels);

            boolean success = stitch.process(frame);

            if (!success)
                throw new RuntimeException("You should handle failures");

            // if the current image is close to the image border recenter the mosaic
            Quadrilateral_F64 corners = stitch.getImageCorners(frame.width, frame.height, null);
            if (nearBorder(corners.a, stitch) || nearBorder(corners.b, stitch) ||
                    nearBorder(corners.c, stitch) || nearBorder(corners.d, stitch)) {
                stitch.setOriginToCurrent();
                corners = stitch.getImageCorners(frame.width, frame.height, null);
            }
            // display the mosaic
            ConvertBufferedImage.convertTo(frame, gui.getImage(0, 2), true);
            ConvertBufferedImage.convertTo(stitch.getStitchedImage(), gui.getImage(0, 0), true);

            // draw a red quadrilateral around the current frame in the mosaic
            Graphics2D g2 = gui.getImage(0, 0).createGraphics();
            g2.setColor(Color.BLUE);
            /*g2.drawLine((int)corners.a.x, (int)corners.a.y, (int)corners.b.x, (int)corners.b.y);
            g2.drawLine((int)corners.b.x, (int)corners.b.y, (int)corners.c.x, (int)corners.c.y);
            g2.drawLine((int)corners.c.x, (int)corners.c.y, (int)corners.d.x, (int)corners.d.y);
            g2.drawLine((int)corners.d.x, (int)corners.d.y, (int)corners.a.x, (int)corners.a.y);*/

            Point2D_I32 curr = getCenter(corners);
            g2.drawRect(curr.x-3,curr.y-3,6,6);

            if (center.distance(curr) > 50) {
                center = curr;
            }

            g2.setColor(Color.RED);
            g2.drawLine(curr.x,curr.y, center.x, center.y);

            gui.repaint();

            // throttle the speed just in case it's on a fast computer
            //BoofMiscOps.pause(50);
        }
        return null;
    }

    private static BufferedImage getImage(int index) {
        return UtilImageIO.loadImageNotNull("Images/DJI_00" + index +".JPG");
    }

    private static BufferedImage center1;
    private static BufferedImage previous;
    private static double distance;
    private static int indexImage;
    private static Planar<GrayF32> frame;
    private static Point2D_I32 center;
    private static Planar<GrayF32> centerImage;
    private static void nextImageButton(String fileName) {

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
        StitchingFromMotion2D<Planar<GrayF32>, Homography2D_F64>
                stitch = FactoryMotion2D.createVideoStitch(0.5, motion2DColor, ImageType.pl(3, GrayF32.class));

        // Load an image sequence
        MediaManager media = DefaultMediaManager.INSTANCE;
        SimpleImageSequence<Planar<GrayF32>> video =
                media.openVideo(fileName, ImageType.pl(3, GrayF32.class));

        int cropPixels = 8;

        frame = video.next();
        frame = frame.subimage(cropPixels, cropPixels, frame.width - cropPixels, frame.height - cropPixels);


        // shrink the input image and center it
        Homography2D_F64 shrink = new Homography2D_F64(0.5, 0, frame.width/4, 0, 0.5, frame.height/4, 0, 0, 1);
        shrink = shrink.invert(null);

        // The mosaic will be larger in terms of pixels but the image will be scaled down.
        // To change this into stabilization just make it the same size as the input with no shrink.
        stitch.configure(frame.width, frame.height, shrink);
        // process the first frame
        stitch.process(frame);

        // Create GUI panel
        int width = 1536;
        int height = 864;
        double displayScale = 1;
        // Create the GUI for displaying the results + input image
        ImageGridPanel gui = new ImageGridPanel(1, 2);
        gui.setImage(0, 1, new BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB));
        gui.setImage(0, 0, new BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB));
        gui.setPreferredSize(new Dimension(3 * frame.width, frame.height * 2));
        ShowImages.showWindow(gui, "Example Mosaic", true);
        //ShowImages.showWindow(gui, "Stitching", true);

        // Create a JFrame to hold the GUI components
        JFrame jFrame = new JFrame("Stitching");
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setLayout(new BorderLayout());

        // Create the JButtons
        JButton nextButton = new JButton("Next Image");
        JButton resetButton = new JButton("reset");
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(nextButton);
        buttonPanel.add(resetButton);

        // Add the button panel to the JFrame
        jFrame.add(buttonPanel, BorderLayout.NORTH);
        jFrame.add(gui, BorderLayout.CENTER);

        // Set the frame size and make it visible
        jFrame.setSize((int)(width*displayScale), (int)(height*displayScale));
        jFrame.setVisible(true);

        Quadrilateral_F64 cornersFirst = stitch.getImageCorners(frame.width, frame.height, null);
        center = getCenter(cornersFirst);

        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame = video.next();

                // Crop the image to remove the specified number of pixels from each side
                frame = frame.subimage(cropPixels, cropPixels, frame.width - cropPixels, frame.height - cropPixels);

                boolean success = stitch.process(frame);

                if (!success) {
                    stitch.reset();
                    stitch.process(frame);
                    return;
                }

                // if the current image is close to the image border recenter the mosaic
                Quadrilateral_F64 corners = stitch.getImageCorners(frame.width, frame.height, null);
                if (nearBorder(corners,stitch)) {
                    stitch.setOriginToCurrent();
                    corners = stitch.getImageCorners(frame.width, frame.height, null);
                }
                // display the mosaic
                ConvertBufferedImage.convertTo(frame, gui.getImage(0, 1), true);
                ConvertBufferedImage.convertTo(stitch.getStitchedImage(), gui.getImage(0, 0), true);

                // draw a quadrilateral around the current frame in the mosaic
                Graphics2D g2 = gui.getImage(0, 0).createGraphics();
                g2.setColor(Color.BLUE);

                // Draw the line between centers and update the current center if needed
                Point2D_I32 curr = getCenter(corners);
                g2.drawRect(curr.x-3,curr.y-3,6,6);

                if (center.distance(curr) > 50) {
                    center = curr;
                }

                g2.setColor(Color.RED);
                g2.drawLine(curr.x,curr.y, center.x, center.y);

                gui.repaint();
            }
        });
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stitch.reset();
                ConvertBufferedImage.convertTo(frame, gui.getImage(0, 1), true);
                ConvertBufferedImage.convertTo(stitch.getStitchedImage(), gui.getImage(0, 0), true);
                gui.repaint();
            }
        });

    }
    private static void nextImageButton2Images(String fileName) {

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
        StitchingFromMotion2D<Planar<GrayF32>, Homography2D_F64>
                stitch = FactoryMotion2D.createVideoStitch(0.5, motion2DColor, ImageType.pl(3, GrayF32.class));

        // Load an image sequence
        MediaManager media = DefaultMediaManager.INSTANCE;
        SimpleImageSequence<Planar<GrayF32>> video =
                media.openVideo(fileName, ImageType.pl(3, GrayF32.class));

        int cropPixels = 8;

        frame = video.next();
        frame = frame.subimage(cropPixels, cropPixels, frame.width - cropPixels, frame.height - cropPixels);


        // shrink the input image and center it
        Homography2D_F64 shrink = new Homography2D_F64(0.5, 0, frame.width/4, 0, 0.5, frame.height/4, 0, 0, 1);
        shrink = shrink.invert(null);

        // The mosaic will be larger in terms of pixels but the image will be scaled down.
        // To change this into stabilization just make it the same size as the input with no shrink.
        stitch.configure(frame.width, frame.height, shrink);
        // process the first frame
        stitch.process(frame);

        // Create GUI panel
        int width = 1536;
        int height = 864;
        double displayScale = 1;
        // Create the GUI for displaying the results + input image
        ImageGridPanel gui = new ImageGridPanel(1, 2);
        gui.setImage(0, 1, new BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB));
        gui.setImage(0, 0, new BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB));
        gui.setPreferredSize(new Dimension(3 * frame.width, frame.height * 2));
        ShowImages.showWindow(gui, "Example Mosaic", true);
        //ShowImages.showWindow(gui, "Stitching", true);

        // Create a JFrame to hold the GUI components
        JFrame jFrame = new JFrame("Stitching");
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setLayout(new BorderLayout());

        // Create the JButtons
        JButton nextButton = new JButton("Next Image");
        JButton resetButton = new JButton("Reset");
        JButton showCenterButton = new JButton("Show Center Image");
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(nextButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(showCenterButton);

        // Add the button panel to the JFrame
        jFrame.add(buttonPanel, BorderLayout.NORTH);
        jFrame.add(gui, BorderLayout.CENTER);

        // Set the frame size and make it visible
        jFrame.setSize((int)(width*displayScale), (int)(height*displayScale));
        jFrame.setVisible(true);

        Quadrilateral_F64 cornersFirst = stitch.getImageCorners(frame.width, frame.height, null);
        center = getCenter(cornersFirst);
        centerImage = deepCopyPlanar(frame);

        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame = video.next();

                // Crop the image to remove the specified number of pixels from each side
                frame = frame.subimage(cropPixels, cropPixels, frame.width - cropPixels, frame.height - cropPixels);

                boolean success = stitch.process(frame);

                if (!success) {
                    stitch.reset();
                    stitch.process(frame);
                    return;
                }

                // if the current image is close to the image border recenter the mosaic
                Quadrilateral_F64 corners = stitch.getImageCorners(frame.width, frame.height, null);
                if (nearBorder(corners,stitch)) {
                    stitch.setOriginToCurrent();
                    corners = stitch.getImageCorners(frame.width, frame.height, null);
                }
                // display the mosaic
                ConvertBufferedImage.convertTo(frame, gui.getImage(0, 1), true);
                ConvertBufferedImage.convertTo(stitch.getStitchedImage(), gui.getImage(0, 0), true);

                // draw a quadrilateral around the current frame in the mosaic
                Graphics2D g2 = gui.getImage(0, 0).createGraphics();
                g2.setColor(Color.BLUE);
                g2.drawLine((int)corners.a.x, (int)corners.a.y, (int)corners.b.x, (int)corners.b.y);
                g2.drawLine((int)corners.b.x, (int)corners.b.y, (int)corners.c.x, (int)corners.c.y);
                g2.drawLine((int)corners.c.x, (int)corners.c.y, (int)corners.d.x, (int)corners.d.y);
                g2.drawLine((int)corners.d.x, (int)corners.d.y, (int)corners.a.x, (int)corners.a.y);

                // Draw the line between centers and update the current center if needed
                Point2D_I32 curr = getCenter(corners);
                g2.drawRect(curr.x-3,curr.y-3,6,6);

                if (center.distance(curr) > 50) {
                    center = curr;
                }

                g2.setColor(Color.RED);
                g2.drawLine(curr.x,curr.y, center.x, center.y);

                gui.repaint();

                stitch.reset();
                stitch.process(centerImage);
            }
        });
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stitch.reset();
                ConvertBufferedImage.convertTo(frame, gui.getImage(0, 1), true);
                ConvertBufferedImage.convertTo(stitch.getStitchedImage(), gui.getImage(0, 0), true);
                gui.repaint();
            }
        });
        showCenterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stitch.reset();
                stitch.process(centerImage);
                ConvertBufferedImage.convertTo(frame, gui.getImage(0, 1), true);
                ConvertBufferedImage.convertTo(stitch.getStitchedImage(), gui.getImage(0, 0), true);
                gui.repaint();
            }
        });

    }

    private static void stitchingApp() {
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

        int width = 1536;
        int height = 864;
        double displayScale = 1;

        // This fuses the images together
        StitchingFromMotion2D<Planar<GrayF32>, Homography2D_F64>
                stitch = FactoryMotion2D.createVideoStitch(0.5, motion2DColor, ImageType.pl(3, GrayF32.class));

        Homography2D_F64 shrink = new Homography2D_F64(0.5, 0, (int)(width*displayScale)/4, 0, 0.5, (int)(height*displayScale)/4, 0, 0, 1);
        shrink = shrink.invert(null);

        // The mosaic will be larger in terms of pixels but the image will be scaled down.
        // To change this into stabilization just make it the same size as the input with no shrink.
        stitch.configure((int)(width*displayScale), (int)(height*displayScale), shrink);

        ImageGridPanel gui = new ImageGridPanel(1, 1);
        gui.setImage(0, 0, new BufferedImage((int)(width*displayScale), (int)(height*displayScale), BufferedImage.TYPE_INT_RGB));
        gui.setPreferredSize(new Dimension(3 * (int)(width*displayScale), (int)(height*displayScale) * 2));
        //ShowImages.showWindow(gui, "Stitching", true);

        // Create a JFrame to hold the GUI components
        JFrame frame = new JFrame("Stitching");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Create a JButton for image selection
        JButton stitchButton = new JButton("Stitch");
        frame.add(stitchButton, BorderLayout.NORTH);

        // Add the BoofCV ImageGridPanel to the JFrame
        frame.add(gui, BorderLayout.CENTER);

        // Set the frame size and make it visible
        frame.setSize((int)(width*displayScale), (int)(height*displayScale));
        frame.setVisible(true);

        stitchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Planar<GrayF32> im1 = new Planar<GrayF32>(GrayF32.class, 1, 1, 1);
                Planar<GrayF32> im2 = new Planar<GrayF32>(GrayF32.class, 1, 1, 1);
                Point2D_I32 center = new Point2D_I32(0,0);
                // Create a file chooser dialog
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File("C:\\Users\\adipe\\IdeaProjects\\untitled1\\Images"));
                int returnValue = fileChooser.showOpenDialog(null);

                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();

                    try {
                        im1 = convert(ImageIO.read(selectedFile));
                        stitch.process(im1);
                        Quadrilateral_F64 corners = stitch.getImageCorners(im2.width, im2.height, null);
                        center = getCenter(corners);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Error loading image.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                System.out.println("chose image 1");
                // Create a file chooser dialog
                fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File("C:\\Users\\adipe\\IdeaProjects\\untitled1\\Images"));
                returnValue = fileChooser.showOpenDialog(null);

                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();

                    try {
                        im2 = convert(ImageIO.read(selectedFile));
                        stitch.process(im2);
                        Quadrilateral_F64 corners = stitch.getImageCorners(im2.width, im2.height, null);
                        // draw a quadrilateral around the current frame in the mosaic
                        Graphics2D g2 = gui.getImage(0, 0).createGraphics();
                        g2.setColor(Color.BLUE);
                        g2.drawLine((int)corners.a.x, (int)corners.a.y, (int)corners.b.x, (int)corners.b.y);
                        g2.drawLine((int)corners.b.x, (int)corners.b.y, (int)corners.c.x, (int)corners.c.y);
                        g2.drawLine((int)corners.c.x, (int)corners.c.y, (int)corners.d.x, (int)corners.d.y);
                        g2.drawLine((int)corners.d.x, (int)corners.d.y, (int)corners.a.x, (int)corners.a.y);

                        // Draw the line between centers and update the current center if needed
                        Point2D_I32 curr = getCenter(corners);
                        g2.drawRect(curr.x-3,curr.y-3,6,6);
                        g2.setColor(Color.RED);
                        g2.drawLine(curr.x,curr.y, center.x, center.y);

                        gui.repaint();

                    } catch (IOException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Error loading image.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                System.out.println("chose image 2");


                Planar<GrayF32> result = stitch.getStitchedImage();
                BufferedImage resBuff = ConvertBufferedImage.convertTo(result,new BufferedImage(result.width,result.height,BufferedImage.TYPE_INT_RGB), true);
                showImage(resBuff);
                gui.setImage(0,0,resBuff);

                System.out.println("done");
                gui.repaint();
                stitch.reset();
            }
        });
    }

    public static void main(String[] args) {

        UtilImageIO.loadImageNotNull("Images/DJI_0021.JPG");
        //analyseVideo("Videos/DroneVideo2.mjpeg");
        analyseVideo2Images("Videos/DroneVideo2.mjpeg");
        //nextImageButton2Images("Videos/DroneVideo3.mjpeg");
        //stitchingApp();

    }
}