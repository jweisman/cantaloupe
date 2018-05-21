package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Provides access to the shared {@link DelegateProxy} instance.
 */
public final class DelegateProxyService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DelegateProxyService.class);

    private static DelegateProxyService instance;

    private static boolean isCodeLoaded = false;

    private ScriptWatcher scriptWatcher;

    private ScheduledExecutorService watcherExecutorService;

    private Future<?> watcherFuture;

    /**
     * @return Whether the delegate script is enabled.
     */
    public static boolean isEnabled() {
        Configuration config = Configuration.getInstance();
        return config.getBoolean(Key.DELEGATE_SCRIPT_ENABLED, false);
    }

    /**
     * For testing only!
     */
    static synchronized void clearInstance() {
        instance = null;
    }

    /**
     * Returns the shared instance. If the instance is being created, the
     * delegate script code will be {@link DelegateProxy#load(String) loaded}
     * into it from the result of {@link #getScriptFile()}.
     *
     * @return Shared instance.
     */
    public static synchronized DelegateProxyService getInstance() {
        if (instance == null) {
            instance = new DelegateProxyService();
        }
        if (!isCodeLoaded) {
            try {
                Path file = getScriptFile();
                if (file != null) {
                    byte[] fileBytes = Files.readAllBytes(file);
                    String code = new String(fileBytes, "UTF-8");
                    DelegateProxy.load(code);
                    isCodeLoaded = true;
                }
            } catch (IOException | ScriptException e) {
                LOGGER.error(e.getMessage());
            }
        }
        return instance;
    }

    /**
     * @return File representing the delegate script, regardless of whether the
     *         delegate script system is enabled; or {@literal null }if {@link
     *         Key#DELEGATE_SCRIPT_PATHNAME} is not set.
     * @throws NoSuchFileException If the script specified in {@link
     *         Key#DELEGATE_SCRIPT_PATHNAME} does not exist.
     */
    static Path getScriptFile() throws NoSuchFileException {
        final Configuration config = Configuration.getInstance();
        // The script name may be an absolute pathname or a filename.
        final String configValue =
                config.getString(Key.DELEGATE_SCRIPT_PATHNAME, "");
        if (!configValue.isEmpty()) {
            Path script = findScript(configValue);
            if (!Files.exists(script)) {
                throw new NoSuchFileException(
                        "File not found: " + script.toString());
            }
            return script;
        }
        return null;
    }

    /**
     * Finds the canonical location of a script based on the given filename or
     * absolute pathname. Existence of the underlying file is not checked.
     */
    private static Path findScript(String filenameOrPathname) {
        Path script = Paths.get(filenameOrPathname);
        if (!script.isAbsolute()) {
            // Search for it in the same directory as the application config
            // (if available), or the current working directory if not.
            final File configFile = Configuration.getInstance().getFile();
            if (configFile != null) {
                script = Paths.get(configFile.getParent(),
                        script.getFileName().toString());
            } else {
                script = Paths.get(".", script.getFileName().toString());
            }
        }
        return script;
    }

    /**
     * <p>Acquires a new {@link DelegateProxy} instance.</p>
     *
     * <p>This should normally be called only once at the beginning of a
     * request lifecycle, and the returned object passed around to wherever it
     * is needed through the request lifecycle.</p>
     *
     * @param context Request context.
     * @return        Shared delegate proxy.
     * @throws DisabledException if the delegate script is disabled.
     */
    public DelegateProxy newDelegateProxy(RequestContext context)
            throws DisabledException {
        if (isEnabled()) {
            return new DelegateProxy(context);
        } else {
            throw new DisabledException();
        }
    }

    /**
     * Starts watching the delegate script for changes.
     */
    public void startWatching() {
        if (scriptWatcher == null) {
            scriptWatcher = new ScriptWatcher();
        }
        if (watcherExecutorService == null) {
            watcherExecutorService =
                    Executors.newSingleThreadScheduledExecutor();
        }
        watcherFuture = watcherExecutorService.submit(scriptWatcher);
    }

    /**
     * Stops watching the delegate script for changes.
     */
    public void stopWatching() {
        if (scriptWatcher != null) {
            scriptWatcher.stop();
            scriptWatcher = null;
        }
        if (watcherFuture != null) {
            watcherFuture.cancel(true);
        }
        watcherExecutorService.shutdown();
        watcherExecutorService = null;
    }

}
