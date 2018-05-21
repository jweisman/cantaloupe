package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Sharpen;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Transpose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.TransposeDescriptor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;

/**
 * @see <a href="http://docs.oracle.com/cd/E19957-01/806-5413-10/806-5413-10.pdf">
 *     Programming in Java Advanced Imaging</a>
 * @deprecated Since version 4.0.
 */
@Deprecated
final class JAIUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JAIUtil.class);

    /**
     * @param inImage Image to crop.
     * @param crop    Crop operation. Clients should call
     *                {@link Operation#hasEffect(Dimension, OperationList)}
     *                before invoking.
     * @return Cropped image, or the input image if the given operation is a
     *         no-op.
     */
    static RenderedOp cropImage(RenderedOp inImage, Crop crop) {
        return cropImage(inImage, crop, new ReductionFactor(0));
    }

    /**
     * Crops the given image taking into account a reduction factor. In other
     * words, the dimensions of the input image have already been halved
     * <code>reductionFactor</code> times but the given crop region is relative
     * to the full-sized image.
     *
     * @param inImage Image to crop.
     * @param crop    Crop operation. Clients should call
     *                {@link Operation#hasEffect(Dimension, OperationList)}
     *                before invoking.
     * @param rf      Number of times the dimensions of
     *                <code>inImage</code> have already been halved relative to
     *                the full-sized version.
     * @return Cropped image, or the input image if the given operation is a
     *         no-op.
     */
    static RenderedOp cropImage(RenderedOp inImage,
                                Crop crop,
                                ReductionFactor rf) {
        if (crop.hasEffect()) {
            final Rectangle cropRegion = crop.getRectangle(
                    new Dimension(inImage.getWidth(), inImage.getHeight()), rf);
            LOGGER.debug("cropImage(): x: {}; y: {}; width: {}; height: {}",
                    cropRegion.x, cropRegion.y,
                    cropRegion.width, cropRegion.height);
            final ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);
            pb.add((float) cropRegion.x);
            pb.add((float) cropRegion.y);
            pb.add((float) cropRegion.width);
            pb.add((float) cropRegion.height);
            inImage = JAI.create("crop", pb);
        }
        return inImage;
    }

    /**
     * @param inImage Image to get a RenderedOp of.
     * @return RenderedOp version of <code>inImage</code>.
     */
    static RenderedOp getAsRenderedOp(PlanarImage inImage) {
        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(inImage);
        return JAI.create("null", pb);
    }

    /**
     * <p>Reduces an image's component size to 8 bits if greater.</p>
     *
     * <p>Pixel values will not be rescaled.</p>
     *
     * @param inImage Image to reduce.
     * @return Reduced image, or the input image if it already is 8 bits or
     *         less.
     * @see #rescalePixels(RenderedOp)
     */
    static RenderedOp reduceTo8Bits(RenderedOp inImage) {
        final int componentSize = inImage.getColorModel().getComponentSize(0);
        if (componentSize > 8) {
            final ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);

            LOGGER.debug("reduceTo8Bits(): converting {}-bit to 8-bit",
                    componentSize);

            // See Programming in Java Advanced Imaging sec. 4.5 for an
            // explanation of the Format operation.
            inImage = JAI.create("format", pb, inImage.getRenderingHints());
        }
        return inImage;
    }

    /**
     * Linearly scales the pixel values of the given image into an 8-bit range.
     *
     * @param inImage Image to rescale.
     * @return Rescaled image.
     * @see #reduceTo8Bits(RenderedOp)
     */
    static RenderedOp rescalePixels(RenderedOp inImage) {
        final int targetSize = 8;
        final int componentSize = inImage.getColorModel().getComponentSize(0);
        if (componentSize != targetSize) {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);

            final double multiplier = Math.pow(2, targetSize) /
                    Math.pow(2, componentSize);
            // Per-band constants to multiply by.
            final double[] constants = {multiplier};
            pb.add(constants);

            // Per-band offsets to be added.
            final double[] offsets = {0};
            pb.add(offsets);

            LOGGER.debug("rescalePixels(): multiplying by {}", multiplier);
            inImage = JAI.create("rescale", pb);
        }
        return inImage;
    }

    /**
     * @param inImage Image to rotate
     * @param rotate  Rotate operation
     * @return Rotated image, or the input image if the given rotate operation
     *         is a no-op.
     */
    static RenderedOp rotateImage(RenderedOp inImage, Rotate rotate) {
        if (rotate.hasEffect()) {
            LOGGER.debug("rotateImage(): rotating {} degrees",
                    rotate.getDegrees());
            final ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);
            pb.add(inImage.getWidth() / 2.0f);                   // x origin
            pb.add(inImage.getHeight() / 2.0f);                  // y origin
            pb.add((float) Math.toRadians(rotate.getDegrees())); // radians
            pb.add(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
            inImage = JAI.create("rotate", pb);
        }
        return inImage;
    }

    /**
     * <p>Scales an image using the JAI <code>Scale</code> operator, taking an
     * already-applied reduction factor into account. (In other words, the
     * dimensions of the input image have already been halved
     * <code>reductionFactor</code> times but the given size is relative to the
     * full-sized image.)</p>
     *
     * <p>N.B. The image quality of the <code>Scale</code> operator is quite
     * poor and so {@link #scaleImageUsingSubsampleAverage(RenderedOp, Scale,
     * ReductionFactor)} should be used instead, if possible.</p>
     *
     * @param inImage       Image to scale.
     * @param scale         Requested size ignoring any reduction factor.
     *                      Clients should call
     *                      {@link Operation#hasEffect(Dimension, OperationList)}
     *                      before invoking.
     * @param interpolation Interpolation.
     * @param rf            Reduction factor that has already been applied to
     *                      <code>inImage</code>.
     * @return Scaled image, or the input image if the given scale is a no-op.
     */
    static RenderedOp scaleImage(RenderedOp inImage,
                                 Scale scale,
                                 Interpolation interpolation,
                                 ReductionFactor rf) {
        if (scale.hasEffect()) {
            final int sourceWidth = inImage.getWidth();
            final int sourceHeight = inImage.getHeight();
            final Dimension scaledSize = scale.getResultingSize(
                    new Dimension(sourceWidth, sourceHeight));

            double xScale = scaledSize.width / (double) sourceWidth;
            double yScale = scaledSize.height / (double) sourceHeight;
            if (scale.getPercent() != null) {
                xScale = scale.getPercent() / rf.getScale();
                yScale = scale.getPercent() / rf.getScale();
            }

            LOGGER.debug("scaleImage(): width: {}%; height: {}%",
                    xScale * 100, yScale * 100);
            final ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);
            pb.add((float) xScale);
            pb.add((float) yScale);
            pb.add(0.0f);
            pb.add(0.0f);
            pb.add(interpolation);
            inImage = JAI.create("scale", pb);
        }
        return inImage;
    }

    /**
     * <p>Better-quality alternative to {@link #scaleImage(RenderedOp, Scale,
     * Interpolation, ReductionFactor)} using JAI's
     * <code>SubsampleAverage</code> operator.</p>
     *
     * <p>N.B. The <code>SubsampleAverage</code> operator is not capable of
     * upscaling. If asked to upscale, this method will use the inferior-quality
     * <code>Scale</code> operator instead.</p>
     *
     * @param inImage Image to scale. Must be at least 3 pixels on the
     *                smallest side.
     * @param scale   Requested size ignoring any reduction factor. Clients
     *                should call
     *                {@link Operation#hasEffect(Dimension, OperationList)}
     *                before invoking.
     * @param rf      Reduction factor that has already been applied to
     *                <code>inImage</code>.
     * @return Scaled image, or the input image if the given scale is a no-op.
     */
    static RenderedOp scaleImageUsingSubsampleAverage(RenderedOp inImage,
                                                      Scale scale,
                                                      ReductionFactor rf) {
        final int sourceWidth = inImage.getWidth();
        final int sourceHeight = inImage.getHeight();
        final Dimension fullSize = new Dimension(sourceWidth, sourceHeight);

        if (scale.isUp(fullSize)) {
            LOGGER.debug("scaleImageUsingSubsampleAverage(): can't upscale; " +
                    "invoking scaleImage() instead");
            return scaleImage(inImage, scale,
                    Interpolation.getInstance(Interpolation.INTERP_BILINEAR),
                    rf);
        } else if (scale.hasEffect()) {
            final Dimension scaledSize = scale.getResultingSize(
                    new Dimension(sourceWidth, sourceHeight));

            double xScale = scaledSize.width / (double) sourceWidth;
            double yScale = scaledSize.height / (double) sourceHeight;
            if (scale.getPercent() != null) {
                xScale = scale.getPercent() / rf.getScale();
                yScale = scale.getPercent() / rf.getScale();
            }

            LOGGER.debug("scaleImageUsingSubsampleAverage(): " +
                            "width: {}%; height: {}%",
                    xScale * 100, yScale * 100);
            final ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);
            pb.add(xScale);
            pb.add(yScale);
            pb.add(0.0); // X translation
            pb.add(0.0); // Y translation

            final RenderingHints hints = new RenderingHints(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            inImage = JAI.create("SubsampleAverage", pb, hints);
        }
        return inImage;
    }

    /**
     * @param inImage Image to sharpen.
     * @param sharpen The sharpen operation.
     * @return Sharpened image.
     */
    static RenderedOp sharpenImage(RenderedOp inImage,
                                   final Sharpen sharpen) {
        if (sharpen.hasEffect()) {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);
            pb.add(null);
            pb.add(sharpen.getAmount());
            inImage = JAI.create("UnsharpMask", pb);
        }
        return inImage;
    }

    /**
     * <p>Linearly stretches the contrast of an image to occupy the full range
     * of intensities. Histogram gaps will result.</p>
     *
     * <p>Does not work with indexed images.</p>
     *
     * @param inImage Image to stretch.
     * @return Stretched image.
     */
    static RenderedOp stretchContrast(RenderedOp inImage) {
        final int numLevels =
                (int) Math.pow(2, inImage.getColorModel().getComponentSize(0));
        final byte[] blut = new byte[numLevels];
        for (int i = 0; i < numLevels; i++) {
            blut[i] = (byte) (i >> 4);
        }

        final Dimension fullSize = new Dimension(inImage.getWidth(),
                inImage.getHeight());
        final Rectangle sampleArea = new Rectangle(0, 0,
                fullSize.width, fullSize.height);

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(inImage);
        pb.add(new ROIShape(sampleArea));
        pb.add(1); // Horizontal sampling rate
        pb.add(1); // Vertical sampling rate
        RenderedOp op = JAI.create("extrema", pb);

        // Retrieve both the maximum and minimum pixel value.
        final double[][] extrema = (double[][]) op.getProperty("extrema");
        int min = numLevels, max = 0;
        for (int i = 0; i < inImage.getNumBands(); i++) {
            if (extrema[0][i] < min) {
                min = (int) extrema[0][i];
            }
            if (extrema[1][i] > max) {
                max = (int) extrema[1][i];
            }
        }

        double scale = 255f / (float) (max - min);
        for (int i = min; i <= max; i++) {
            blut[i] = (byte) ((i - min) * scale);
        }

        // Clamp any input values outside min/max range.
        for (int i = 0; i < min; i++) {
            blut[i] = 0;
        }
        for (int i = max; i < numLevels; i++) {
            blut[i] = (byte) 255;
        }

        pb = new ParameterBlock();
        pb.addSource(inImage);
        pb.add(new LookupTableJAI(blut));
        return JAI.create("lookup", pb);
    }

    /**
     * @param inImage        Image to filter
     * @param colorTransform Color transform operation
     * @return Transformed image, or the input image if the given operation
     *         is a no-op.
     */
    @SuppressWarnings({"deprecation"}) // really, JAI itself is basically deprecated
    static RenderedOp transformColor(RenderedOp inImage,
                                     ColorTransform colorTransform) {
        // convert to grayscale
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(inImage);
        final int numBands = OpImage.getExpandedNumBands(
                inImage.getSampleModel(), inImage.getColorModel());
        double[][] matrix = new double[1][numBands + 1];
        matrix[0][0] = 0.114;
        matrix[0][1] = 0.587;
        matrix[0][2] = 0.299;
        for (int i = 3; i <= numBands; i++) {
            matrix[0][i] = 0;
        }
        pb.add(matrix);
        RenderedOp filteredImage = JAI.create("bandcombine", pb, null);
        if (ColorTransform.BITONAL.equals(colorTransform)) {
            pb = new ParameterBlock();
            pb.addSource(filteredImage);
            pb.add(1.0 * 128);
            filteredImage = JAI.create("binarize", pb);
        }
        return filteredImage;
    }

    /**
     * @param inImage   Image to transpose.
     * @param transpose The transpose operation.
     * @return Transposed image, or the input image if the given transpose
     *         operation is a no-op.
     */
    static RenderedOp transposeImage(RenderedOp inImage, Transpose transpose) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(inImage);
        switch (transpose) {
            case HORIZONTAL:
                LOGGER.debug("transposeImage(): horizontal");
                pb.add(TransposeDescriptor.FLIP_HORIZONTAL);
                break;
            case VERTICAL:
                LOGGER.debug("transposeImage(): vertical");
                pb.add(TransposeDescriptor.FLIP_VERTICAL);
                break;
        }
        return JAI.create("transpose", pb);
    }

    private JAIUtil() {}

}
