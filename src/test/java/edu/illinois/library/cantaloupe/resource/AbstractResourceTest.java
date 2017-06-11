package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;
import org.restlet.data.Header;
import org.restlet.data.Reference;
import org.restlet.util.Series;

import static org.junit.Assert.*;

public class AbstractResourceTest extends BaseTest {

    private static class TestResource extends AbstractResource {
    }

    private TestResource resource = new TestResource();

    @Test
    public void testGetPublicRootRefUsingConfiguration() {
        final String uri1 = "http://example.net/cats";
        final String uri2 = "http://example.org/dogs";
        Configuration.getInstance().setProperty(Key.BASE_URI, uri1);

        Series<Header> headers = new Series<>(Header.class);

        Reference ref = AbstractResource.getPublicRootRef(
                new Reference(uri2), headers);
        assertEquals(uri1, ref.toString());
    }

    @Test
    public void testGetPublicRootRefUsingXForwardedHeaders() {
        Reference rootRef = new Reference("http://bogus");

        // HTTP & port 80
        Series<Header> headers = new Series<>(Header.class);
        headers.set("X-Forwarded-Proto", "HTTP");
        headers.set("X-Forwarded-Host", "example.org");
        headers.set("X-Forwarded-Port", "80");
        headers.set("X-Forwarded-Path", "/");
        Reference ref = AbstractResource.getPublicRootRef(rootRef, headers);
        assertEquals("http://example.org", ref.toString());

        // HTTP & port != 80
        headers.set("X-Forwarded-Proto", "HTTP");
        headers.set("X-Forwarded-Host", "example.org");
        headers.set("X-Forwarded-Port", "85");
        headers.set("X-Forwarded-Path", "/");
        ref = AbstractResource.getPublicRootRef(rootRef, headers);
        assertEquals("http://example.org:85", ref.toString());

        // HTTPS & port 443
        headers.set("X-Forwarded-Proto", "HTTPS");
        headers.set("X-Forwarded-Host", "example.org");
        headers.set("X-Forwarded-Port", "443");
        headers.set("X-Forwarded-Path", "/");
        ref = AbstractResource.getPublicRootRef(rootRef, headers);
        assertEquals("https://example.org", ref.toString());

        // HTTPS & port != 443
        headers.set("X-Forwarded-Proto", "HTTPS");
        headers.set("X-Forwarded-Host", "example.org");
        headers.set("X-Forwarded-Port", "450");
        headers.set("X-Forwarded-Path", "/");
        ref = AbstractResource.getPublicRootRef(rootRef, headers);
        assertEquals("https://example.org:450", ref.toString());

        // Multiple values
        headers.set("X-Forwarded-Proto", "http,http");
        headers.set("X-Forwarded-Host", "example.org,example.mil");
        headers.set("X-Forwarded-Port", "80,8080");
        headers.set("X-Forwarded-Path", "/cats,/dogs");
        ref = AbstractResource.getPublicRootRef(rootRef, headers);
        assertEquals("http://example.org/cats", ref.toString());
    }

    @Test
    public void testGetPublicRootRefFallsBackToRequest() {
        final String uri = "http://example.net/cats";

        Series<Header> headers = new Series<>(Header.class);

        Reference ref = AbstractResource.getPublicRootRef(
                new Reference(uri), headers);
        assertEquals(uri, ref.toString());
    }

    @Test
    public void testTemplateWithValidTemplate() {
        assertNotNull(resource.template("/error.vm"));
    }

    @Test
    public void testTemplateWithInvalidTemplate() {
        assertNotNull(resource.template("/bogus.vm"));
    }

}
