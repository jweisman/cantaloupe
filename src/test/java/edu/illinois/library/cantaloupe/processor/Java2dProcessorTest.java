package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ImageReaderFactory;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class Java2dProcessorTest extends ImageIOProcessorTest {

    private Java2dProcessor instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @Override
    protected Format getSupported16BitSourceFormat() {
        return Format.PNG;
    }

    @Override
    protected Path getSupported16BitImage() throws IOException {
        return TestUtil.getImage("png-rgb-64x56x16.png");
    }

    @Override
    protected Java2dProcessor newInstance() {
        return new Java2dProcessor();
    }

    @Test
    public void testGetSupportedFeatures() throws Exception {
        instance.setSourceFormat(getAnySupportedSourceFormat(instance));

        Set<ProcessorFeature> expectedFeatures = new HashSet<>(Arrays.asList(
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
        assertEquals(expectedFeatures, instance.getSupportedFeatures());
    }

    @Test
    public void testProcessWithAnimatedGIF() throws Exception {
        Path image = TestUtil.getImage("gif-animated-looping.gif");
        OperationList ops = new OperationList(new Encode(Format.GIF));
        Info info = Info.builder()
                .withSize(136, 200)
                .withFormat(Format.GIF)
                .build();

        instance.setSourceFile(image);
        instance.setSourceFormat(Format.GIF);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            instance.process(ops, info, os);

            try (ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray())) {
                ImageReader reader = null;
                try {
                    reader = new ImageReaderFactory().newImageReader(is, Format.GIF);
                    assertEquals(2, reader.getNumImages());
                } finally {
                    if (reader != null) {
                        reader.dispose();
                    }
                }
            }
        }
    }

}
