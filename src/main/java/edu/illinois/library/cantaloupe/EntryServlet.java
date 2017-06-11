package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheException;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.CacheWorkerRunner;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.logging.LoggerUtil;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.logging.velocity.Slf4jLogChute;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.restlet.data.Protocol;
import org.restlet.ext.servlet.ServerServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.FileNotFoundException;

/**
 * <p>Serves as the entry Servlet in both standalone and Servlet container
 * context. Also performs application initialization that cannot be performed
 * in {@link StandaloneEntry} as that class is not available in a Servlet
 * container context.</p>
 *
 * <p>Because this is a Restlet application, there are no other Servlet
 * classes. Instead there are Restlet resource classes residing in
 * {@link edu.illinois.library.cantaloupe.resource}.</p>
 */
public class EntryServlet extends ServerServlet {

    private static Logger logger = LoggerFactory.getLogger(EntryServlet.class);

    static final String CLEAN_CACHE_VM_ARGUMENT = "cantaloupe.cache.clean";
    static final String PURGE_CACHE_VM_ARGUMENT = "cantaloupe.cache.purge";
    static final String PURGE_EXPIRED_FROM_CACHE_VM_ARGUMENT =
            "cantaloupe.cache.purge_expired";

    static {
        // Suppress a Dock icon in OS X
        System.setProperty("java.awt.headless", "true");

        // Tell Restlet to use SLF4J instead of java.util.logging. This needs
        // to be performed before Restlet has been initialized.
        System.setProperty("org.restlet.engine.loggerFacadeClass",
                "org.restlet.ext.slf4j.Slf4jLoggerFacade");
    }

    private void handleVmArguments() {
        try {
            if (System.getProperty(CLEAN_CACHE_VM_ARGUMENT) != null) {
                Cache cache = CacheFactory.getSourceCache();
                if (cache != null) {
                    System.out.println("Cleaning the source cache...");
                    cache.cleanUp();
                } else {
                    System.out.println("Source cache is disabled.");
                }
                cache = CacheFactory.getDerivativeCache();
                if (cache != null) {
                    System.out.println("Cleaning the derivative cache...");
                    cache.cleanUp();
                } else {
                    System.out.println("Derivative cache is disabled.");
                }
                System.out.println("Done.");
                System.exit(0);
            } else if (System.getProperty(PURGE_CACHE_VM_ARGUMENT) != null) {
                // Two variants of this argument are supported:
                // -Dcantaloupe.cache.purge (purge everything)
                // -Dcantaloupe.cache.purge=identifier (purge all content
                // related to the given identifier)
                final String purgeArg =
                        System.getProperty(PURGE_CACHE_VM_ARGUMENT);
                if (purgeArg.length() > 0) {
                    Identifier identifier = new Identifier(purgeArg);
                    Cache cache = CacheFactory.getSourceCache();
                    if (cache != null) {
                        System.out.println("Purging " + identifier +
                                " from the source cache...");
                        cache.purge(identifier);
                    } else {
                        System.out.println("Source cache is disabled.");
                    }
                    cache = CacheFactory.getDerivativeCache();
                    if (cache != null) {
                        System.out.println("Purging " + identifier +
                                " from the derivative cache...");
                        cache.purge(identifier);
                    } else {
                        System.out.println("Derivative cache is disabled.");
                    }
                } else {
                    Cache cache = CacheFactory.getSourceCache();
                    if (cache != null) {
                        System.out.println("Purging the source cache...");
                        cache.purge();
                    } else {
                        System.out.println("Source cache is disabled.");
                    }
                    cache = CacheFactory.getDerivativeCache();
                    if (cache != null) {
                        System.out.println("Purging the derivative cache...");
                        cache.purge();
                    } else {
                        System.out.println("Derivative cache is disabled.");
                    }
                }
                System.out.println("Done.");
                System.exit(0);
            } else if (System.getProperty(PURGE_EXPIRED_FROM_CACHE_VM_ARGUMENT) != null) {
                Cache cache = CacheFactory.getSourceCache();
                if (cache != null) {
                    System.out.println("Purging expired items from the source cache...");
                    cache.purgeExpired();
                } else {
                    System.out.println("Source cache is disabled.");
                }
                cache = CacheFactory.getDerivativeCache();
                if (cache != null) {
                    System.out.println("Purging expired items from the derivative cache...");
                    cache.purgeExpired();
                } else {
                    System.out.println("Derivative cache is disabled.");
                }
                System.out.println("Done.");
                System.exit(0);
            }
        } catch (CacheException e) {
            System.out.println(e.getMessage());
            System.exit(-1);
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();

        // Logback has already initialized itself, which is a problem because
        // logback.xml depends on the application configuration, which at the
        // time, had not been initialized yet. So, reload it.
        LoggerUtil.reloadConfiguration();

        Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        Velocity.setProperty("classpath.resource.loader.class",
                ClasspathResourceLoader.class.getName());
        Velocity.setProperty("class.resource.loader.cache", true);
        Velocity.setProperty("runtime.log.logsystem.class",
                Slf4jLogChute.class.getCanonicalName());
        Velocity.init();

        final int mb = 1024 * 1024;
        final Runtime runtime = Runtime.getRuntime();
        logger.info(System.getProperty("java.vm.name") + " / " +
                System.getProperty("java.vm.info"));
        logger.info("{} available processor cores",
                runtime.availableProcessors());
        logger.info("Heap total: {}MB; max: {}MB", runtime.totalMemory() / mb,
                runtime.maxMemory() / mb);
        logger.info("\uD83C\uDF48 Starting Cantaloupe {}",
                Application.getVersion());

        getComponent().getClients().add(Protocol.CLAP);

        handleVmArguments();
        ConfigurationFactory.getInstance().startWatching();
        CacheWorkerRunner.start();
        try {
            ScriptEngineFactory.getScriptEngine().startWatching();
        } catch (DelegateScriptDisabledException e) {
            logger.info("init(): {}", e.getMessage());
        } catch (FileNotFoundException e) {
            logger.error("init(): file not found: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("init(): {}", e.getMessage());
        }
    }

    @Override
    public void destroy() {
        logger.info("Shutting down...");
        super.destroy();
        CacheWorkerRunner.stop();
        ConfigurationFactory.getInstance().stopWatching();
        ThreadPool.getInstance().shutdown();
        try {
            ScriptEngineFactory.getScriptEngine().stopWatching();
        } catch (DelegateScriptDisabledException e) {
            logger.info("init(): {}", e.getMessage());
        } catch (FileNotFoundException e) {
            logger.error("destroy(): file not found: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("destroy(): {}", e.getMessage());
        }
    }

}
