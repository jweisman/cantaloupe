package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.source.PathStreamFactory;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static edu.illinois.library.cantaloupe.test.Assert.ImageAssert.*;
import static org.junit.Assert.*;

abstract class MagickProcessorTest extends AbstractProcessorTest {

    abstract protected HashMap<Format, Set<Format>> getAvailableOutputFormats()
            throws IOException;

    @Test
    public void testGetAvailableOutputFormats() throws Exception {
        for (Format format : Format.values()) {
            try {
                StreamProcessor instance = (StreamProcessor) newInstance();
                instance.setSourceFormat(format);
                Set<Format> expectedFormats = getAvailableOutputFormats().
                        get(format);
                assertEquals(expectedFormats, instance.getAvailableOutputFormats());
            } catch (UnsupportedSourceFormatException e) {
                // continue
            }
        }
    }

    @Test
    public void testGetSupportedFeatures() throws Exception {
        StreamProcessor instance = (StreamProcessor) newInstance();
        instance.setSourceFormat(getAnySupportedSourceFormat(instance));
        Set<ProcessorFeature> expectedFeatures = new HashSet<>();
        expectedFeatures.add(ProcessorFeature.MIRRORING);
        expectedFeatures.add(ProcessorFeature.REGION_BY_PERCENT);
        expectedFeatures.add(ProcessorFeature.REGION_BY_PIXELS);
        expectedFeatures.add(ProcessorFeature.REGION_SQUARE);
        expectedFeatures.add(ProcessorFeature.ROTATION_ARBITRARY);
        expectedFeatures.add(ProcessorFeature.ROTATION_BY_90S);
        expectedFeatures.add(ProcessorFeature.SIZE_ABOVE_FULL);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_DISTORTED_WIDTH_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_PERCENT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_WIDTH);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
        assertEquals(expectedFeatures, instance.getSupportedFeatures());
    }

    @Test
    public void testProcessPreservesMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.clear();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);
        assertXmpPresent(true);
    }

    @Test
    public void testProcessStripsMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.clear();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, false);
        // Metadata shouldn't be stripped, because it would also strip the ICC
        // profile.
        assertXmpPresent(true);
    }

    private void assertXmpPresent(boolean yesOrNo) throws Exception {
        final Configuration config = Configuration.getInstance();
        config.clear();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, yesOrNo);

        OperationList ops = new OperationList();
        Rotate rotation = new Rotate(15);
        ops.add(rotation);
        ops.add(new Encode(Format.JPG));

        Info imageInfo = Info.builder().withSize(64, 58).build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final StreamProcessor instance = (StreamProcessor) newInstance();
        instance.setSourceFormat(Format.JPG);
        StreamFactory streamFactory = new PathStreamFactory(
                TestUtil.getImage("jpg-xmp.jpg"));
        instance.setStreamFactory(streamFactory);
        instance.process(ops, imageInfo, outputStream);

        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());
        Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("JPEG");
        ImageReader reader = it.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(inputStream)) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final IIOMetadataNode tree = (IIOMetadataNode)
                    metadata.getAsTree(metadata.getNativeMetadataFormatName());

            boolean found = false;
            final NodeList unknowns = tree.getElementsByTagName("unknown");
            for (int i = 0; i < unknowns.getLength(); i++) {
                if ("225".equals(unknowns.item(i).getAttributes().getNamedItem("MarkerTag").getNodeValue())) {
                    found = true;
                }
            }
            if (yesOrNo) {
                assertTrue(found);
            } else {
                assertFalse(found);
            }
        } finally {
            reader.dispose();
        }
    }

    @Test
    public void testProcessWithRotationAndCustomBackgroundColorAndTransparentOutputFormat()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.clear();
        config.setProperty(Key.PROCESSOR_BACKGROUND_COLOR, "blue");

        OperationList ops = new OperationList();
        Rotate rotation = new Rotate(15);
        ops.add(rotation);
        ops.add(new Encode(Format.PNG));

        Info imageInfo = Info.builder().withSize(64, 58).build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final StreamProcessor instance = (StreamProcessor) newInstance();
        instance.setSourceFormat(Format.JPG);
        StreamFactory streamFactory = new PathStreamFactory(
                TestUtil.getImage("jpg-rgb-64x56x8-baseline.jpg"));
        instance.setStreamFactory(streamFactory);
        instance.process(ops, imageInfo, outputStream);

        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());
        final BufferedImage rotatedImage = ImageIO.read(inputStream);

        assertRGBA(rotatedImage.getRGB(0, 0), 0, 0, 0, 0);
    }

    @Test
    public void testProcessWithRotationAndCustomBackgroundColorAndNonTransparentOutputFormat()
            throws Exception {
        OperationList ops = new OperationList();
        Rotate rotation = new Rotate(15);
        ops.add(rotation);

        Encode encode = new Encode(Format.JPG);
        encode.setBackgroundColor(Color.fromString("#0000FF"));
        ops.add(encode);

        Info imageInfo = Info.builder().withSize(64, 58).build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final StreamProcessor instance = (StreamProcessor) newInstance();
        instance.setSourceFormat(Format.JPG);
        StreamFactory streamFactory = new PathStreamFactory(
                TestUtil.getImage("jpg-rgb-64x56x8-baseline.jpg"));
        instance.setStreamFactory(streamFactory);
        instance.process(ops, imageInfo, outputStream);

        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());
        final BufferedImage rotatedImage = ImageIO.read(inputStream);

        int pixel = rotatedImage.getRGB(0, 0);
        int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        assertEquals(255, alpha);
        assertTrue(red < 10);
        assertTrue(green < 20);
        assertTrue(blue > 230);
    }

}
