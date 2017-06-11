package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.cache.DerivativeFileCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resource.CachedImageRepresentation;
import edu.illinois.library.cantaloupe.resource.SourceImageWrangler;
import org.apache.commons.lang3.StringUtils;
import org.restlet.data.Disposition;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles IIIF Image API 1.1 image requests.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#url-syntax-image-request">Image
 * Request Operations</a>
 */
public class ImageResource extends IIIF1Resource {

    /**
     * Format to assume when no extension is present in the URI.
     */
    private static final Format DEFAULT_FORMAT = Format.JPG;

    /**
     * Responds to image requests.
     *
     * @return ImageRepresentation
     * @throws Exception
     */
    @Get
    public Representation doGet() throws Exception {
        final Configuration config = Configuration.getInstance();
        final Map<String,Object> attrs = this.getRequest().getAttributes();
        Identifier identifier =
                new Identifier(Reference.decode((String) attrs.get("identifier")));
        identifier = decodeSlashes(identifier);

        final Resolver resolver = new ResolverFactory().getResolver(identifier);

        // Determine the format of the source image
        Format format = Format.UNKNOWN;
        try {
            format = resolver.getSourceFormat();
        } catch (FileNotFoundException e) {
            if (config.getBoolean(Key.CACHE_SERVER_PURGE_MISSING, false)) {
                // if the image was not found, purge it from the cache
                final Cache cache = CacheFactory.getDerivativeCache();
                if (cache != null) {
                    cache.purge(identifier);
                }
            }
            throw e;
        }

        // Obtain an instance of the processor assigned to that format in
        // the config file. This will throw a variety of exceptions if there
        // are any issues.
        final Processor processor = new ProcessorFactory().getProcessor(format);

        new SourceImageWrangler(resolver, processor, identifier).wrangle();

        final Set<Format> availableOutputFormats =
                processor.getAvailableOutputFormats();

        // Extract the quality and format from the URI
        String[] qualityAndFormat = StringUtils.
                split((String) attrs.get("quality_format"), ".");
        // If a format is present, try to use that. Otherwise, guess it based
        // on the Accept header per Image API 1.1 spec section 4.5.
        String outputFormat;
        if (qualityAndFormat.length > 1) {
            outputFormat = qualityAndFormat[qualityAndFormat.length - 1];
        } else {
            outputFormat = getOutputFormat(availableOutputFormats).
                    getPreferredExtension();
        }

        // Assemble the URI parameters into an OperationList instance
        final OperationList ops = new Parameters(
                (String) attrs.get("identifier"),
                (String) attrs.get("region"),
                (String) attrs.get("size"),
                (String) attrs.get("rotation"),
                qualityAndFormat[0],
                outputFormat).toOperationList();
        ops.setIdentifier(identifier);
        ops.getOptions().putAll(
                this.getReference().getQueryAsForm(true).getValuesMap());

        final Dimension fullSize = getOrReadInfo(identifier, processor).getSize();

        processor.validate(ops, fullSize);

        final Disposition disposition = getRepresentationDisposition(
                ops.getIdentifier(), ops.getOutputFormat());

        // If we don't need to resolve first, and are using a cache, and the
        // cache contains an image matching the request, skip all the setup and
        // just return the cached image.
        final DerivativeCache cache = CacheFactory.getDerivativeCache();
        if (!config.getBoolean(Key.CACHE_SERVER_RESOLVE_FIRST, true)) {
            if (cache != null) {
                InputStream inputStream = cache.newDerivativeImageInputStream(ops);
                if (inputStream != null) {
                    return new CachedImageRepresentation(
                            ops.getOutputFormat().getPreferredMediaType(),
                            disposition, inputStream);
                }
            }
        }

        final ComplianceLevel complianceLevel = ComplianceLevel.getLevel(
                processor.getSupportedFeatures(),
                processor.getSupportedIiif1_1Qualities(),
                processor.getAvailableOutputFormats());
        getResponse().getHeaders().add("Link",
                String.format("<%s>;rel=\"profile\";", complianceLevel.getUri()));

        StringRepresentation redirectingRep = checkAuthorization(ops, fullSize);
        if (redirectingRep != null) {
            return redirectingRep;
        }

        // Will throw an exception if anything is wrong.
        checkRequest(ops, fullSize);

        ops.applyNonEndpointMutations(fullSize,
                getCanonicalClientIpAddress(),
                getReference().toUrl(),
                getRequest().getHeaders().getValuesMap(),
                getCookies().getValuesMap());

        // If the cache is enabled, and is file-based, and the file exists, add
        // an X-Sendfile header. This has to be done *after*
        // OperationList.applyNonEndpointMutations() has been called.
        if (cache != null && cache instanceof DerivativeFileCache) {
            DerivativeFileCache fileCache = (DerivativeFileCache) cache;
            if (fileCache.derivativeImageExists(ops)) {
                final String relativePathname =
                        fileCache.getRelativePathname(ops);
                addXSendfileHeader(relativePathname);
            }
        }

        // Find out whether the processor supports that source format by
        // asking it whether it offers any output formats for it
        if (!availableOutputFormats.contains(ops.getOutputFormat())) {
            String msg = String.format("%s does not support the \"%s\" output format",
                    processor.getClass().getSimpleName(),
                    ops.getOutputFormat().getPreferredExtension());
            getLogger().warning(msg + ": " + this.getReference());
            throw new UnsupportedSourceFormatException(msg);
        }

        // Add client cache header(s) if configured to do so. We do this later
        // rather than sooner to prevent them from being sent along with an
        // error response.
        getResponseCacheDirectives().addAll(getCacheDirectives());

        return getRepresentation(ops, format, disposition, processor);
    }

    /**
     * @param limitToFormats Set of OutputFormats to limit the result to.
     * @return The best output format based on the URI extension, Accept
     * header, or default, as outlined by the Image API 1.1 spec.
     */
    private Format getOutputFormat(Set<Format> limitToFormats) {
        // check the URI for a format in the extension
        Format format = null;
        for (Format f : Format.values()) {
            if (f.getPreferredExtension().
                    equals(this.getReference().getExtensions())) {
                format = f;
                break;
            }
        }
        if (format == null) { // if none, check the Accept header
            format = getPreferredOutputFormat(limitToFormats);
            if (format == null) {
                format = DEFAULT_FORMAT;
            }
        }
        return format;
    }

    /**
     * @param limitToFormats Set of OutputFormats to limit the result to.
     * @return Best OutputFormat for the client preferences as specified in the
     * Accept header.
     */
    private Format getPreferredOutputFormat(Set<Format> limitToFormats) {
        List<Variant> variants = new ArrayList<>();
        for (Format format : limitToFormats) {
            variants.add(new Variant(new org.restlet.data.MediaType(
                    format.getPreferredMediaType().toString())));
        }
        Variant preferred = getPreferredVariant(variants);
        if (preferred != null) {
            String mediaTypeStr = preferred.getMediaType().toString();
            return new MediaType(mediaTypeStr).toFormat();
        }
        return null;
    }

}
