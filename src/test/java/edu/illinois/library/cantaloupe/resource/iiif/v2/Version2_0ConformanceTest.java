package edu.illinois.library.cantaloupe.resource.iiif.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.junit.Ignore;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

/**
 * <p>Functional test of conformance to the IIIF Image API 2.0 spec. Methods
 * are implemented in the order of the assertions in the spec document.</p>
 *
 * @see <a href="http://iiif.io/api/image/2.0/#image-information">IIIF Image
 * API 2.0</a>
 */
public class Version2_0ConformanceTest extends ResourceTest {

    static final Identifier IMAGE =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    private String getBaseUri() {
        return "http://localhost:" + PORT + RestletApplication.IIIF_2_PATH;
    }

    /**
     * 2. "When the base URI is dereferenced, the interaction should result in
     * the Image Information document. It is recommended that the response be a
     * 303 status redirection to the Image Information document’s URI."
     *
     * @throws IOException
     */
    @Test
    public void testBaseUriReturnsImageInfoViaHttp303() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE);
        client.setFollowingRedirects(false);
        client.get();
        assertEquals(Status.REDIRECTION_SEE_OTHER, client.getStatus());
        assertEquals(getBaseUri() + "/" + IMAGE + "/info.json",
                client.getLocationRef().toString());
    }

    /**
     * 3. "All special characters (e.g. ? or #) [in an identifier] must be URI
     * encoded to avoid unpredictable client behaviors. The URI syntax relies
     * upon slash (/) separators so any slashes in the identifier must be URI
     * encoded (also called “percent encoded”).
     *
     * @throws IOException
     */
    @Test
    public void testIdentifierWithEncodedCharacters() throws IOException {
        // override the filesystem prefix to one folder level up so we can use
        // a slash in the identifier
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        Path path = Paths.get(cwd, "src", "test", "resources");
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX,
                path + File.separator);

        // image endpoint
        String identifier = Reference.encode("images/" + IMAGE);
        ClientResource client = getClientForUriPath("/iiif/2/" + identifier + "/full/full/0/default.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
        // information endpoint
        client = getClientForUriPath("/iiif/2/" + identifier + "/info.json");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.1
     *
     * @throws IOException
     */
    @Test
    public void testFullRegion() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/full/0/default.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    /**
     * 4.1
     *
     * @throws IOException
     */
    @Test
    public void testAbsolutePixelRegion() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/20,20,100,100/full/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(44, image.getWidth());
        assertEquals(36, image.getHeight());
    }

    /**
     * 4.1
     *
     * @throws IOException
     */
    @Test
    public void testPercentageRegion() throws IOException {
        // with ints
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/pct:20,20,50,50/full/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(32, image.getWidth());
        assertEquals(28, image.getHeight());

        // with floats
        client = getClientForUriPath("/iiif/2/" + IMAGE + "/pct:20.2,20.6,50.2,50.6/full/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        rep = client.getResponseEntity();
        image = ImageIO.read(rep.getStream());
        assertEquals(32, image.getWidth());
        assertEquals(28, image.getHeight());
    }

    /**
     * 4.1. "If the request specifies a region which extends beyond the
     * dimensions reported in the Image Information document, then the service
     * should return an image cropped at the image’s edge, rather than adding
     * empty space."
     *
     * @throws IOException
     */
    @Test
    public void testAbsolutePixelRegionLargerThanSource() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/0,0,99999,99999/full/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    /**
     * 4.1. "If the requested region’s height or width is zero, or if the
     * region is entirely outside the bounds of the reported dimensions, then
     * the server should return a 400 status code."
     *
     * @throws IOException
     */
    @Test
    public void testPixelRegionOutOfBounds() throws IOException {
        // zero width/height
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/0,0,0,0/full/0/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }

        // x/y out of bounds
        client = getClientForUriPath("/iiif/2/" + IMAGE + "/99999,99999,50,50/full/0/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
    }

    /**
     * The IIIF API Validator wants the server to return 400 for a bogus
     * (junk characters) region.
     *
     * @throws IOException
     */
    @Test
    public void testBogusRegion() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/ca%20ioU/full/0/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
    }

    /**
     * 4.2
     *
     * @throws IOException
     */
    @Test
    public void testFullSize() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/full/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    /**
     * 4.2. "The extracted region should be scaled so that its width is
     * exactly equal to w, and the height will be a calculated value that
     * maintains the aspect ratio of the extracted region."
     *
     * @throws IOException
     */
    @Test
    public void testSizeScaledToFitWidth() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/50,/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(50, image.getWidth());
        assertEquals(44, image.getHeight());
    }

    /**
     * 4.2. "The extracted region should be scaled so that its height is
     * exactly equal to h, and the width will be a calculated value that
     * maintains the aspect ratio of the extracted region."
     *
     * @throws IOException
     */
    @Test
    public void testSizeScaledToFitHeight() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/,50/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(57, image.getWidth());
        assertEquals(50, image.getHeight());
    }

    /**
     * 4.2. "The width and height of the returned image is scaled to n% of the
     * width and height of the extracted region. The aspect ratio of the
     * returned image is the same as that of the extracted region."
     *
     * @throws IOException
     */
    @Test
    public void testSizeScaledToPercent() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/pct:50/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(32, image.getWidth());
        assertEquals(28, image.getHeight());
    }

    /**
     * 4.2. "The width and height of the returned image are exactly w and h.
     * The aspect ratio of the returned image may be different than the
     * extracted region, resulting in a distorted image."
     *
     * @throws IOException
     */
    @Test
    public void testAbsoluteWidthAndHeight() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/50,50/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(50, image.getWidth());
        assertEquals(50, image.getHeight());
    }

    /**
     * 4.2. "The image content is scaled for the best fit such that the
     * resulting width and height are less than or equal to the requested
     * width and height. The exact scaling may be determined by the service
     * provider, based on characteristics including image quality and system
     * performance. The dimensions of the returned image content are
     * calculated to maintain the aspect ratio of the extracted region."
     *
     * @throws IOException
     */
    @Test
    public void testSizeScaledToFitInside() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/20,20/0/default.jpg");
        client.get();
        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(20, image.getWidth());
        assertEquals(20, image.getHeight());
    }

    /**
     * 4.2. "If the resulting height or width is zero, then the server should
     * return a 400 (bad request) status code."
     *
     * @throws IOException
     */
    @Test
    public void testResultingWidthOrHeightIsZero() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/pct:0/15/color.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }

        client = getClientForUriPath("/iiif/2/wide.jpg/full/3,0/15/color.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
    }

    /**
     * IIIF Image API 2.0 doesn't say anything about an invalid size
     * parameter, so we will check for an HTTP 400.
     *
     * @throws IOException
     */
    @Test
    public void testInvalidSize() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/cats/0/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        } finally {
            client.release();
        }

        client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/cats,50/0/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        } finally {
            client.release();
        }

        client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/50,cats/0/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        } finally {
            client.release();
        }

        client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/cats,/0/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        } finally {
            client.release();
        }

        client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/,cats/0/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        } finally {
            client.release();
        }

        client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/!cats,50/0/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        } finally {
            client.release();
        }
    }

    /**
     * 4.3. "The degrees of clockwise rotation from 0 up to 360."
     *
     * @throws IOException
     */
    @Test
    public void testRotation() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/full/15.5/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.3. "The image should be mirrored and then rotated as above."
     *
     * @throws IOException
     */
    @Test
    public void testMirroredRotation() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/full/!15/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.3. "A rotation value that is out of range or unsupported should result
     * in a 400 status code."
     *
     * @throws IOException
     */
    @Test
    public void testInvalidRotation() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/full/-15/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }

        client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/full/385/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
    }

    /**
     * 4.4. "The image is returned in full color."
     *
     * @throws IOException
     */
    @Test
    public void testColorQuality() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/full/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.4. "The image is returned in grayscale, where each pixel is black,
     * white or any shade of gray in between."
     *
     * @throws IOException
     */
    @Test
    public void testGrayQuality() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/full/0/gray.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.4. "The image returned is bitonal, where each pixel is either black or
     * white."
     *
     * @throws IOException
     */
    @Test
    public void testBitonalQuality() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/full/0/bitonal.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.4. "The image is returned using the server’s default quality (e.g.
     * color, gray or bitonal) for the image."
     *
     * @throws IOException
     */
    @Test
    public void testDefaultQuality() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/full/0/default.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.4. "A quality value that is unsupported should result in a 400 status
     * code."
     *
     * @throws IOException
     */
    @Test
    public void testUnsupportedQuality() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/full/0/bogus.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
    }

    /**
     * 4.5
     *
     * @throws IOException
     */
    @Test
    public void testFormats() throws Exception {
        testFormat(Format.JPG);
        testFormat(Format.TIF);
        testFormat(Format.PNG);
        testFormat(Format.GIF);
        testFormat(Format.JP2);
        testFormat(Format.PDF);
        testFormat(Format.WEBP);
    }

    private void testFormat(Format format) throws Exception {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE +
                "/full/full/0/default." + format.getPreferredExtension());

        // does the current processor support this output format?
        Format sourceFormat = Format.inferFormat(IMAGE);
        Processor processor = new ProcessorFactory().getProcessor(sourceFormat);
        if (processor.getAvailableOutputFormats().contains(format)) {
            client.get();
            assertEquals(Status.SUCCESS_OK, client.getStatus());
            assertEquals(format.getPreferredMediaType().toString(),
                    client.getResponse().getHeaders().getFirst("Content-Type").getValue());
        } else {
            try {
                client.get();
                fail("Expected exception");
            } catch (ResourceException e) {
                assertEquals(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE,
                        client.getStatus());
            }
        }
    }

    /**
     * 4.5
     *
     * @throws IOException
     */
    @Test
    public void testUnsupportedFormat() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/full/0/default.bogus");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
    }

    /**
     * 4.7. "When the client requests an image, the server may add a link
     * header to the response that indicates the canonical URI for that
     * request."
     *
     * @throws IOException
     */
    @Test
    public void testCanonicalUriLinkHeader() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/full/full/0/default.jpg");
        client.get();
        assertEquals("<" + getBaseUri() + "/" + IMAGE +
                        "/full/full/0/default.jpg>;rel=\"canonical\"",
                client.getResponse().getHeaders().getFirst("Link").getValue());
    }

    /**
     * 5. "The service must return this information about the image."
     *
     * @throws IOException
     */
    @Test
    public void testInformationRequest() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/info.json");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 5. "The content-type of the response must be either “application/json”,
     * (regular JSON), or “application/ld+json” (JSON-LD)."
     *
     * @throws IOException
     */
    @Test
    public void testInformationRequestContentType() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/info.json");
        client.get();
        assertEquals("application/json;charset=utf-8",
                client.getResponse().getHeaders().getFirst("Content-Type").
                        getValue().replace(" ", "").toLowerCase());
    }

    /**
     * 5. "If the client explicitly wants the JSON-LD content-type, then it
     * must specify this in an Accept header, otherwise the server must return
     * the regular JSON content-type."
     *
     * @throws IOException
     */
    @Test
    public void testInformationRequestContentTypeJsonLd() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/info.json");
        client.accept(new MediaType("application/ld+json"));
        client.get();
        assertEquals("application/ld+json; charset=UTF-8",
                client.getResponse().getHeaders().getFirst("Content-Type").getValue());

        client = getClientForUriPath("/iiif/2/" + IMAGE + "/info.json");
        client.accept(new MediaType("application/json"));
        client.get();
        assertEquals("application/json;charset=utf-8",
                client.getResponse().getHeaders().getFirst("Content-Type").
                        getValue().replace(" ", "").toLowerCase());
    }

    /**
     * 5. "Servers should send the Access-Control-Allow-Origin header with the
     * value * in response to information requests."
     *
     * @throws IOException
     */
    @Test
    @Ignore // TODO: possibly a restlet bug: http://restlet-discuss.1400322.n2.nabble.com/How-can-I-set-Origin-and-Access-Control-Request-Method-when-doing-a-request-td7579398.html
    public void testInformationRequestCorsHeader() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/info.json");
        client.getRequest().getHeaders().set("Origin", "*");
        client.get();
        assertEquals("*", client.getResponse().getAccessControlAllowOrigin());
    }

    /**
     * 5.1
     *
     * @throws IOException
     */
    @Test
    public void testInformationRequestJson() throws IOException {
        // this will be tested in ImageInfoFactoryTest
    }

    /**
     * 5.1. "If any of formats, qualities, or supports have no additional
     * values beyond those specified in the referenced compliance level, then
     * the property should be omitted from the response rather than being
     * present with an empty list."
     *
     * @throws IOException
     */
    @Test
    public void testInformationRequestEmptyJsonProperties() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/info.json");
        assertFalse(client.get().getText().contains("null"));
    }

    /**
     * 6. "The Image Information document must ... include a compliance level
     * URI as the first entry in the profile property."
     *
     * @throws IOException
     */
    @Test
    public void testComplianceLevel() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/2/" + IMAGE + "/info.json");
        String json = client.get().getText();
        ObjectMapper mapper = new ObjectMapper();
        ImageInfo info = mapper.readValue(json, ImageInfo.class);
        List profile = (List) info.get("profile");
        assertEquals("http://iiif.io/api/image/2/level2.json",
                profile.get(0));
    }

}
