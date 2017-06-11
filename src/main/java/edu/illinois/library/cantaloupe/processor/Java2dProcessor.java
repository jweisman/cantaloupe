package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Normalize;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Processor using the Java 2D and ImageIO frameworks.</p>
 */
class Java2dProcessor extends AbstractJava2DProcessor
        implements StreamProcessor, FileProcessor {

    @Override
    public void process(final OperationList ops,
                        final Info imageInfo,
                        final OutputStream outputStream)
            throws ProcessorException {
        super.process(ops, imageInfo, outputStream);

        final ImageReader reader = getReader();
        try {
            final ReductionFactor rf = new ReductionFactor();
            final Set<ImageReader.Hint> hints = new HashSet<>();

            if (ops.getFirst(Normalize.class) != null) {
                // When normalizing, the reader needs to read the entire image
                // so that its histogram can be sampled accurately. This will
                // preserve the luminance across tiles.
                hints.add(ImageReader.Hint.IGNORE_CROP);
            }

            BufferedImage image = reader.read(ops, imageInfo.getOrientation(),
                    rf, hints);
            postProcess(image, hints, ops, imageInfo, rf, outputStream);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        } finally {
            reader.dispose();
        }
    }

}
