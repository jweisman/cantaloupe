package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.ConcurrentReaderWriter;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;

public class ImageOverlayCacheTest extends BaseTest {

    private static WebServer webServer;
    private ImageOverlayCache instance;

    @BeforeClass
    public static void beforeClass() throws Exception {
        webServer = new WebServer();
        webServer.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        webServer.stop();
    }

    @Before
    public void setUp() {
        instance = new ImageOverlayCache();
    }

    // putAndGet(URI)

    @Test
    public void testPutAndGetWithPresentFileURI() throws IOException {
        URI uri = TestUtil.getImage("jpg").toUri();
        byte[] bytes = instance.putAndGet(uri);
        assertEquals(5439, bytes.length);
    }

    @Test
    public void testPutAndGetWithMissingFileURI() {
        try {
            URI uri = TestUtil.getImage("blablabla").toUri();
            instance.putAndGet(uri);
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
    }

    @Test
    public void testPutAndGetWithPresentRemoteURI() throws Exception {
        URI uri = new URI(webServer.getHTTPURI() + "/jpg");
        byte[] bytes = instance.putAndGet(uri);
        assertEquals(5439, bytes.length);
    }

    @Test
    public void testPutAndGetWithMissingRemoteURI() throws Exception {
        try {
            URI uri = new URI(webServer.getHTTPURI() + "/blablabla");
            instance.putAndGet(uri);
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
    }

    @Test
    public void testPutAndGetConcurrently() throws Exception {
        Callable<Void> callable = () -> {
            URI uri = new URI(webServer.getHTTPURI() + "/jpg");
            byte[] bytes = instance.putAndGet(uri);
            assertEquals(5439, bytes.length);
            return null;
        };
        new ConcurrentReaderWriter(callable, callable, 5000).run();
    }

}
