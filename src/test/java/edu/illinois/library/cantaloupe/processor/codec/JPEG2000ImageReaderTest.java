package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.*;

public class JPEG2000ImageReaderTest extends AbstractImageReaderTest {

    @Override
    JPEG2000ImageReader newInstance() throws IOException {
        JPEG2000ImageReader reader = new JPEG2000ImageReader();
        reader.setSource(TestUtil.getImage("jp2"));
        return reader;
    }

    @Test
    @Override
    public void testGetCompression() throws IOException {
        assertEquals(Compression.JPEG2000, instance.getCompression(0));
    }

    @Test
    @Override
    public void testGetNumResolutions() throws Exception {
        assertEquals(6, instance.getNumResolutions());
    }

    @Test
    @Override
    public void testGetPreferredIIOImplementationsWithNoUserPreference() {}

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testRead() throws Exception {
        instance.read();
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testReadWithArguments() throws Exception {
        OperationList ops = new OperationList();
        Orientation orientation = Orientation.ROTATE_0;
        ReductionFactor rf = new ReductionFactor();
        Set<ReaderHint> hints = EnumSet.noneOf(ReaderHint.class);

        instance.read(ops, orientation, rf, hints);
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testReadRendered() throws Exception {
        instance.read();
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testReadRenderedWithArguments() throws Exception {
        OperationList ops = new OperationList();
        Orientation orientation = Orientation.ROTATE_0;
        ReductionFactor rf = new ReductionFactor();
        Set<ReaderHint> hints = EnumSet.noneOf(ReaderHint.class);

        instance.readRendered(ops, orientation, rf, hints);
    }

}
