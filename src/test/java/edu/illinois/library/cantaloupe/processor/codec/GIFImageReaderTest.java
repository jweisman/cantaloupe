package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class GIFImageReaderTest extends AbstractImageReaderTest {

    @Override
    GIFImageReader newInstance() throws IOException {
        GIFImageReader reader = new GIFImageReader();
        reader.setSource(TestUtil.getImage("gif"));
        return reader;
    }

    @Test
    public void testGetApplicationPreferredIIOImplementations() {
        String[] impls = ((GIFImageReader) instance).
                getApplicationPreferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.gif.GIFImageReader", impls[0]);
    }

    @Test
    @Override
    public void testGetCompression() throws IOException {
        assertEquals(Compression.LZW, instance.getCompression(0));
    }

    /* getPreferredIIOImplementations() */

    @Test
    public void testGetPreferredIIOImplementationsWithUserPreference() {
        Configuration config = Configuration.getInstance();
        config.setProperty(GIFImageReader.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");

        String userImpl = ((AbstractIIOImageReader) instance).
                getUserPreferredIIOImplementation();
        String[] appImpls = ((AbstractIIOImageReader) instance).
                getApplicationPreferredIIOImplementations();

        String[] expected = new String[appImpls.length + 1];
        expected[0] = userImpl;
        System.arraycopy(appImpls, 0, expected, 1, appImpls.length);

        assertArrayEquals(expected,
                ((AbstractIIOImageReader) instance).getPreferredIIOImplementations());
    }

    /* getUserPreferredIIOImplementation() */

    @Test
    public void testGetUserPreferredIIOImplementation() {
        Configuration config = Configuration.getInstance();
        config.setProperty(GIFImageReader.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");
        assertEquals("cats",
                ((GIFImageReader) instance).getUserPreferredIIOImplementation());
    }

    /* readSequence() */

    @Test
    @Override
    public void testReadSequence() {}

    @Test
    public void testReadSequenceWithStaticImage() throws Exception {
        BufferedImageSequence seq = instance.readSequence();
        assertEquals(1, seq.length());
    }

    @Test
    public void testReadSequenceWithAnimatedImage() throws Exception {
        instance = new GIFImageReader();
        instance.setSource(TestUtil.getImage("gif-animated-looping.gif"));
        BufferedImageSequence seq = instance.readSequence();
        assertEquals(2, seq.length());
    }

}
