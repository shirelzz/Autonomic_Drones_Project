package com.dji.sdk.sample.demo.stitching;

import android.graphics.Bitmap;

import boofcv.abst.feature.detect.line.DetectLine;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.core.image.impl.ImplConvertImage;
import boofcv.factory.feature.detect.line.ConfigHoughGradient;
import boofcv.factory.feature.detect.line.FactoryDetectLine;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_F32;

import java.util.LinkedList;
import java.util.List;

public class LineDetection {

    // adjusts edge threshold for identifying pixels belonging to a line. NOT CURRENTLY IN USE
    private static final float edgeThreshold = 25;
    // adjust the maximum number of found lines in the image
    private static final int maxLines = 5;

    /*public static GrayU8 edgeDetection(GrayU8 gray) {
        GrayU8 edgeImage = gray.createSameShape();
        boofcv.alg.feature.detect.edge.CannyEdge<GrayU8, GrayS16> canny = FactoryEdgeDetectors.canny(2, true, true, GrayU8.class, GrayS16.class);
        canny.process(gray, 0.1f, 0.3f, edgeImage);
        List<Contour> contours = BinaryImageOps.contourExternal(edgeImage, ConnectRule.EIGHT);

        BufferedImage visualBinary = VisualizeBinaryData.renderBinary(edgeImage, false, null);
        BufferedImage visualEdgeContour = new BufferedImage(gray.width, gray.height, BufferedImage.TYPE_INT_RGB);
        VisualizeBinaryData.render(contours, (int[])null, visualEdgeContour);

        return visualBinary;
    }*/

    /**
     * Detects lines inside the image using different types of Hough detectors
     *
     * @param bitmap Input image.
     * @param distance Distance between red line and green lines.
     */
    public static List<LineParametric2D_F32> detectLines(Bitmap bitmap, int distance ) {
        return detectLines(ConvertBitmapToBoof.bitmapToGray(bitmap,(GrayU8) null,null), distance);
    }

    /**
     * Detects lines inside the image using different types of Hough detectors
     *
     * @param input Input image.
     * @param distance Distance between red line and green lines.
     */
    public static List<LineParametric2D_F32> detectLines(GrayU8 input, int distance ) {
        GrayU8 blurred = input.createSameShape();
        GBlurImageOps.gaussian(input, blurred, 0, 5, null);
        DetectLine<GrayU8> detectorPolar = FactoryDetectLine.houghLinePolar(new ConfigHoughGradient(maxLines), null, GrayU8.class);

        List<LineParametric2D_F32> found = detectorPolar.detect(blurred);

        LinkedList<LineParametric2D_F32> newLines = new LinkedList<>();
        for (LineParametric2D_F32 line : found) {
            double m = line.getSlopeY()/line.getSlopeX();
            double b = line.getY() - m*line.getX();

            double x1 = distance*Math.sqrt(m*m+1)/(m+1/m);
            double y1 = -1/m*x1+b;
            newLines.add(new LineParametric2D_F32(new Point2D_F32((float)x1,(float)y1),line.getSlope()));

            double x2 = -1*x1;
            double y2 = -1/m*x2+b;
            newLines.add(new LineParametric2D_F32(new Point2D_F32((float)x2,(float)y2),line.getSlope()));
        }
        while (!newLines.isEmpty()) { found.add(newLines.remove()); }

        return found;
    }

}
