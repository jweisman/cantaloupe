package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.Normalize;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.Sharpen;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriter;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory;
import edu.illinois.library.cantaloupe.processor.codec.ReaderHint;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.jai.Interpolation;
import javax.media.jai.RenderedOp;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * <p>Processor using the Java Advanced Imaging (JAI) framework.</p>
 *
 * <p>Because they both use ImageIO, this processor has a lot in common with
 * {@link Java2dProcessor} and so common functionality has been extracted into
 * a base class.</p>
 *
 * @deprecated Since version 4.0.
 * @see <a href="http://docs.oracle.com/cd/E19957-01/806-5413-10/806-5413-10.pdf">
 *     Programming in Java Advanced Imaging</a>
 */
@Deprecated
class JaiProcessor extends AbstractImageIOProcessor
        implements FileProcessor, StreamProcessor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JaiProcessor.class);

    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            Collections.unmodifiableSet(EnumSet.of(
                    ProcessorFeature.MIRRORING,
                    ProcessorFeature.REGION_BY_PERCENT,
                    ProcessorFeature.REGION_BY_PIXELS,
                    ProcessorFeature.REGION_SQUARE,
                    ProcessorFeature.ROTATION_ARBITRARY,
                    ProcessorFeature.ROTATION_BY_90S,
                    ProcessorFeature.SIZE_ABOVE_FULL,
                    ProcessorFeature.SIZE_BY_DISTORTED_WIDTH_HEIGHT,
                    ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT,
                    ProcessorFeature.SIZE_BY_HEIGHT,
                    ProcessorFeature.SIZE_BY_PERCENT,
                    ProcessorFeature.SIZE_BY_WIDTH,
                    ProcessorFeature.SIZE_BY_WIDTH_HEIGHT));

    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = Collections.unmodifiableSet(EnumSet.of(
                    edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.BITONAL,
                    edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.COLOR,
                    edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.GRAY,
                    edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.NATIVE));

    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = Collections.unmodifiableSet(EnumSet.of(
                    edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.BITONAL,
                    edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.COLOR,
                    edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.DEFAULT,
                    edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.GRAY));

    /**
     * Override that disables support for GIF source images.
     */
    @Override
    public Set<Format> getAvailableOutputFormats() {
        Set<Format> formats;
        if (Format.GIF.equals(sourceFormat)) {
            formats = Collections.emptySet();
        } else {
            formats = super.getAvailableOutputFormats();
        }
        return formats;
    }

    @Override
    public Set<ProcessorFeature> getSupportedFeatures() {
        Set<ProcessorFeature> features;
        if (!getAvailableOutputFormats().isEmpty()) {
            features = SUPPORTED_FEATURES;
        } else {
            features = Collections.unmodifiableSet(Collections.emptySet());
        }
        return features;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
    getSupportedIIIF1Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality> qualities;
        if (!getAvailableOutputFormats().isEmpty()) {
            qualities = SUPPORTED_IIIF_1_1_QUALITIES;
        } else {
            qualities = Collections.unmodifiableSet(Collections.emptySet());
        }
        return qualities;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
    getSupportedIIIF2Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality> qualities;
        if (!getAvailableOutputFormats().isEmpty()) {
            qualities = SUPPORTED_IIIF_2_0_QUALITIES;
        } else {
            qualities = Collections.unmodifiableSet(Collections.emptySet());
        }
        return qualities;
    }

    @Override
    public void process(final OperationList opList,
                        final Info imageInfo,
                        final OutputStream outputStream)
            throws ProcessorException {
        super.process(opList, imageInfo, outputStream);

        ImageReader reader = null;
        try {
            reader = getReader();
            final Format outputFormat = opList.getOutputFormat();
            final Orientation orientation = getEffectiveOrientation();
            final Dimension fullSize = imageInfo.getSize();
            final ReductionFactor rf = new ReductionFactor();
            final Set<ReaderHint> hints =
                    EnumSet.noneOf(ReaderHint.class);

            final boolean normalize = (opList.getFirst(Normalize.class) != null);
            if (normalize) {
                // When normalizing, the reader needs to read the entire image
                // so that its histogram can be sampled accurately. This will
                // preserve the luminance across tiles.
                hints.add(ReaderHint.IGNORE_CROP);
            }

            final RenderedImage renderedImage = reader.readRendered(opList,
                    orientation, rf, hints);
            RenderedOp renderedOp = JAIUtil.getAsRenderedOp(
                    RenderedOp.wrapRenderedImage(renderedImage));

            // Normalize the image, if specified in the configuration.
            if (normalize) {
                renderedOp = JAIUtil.stretchContrast(renderedOp);
            }

            // If the Encode specifies a max sample size of 8 bits, or if the
            // output format's max sample size is 8 bits, we will need to
            // reduce it. HOWEVER, if the output format's max sample size is
            // LESS THAN 8 bits (I'm looking at you, GIF), don't do anything
            // and let the writer handle it.
            Encode encode = (Encode) opList.getFirst(Encode.class);
            if (((encode != null && encode.getMaxComponentSize() <= 8)
                    || outputFormat.getMaxSampleSize() <= 8)
                    && !Format.GIF.equals(outputFormat)) {
                renderedOp = JAIUtil.rescalePixels(renderedOp);
                renderedOp = JAIUtil.reduceTo8Bits(renderedOp);
            }

            for (Operation op : opList) {
                if (op.hasEffect(fullSize, opList)) {
                    if (op instanceof Crop) {
                        renderedOp = JAIUtil.cropImage(renderedOp, (Crop) op, rf);
                    } else if (op instanceof Scale) {
                        /*
                        JAI has a bug that causes it to fail on certain right-
                        edge compressed TIFF tiles when using the
                        SubsampleAverage operation, as well as the Scale
                        operation with any interpolation other than nearest-
                        neighbor. The error is an ArrayIndexOutOfBoundsException
                        in PlanarImage.cobbleByte().

                        Issue: https://github.com/medusa-project/cantaloupe/issues/94
                        Example: /iiif/2/champaign-pyramidal-tiled-lzw.tif/8048,0,800,6928/99,/0/default.jpg

                        So, the strategy here is:
                        1) if the TIFF is compressed, use the Scale operation with
                           nearest-neighbor interpolation, which is horrible, but
                           better than nothing.
                        2) otherwise, use the SubsampleAverage operation.
                        */
                        if (sourceFormat.equals(Format.TIF) &&
                                (!reader.getCompression(0).equals(Compression.UNCOMPRESSED) &&
                                        !reader.getCompression(0).equals(Compression.UNDEFINED))) {
                            LOGGER.debug("process(): detected compressed TIFF; " +
                                    "using the Scale operation with nearest-" +
                                    "neighbor interpolation.");
                            renderedOp = JAIUtil.scaleImage(renderedOp, (Scale) op,
                                    Interpolation.getInstance(Interpolation.INTERP_NEAREST),
                                    rf);
                        } else if (renderedOp.getWidth() < 3 ||
                                renderedOp.getHeight() < 3) {
                            // SubsampleAverage requires the image to be at least 3
                            // pixels on a side. So, again use the Scale operation,
                            // with a better (but still bad [but it doesn't matter
                            // because of the tiny dimension(s)]) filter.
                            renderedOp = JAIUtil.scaleImage(renderedOp, (Scale) op,
                                    Interpolation.getInstance(Interpolation.INTERP_BILINEAR),
                                    rf);
                        } else {
                            // All clear to use SubsampleAverage.
                            renderedOp = JAIUtil.scaleImageUsingSubsampleAverage(
                                    renderedOp, (Scale) op, rf);
                        }
                    } else if (op instanceof Transpose) {
                        renderedOp = JAIUtil.
                                transposeImage(renderedOp, (Transpose) op);
                    } else if (op instanceof Rotate) {
                        renderedOp = JAIUtil.rotateImage(renderedOp, (Rotate) op);
                    } else if (op instanceof ColorTransform) {
                        renderedOp = JAIUtil.
                                transformColor(renderedOp, (ColorTransform) op);
                    } else if (op instanceof Sharpen) {
                        renderedOp = JAIUtil.
                                sharpenImage(renderedOp, (Sharpen) op);
                    }
                }
            }

            // Apply remaining operations.
            BufferedImage image = null;
            for (Operation op : opList) {
                if (op instanceof Overlay && op.hasEffect(fullSize, opList)) {
                    // Let's cheat and apply the overlay using Java 2D.
                    // There seems to be minimal performance penalty in doing
                    // this, and doing it in JAI is harder (or impossible in
                    // the case of drawing text).
                    image = renderedOp.getAsBufferedImage();
                    Java2DUtil.applyOverlay(image, (Overlay) op);
                }
            }
            final ImageWriter writer = new ImageWriterFactory().newImageWriter(
                    opList, reader.getMetadata(0));

            if (image != null) {
                writer.write(image, outputStream);
            } else {
                writer.write(renderedOp, outputStream);
            }
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

}
