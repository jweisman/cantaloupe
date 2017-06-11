package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class PdfBoxProcessorTest extends ProcessorTest {

    private PdfBoxProcessor instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.PDFBOXPROCESSOR_DPI, 72);
        instance = newInstance();
    }

    protected PdfBoxProcessor newInstance() {
        PdfBoxProcessor proc = new PdfBoxProcessor();
        try {
            proc.setSourceFormat(Format.PDF);
        } catch (UnsupportedSourceFormatException e) {
            fail("Huge bug");
        }
        return proc;
    }

    @Test
    public void testGetSupportedFeatures() throws Exception {
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
    public void testProcessWithPageOption() throws Exception {
        instance.setSourceFile(TestUtil.getImage("pdf-multipage.pdf"));
        final Info imageInfo = instance.readImageInfo();

        // page option missing
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OperationList ops = TestUtil.newOperationList();
        instance.process(ops, imageInfo, outputStream);
        final byte[] page1 = outputStream.toByteArray();

        // page option present
        ops.getOptions().put("page", "2");
        outputStream = new ByteArrayOutputStream();
        instance.process(ops, imageInfo, outputStream);
        final byte[] page2 = outputStream.toByteArray();

        assertFalse(Arrays.equals(page1, page2));
    }

    @Test
    public void testProcessWithIllegalPageOptionThrowsException()
            throws Exception {
        instance.setSourceFile(TestUtil.getImage("pdf-multipage.pdf"));
        final Info imageInfo = instance.readImageInfo();

        // page "35"
        OperationList ops = TestUtil.newOperationList();
        ops.getOptions().put("page", "35");
        OutputStream outputStream = new NullOutputStream();
        try {
            instance.process(ops, imageInfo, outputStream);
            fail("Expected exception");
        } catch (ProcessorException e) {
            // pass
        }
    }

    @Test
    @Override
    public void testReadImageInfo() throws Exception {
        Info expectedInfo = new Info(100, 88, 100, 88, Format.PDF);
        instance.setSourceFile(TestUtil.getImage("pdf.pdf"));
        instance.setSourceFormat(Format.PDF);
        assertEquals(expectedInfo, instance.readImageInfo());
    }

    @Test
    public void testValidate() throws Exception {
        instance.setSourceFile(TestUtil.getImage("pdf.pdf"));

        OperationList ops = TestUtil.newOperationList();
        Dimension fullSize = new Dimension(1000, 1000);
        instance.validate(ops, fullSize);

        ops.getOptions().put("page", "1");
        instance.validate(ops, fullSize);

        ops.getOptions().put("page", "3");
        try {
            instance.validate(ops, fullSize);
            fail("Expected exception");
        } catch (ValidationException e) {
            // pass
        }

        ops.getOptions().put("page", "0");
        try {
            instance.validate(ops, fullSize);
            fail("Expected exception");
        } catch (ValidationException e) {
            // pass
        }

        ops.getOptions().put("page", "-1");
        try {
            instance.validate(ops, fullSize);
            fail("Expected exception");
        } catch (ValidationException e) {
            // pass
        }
    }

}
