package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.*;

public class OpenJpegProcessorTest extends AbstractProcessorTest {

    private OpenJpegProcessor instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration.getInstance().clearProperty(
                Key.OPENJPEGPROCESSOR_PATH_TO_BINARIES);
        OpenJpegProcessor.resetInitialization();

        instance = newInstance();
    }

    @Override
    protected Format getSupported16BitSourceFormat() {
        return Format.JP2;
    }

    @Override
    protected Path getSupported16BitImage() throws IOException {
        return TestUtil.getImage("jp2-5res-rgb-64x56x16-monotiled-lossy.jp2");
    }

    @Override
    protected OpenJpegProcessor newInstance() {
        OpenJpegProcessor proc = new OpenJpegProcessor();
        try {
            proc.setSourceFormat(Format.JP2);
        } catch (UnsupportedSourceFormatException e) {
            fail("Huge bug");
        }
        return proc;
    }

    @Test
    public void testGetInitializationExceptionWithNoException() {
        assertNull(instance.getInitializationException());
    }

    @Test
    public void testGetInitializationExceptionWithMissingBinaries() {
        Configuration.getInstance().setProperty(
                Key.OPENJPEGPROCESSOR_PATH_TO_BINARIES,
                "/bogus/bogus/bogus");
        OpenJpegProcessor.resetInitialization();
        assertNotNull(instance.getInitializationException());
    }

    @Test
    public void testGetSupportedFeatures() {
        Set<ProcessorFeature> expectedFeatures = EnumSet.of(
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
                ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
        assertEquals(expectedFeatures, instance.getSupportedFeatures());
    }

    @Override
    @Test
    public void testProcessOf16BitImageWithEncodeOperationLimitingTo8Bits() {
        // Skipped. See OpenJpegProcessor's class doc for an explanation of why
        // it doesn't support 16-bit output.
    }

    @Override
    @Test
    public void testProcessOf16BitImageWithEncodeOperationWithNoLimit() {
        // Skipped. See OpenJpegProcessor's class doc for an explanation of why
        // it doesn't support 16-bit output.
    }

    @Test
    public void testGetWarningsWithNoWarnings() {
        boolean initialValue = OpenJpegProcessor.isQuietModeSupported();
        try {
            OpenJpegProcessor.setQuietModeSupported(true);
            assertEquals(0, instance.getWarnings().size());
        } finally {
            OpenJpegProcessor.setQuietModeSupported(initialValue);
        }
    }

    @Test
    public void testGetWarningsWithWarnings() {
        boolean initialValue = OpenJpegProcessor.isQuietModeSupported();
        try {
            OpenJpegProcessor.setQuietModeSupported(false);
            assertEquals(1, instance.getWarnings().size());
        } finally {
            OpenJpegProcessor.setQuietModeSupported(initialValue);
        }
    }

    @Test
    public void testReadImageInfoTileAwareness() throws Exception {
        // untiled image
        instance.setSourceFile(TestUtil.getImage("jp2-5res-rgb-64x56x8-monotiled-lossy.jp2"));
        Info expectedInfo = Info.builder()
                .withSize(64, 56)
                .withTileSize(64, 56)
                .withFormat(Format.JP2)
                .withNumResolutions(5)
                .build();
        assertEquals(expectedInfo, instance.readImageInfo());

        // tiled image
        instance.setSourceFile(TestUtil.getImage("jp2-6res-rgb-64x56x8-multitiled-lossy.jp2"));
        expectedInfo = Info.builder()
                .withSize(64, 56)
                .withTileSize(32, 28)
                .withNumResolutions(6)
                .withFormat(Format.JP2)
                .build();
        assertEquals(expectedInfo, instance.readImageInfo());
    }

}
