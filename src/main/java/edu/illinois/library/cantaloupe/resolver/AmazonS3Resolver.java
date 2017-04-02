package edu.illinois.library.cantaloupe.resolver;

import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;

import org.jruby.RubyArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * <p>Maps an identifier to an <a href="https://aws.amazon.com/s3/">Amazon
 * Simple Storage Service (S3)</a> object, for retrieving images from Amazon
 * S3.</p>
 *
 * <h3>Lookup Strategies</h3>
 *
 * <p>Two distinct lookup strategies are supported, defined by
 * {@link #LOOKUP_STRATEGY_CONFIG_KEY}. BasicLookupStrategy maps identifiers
 * directly to S3 object keys. ScriptLookupStrategy invokes a delegate method
 * to retrieve object keys dynamically.</p>
 *
 * @see <a href="http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/welcome.html">
 *     AWS SDK for Java</a>
 */
class AmazonS3Resolver extends AbstractResolver implements StreamResolver {

    private static class AmazonS3StreamSource implements StreamSource {

        private final S3Object object;

        AmazonS3StreamSource(S3Object object) {
            this.object = object;
        }

        @Override
        public ImageInputStream newImageInputStream() throws IOException {
            return ImageIO.createImageInputStream(newInputStream());
        }

        @Override
        public S3ObjectInputStream newInputStream() throws IOException {
            return object.getObjectContent();
        }

    }

    private static Logger logger = LoggerFactory.
            getLogger(AmazonS3Resolver.class);

    static final String ACCESS_KEY_ID_CONFIG_KEY =
            "AmazonS3Resolver.access_key_id";
    static final String BUCKET_NAME_CONFIG_KEY =
            "AmazonS3Resolver.bucket.name";
    static final String BUCKET_REGION_CONFIG_KEY =
            "AmazonS3Resolver.bucket.region";
    static final String ENDPOINT_CONFIG_KEY = "AmazonS3Resolver.endpoint";
    static final String LOOKUP_STRATEGY_CONFIG_KEY =
            "AmazonS3Resolver.lookup_strategy";
    static final String SECRET_KEY_CONFIG_KEY = "AmazonS3Resolver.secret_key";

    static final String GET_KEY_DELEGATE_METHOD =
            "AmazonS3Resolver::get_object_key";

    private static AmazonS3 client;
    private String bucketName;

    /** Lock object for synchronization */
    private static final Object lock = new Object();

    private static AmazonS3 getClientInstance() {
        if (client == null) {
            synchronized (lock) {
                final Configuration config = ConfigurationFactory.getInstance();

                class ConfigFileCredentials implements AWSCredentials {
                    @Override
                    public String getAWSAccessKeyId() {
                        return config.getString(ACCESS_KEY_ID_CONFIG_KEY);
                    }

                    @Override
                    public String getAWSSecretKey() {
                        return config.getString(SECRET_KEY_CONFIG_KEY);
                    }
                }

                AWSCredentials credentials = new ConfigFileCredentials();
                AWSCredentialsProvider chain = new AWSCredentialsProviderChain(
                		new StaticCredentialsProvider(credentials),
                		new EnvironmentVariableCredentialsProvider(),
                		new SystemPropertiesCredentialsProvider(),
                		new ProfileCredentialsProvider(),
                		new InstanceProfileCredentialsProvider(false)
                );
                
                client = new AmazonS3Client(chain);

                // a custom endpoint will be used in testing
                final String endpoint = config.getString(ENDPOINT_CONFIG_KEY);
                if (endpoint != null) {
                    logger.info("Using endpoint: {}", endpoint);
                    client.setEndpoint(endpoint);
                }

                final String regionName = config.getString(BUCKET_REGION_CONFIG_KEY);
                if (regionName != null && regionName.length() > 0) {
                    Regions regions = Regions.fromName(regionName);
                    Region region = Region.getRegion(regions);
                    logger.info("Using region: {}", region);
                    client.setRegion(region);
                }
            }
        }
        return client;
    }

    @Override
    public StreamSource newStreamSource()
            throws IOException {
        return new AmazonS3StreamSource(getObject());
    }

    private S3Object getObject() throws IOException {
        AmazonS3 s3 = getClientInstance();

        Configuration config = ConfigurationFactory.getInstance();
        bucketName = config.getString(BUCKET_NAME_CONFIG_KEY);
        final String objectKey = getObjectKey();
        logger.info("Using bucket: {}", bucketName);
        try {
            logger.info("Requesting {}", objectKey);
            return s3.getObject(new GetObjectRequest(bucketName, objectKey));
        } catch (AmazonS3Exception e) {
            if (e.getErrorCode().equals("NoSuchKey")) {
                throw new FileNotFoundException(e.getMessage());
            } else {
                throw new IOException(e);
            }
        }
    }

    private String getObjectKey() throws IOException {
        final Configuration config = ConfigurationFactory.getInstance();
        switch (config.getString(LOOKUP_STRATEGY_CONFIG_KEY)) {
            case "BasicLookupStrategy":
                return identifier.toString();
            case "ScriptLookupStrategy":
                try {
                    return getObjectKeyWithDelegateStrategy();
                } catch (ScriptException | DelegateScriptDisabledException e) {
                    logger.error(e.getMessage(), e);
                    throw new IOException(e);
                }
            default:
                throw new IOException(LOOKUP_STRATEGY_CONFIG_KEY +
                        " is invalid or not set");
        }
    }

    /**
     * @return
     * @throws FileNotFoundException If the delegate script does not exist
     * @throws IOException
     * @throws ScriptException If the script fails to execute
     */
    private String getObjectKeyWithDelegateStrategy()
            throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final Object result = engine.invoke(GET_KEY_DELEGATE_METHOD,
                identifier.toString());
        if (result == null) {
            throw new FileNotFoundException(GET_KEY_DELEGATE_METHOD +
                    " returned nil for " + identifier);
        }
        if (result.getClass() == RubyArray.class) {
        	bucketName = ((RubyArray) result).get(0).toString();
        	return ((RubyArray) result).get(1).toString();
        } else {
        	return (String) result;
        }
    }

    @Override
    public Format getSourceFormat() throws IOException {
        if (sourceFormat == null) {
            S3Object object = getObject();
            String contentType = object.getObjectMetadata().getContentType();
            // See if we can determine the format from the Content-Type header.
            if (contentType != null && !contentType.isEmpty()) {
                sourceFormat = new MediaType(contentType).toFormat();
            }
            if (sourceFormat == null || sourceFormat.equals(Format.UNKNOWN)) {
                // Try to infer a format based on the identifier.
                sourceFormat = Format.inferFormat(identifier);
            }
        }
        return sourceFormat;
    }

}
