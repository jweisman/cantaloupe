package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.net.URL;
import java.security.ProtectionDomain;

/**
 * <p>Serves as the main application class in a standalone context.</p>
 *
 * <p>This class will be unavailable in a Servlet container, so it should not
 * be referred to externally. It should also have as few dependencies as
 * possible.</p>
 */
public class StandaloneEntry {

    /**
     * <p>When provided (no value required), a list of available fonts will be
     * printed to stdout.</p>
     *
     * <p>The main reason this is a VM option and not a command-line argument
     * is that, due to the way the application is packaged, this class needs to
     * have as few dependencies as possible. All of the other would-be
     * arguments are VM options too, so let's preserve uniformity.</p>
     */
    static String LIST_FONTS_VM_OPTION = "cantaloupe.list_fonts";

    /**
     * When set to "true", calls to {@link System#exit} will be disabled,
     * necessary for testing output-to-console-followed-by-exit.
     */
    static String TEST_VM_OPTION = "cantaloupe.test";

    private static WebServer webServer;

    static {
        System.setProperty("java.awt.headless", "true");
    }

    /**
     * Calls {@link System#exit(int)} unless {@link #isTesting()} returns
     * <code>true</code>.
     *
     * @param status Process return status.
     */
    private static void exitUnlessTesting(int status) {
        if (!isTesting()) {
            System.exit(status);
        }
    }

    /**
     * @return Whether the value of the {@link #TEST_VM_OPTION} VM option is
     *         <code>true</code>.
     */
    private static boolean isTesting() {
        return "true".equals(System.getProperty(TEST_VM_OPTION));
    }

    /**
     * <p>Checks the configuration VM option and starts the embedded web
     * container. The following configuration options are available:</p>
     *
     * <dl>
     *     <dt><code>-Dcantaloupe.config</code></dt>
     *     <dd>Use the configuration file at the corresponding pathname.
     *     Required.</dd>
     *     <dt><code>-Dcantaloupe.test</code></dt>
     *     <dd>If set to <code>true</code>, calls to {@link System#exit(int)}
     *     will be disabled. Should only be supplied when testing.</dd>
     * </dl>
     *
     * @param args Ignored.
     * @throws Exception If there is a problem starting the web server.
     */
    public static void main(String[] args) throws Exception {
        if (System.getProperty(LIST_FONTS_VM_OPTION) != null) {
            GraphicsEnvironment ge =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();
            for (String family : ge.getAvailableFontFamilyNames()) {
                System.out.println(family);
            }
            exitUnlessTesting(0);
        }

        final Configuration config = ConfigurationFactory.getInstance();
        if (config == null) {
            printUsage();
            exitUnlessTesting(-1);
        } else {
            final File configFile = config.getFile();
            if (configFile == null) {
                printUsage();
                exitUnlessTesting(-1);
            } else if (!configFile.exists()) {
                System.out.println("Does not exist: " + configFile);
                printUsage();
                exitUnlessTesting(-1);
            } else if (!configFile.isFile()) {
                System.out.println("Not a file: " + configFile);
                printUsage();
                exitUnlessTesting(-1);
            } else if (!configFile.canRead()) {
                System.out.println("Not readable: " + configFile);
                printUsage();
                exitUnlessTesting(-1);
            }
        }
        getWebServer().start();
    }

    static File getWarFile() {
        ProtectionDomain protectionDomain =
                WebServer.class.getProtectionDomain();
        URL location = protectionDomain.getCodeSource().getLocation();
        return new File(location.getFile());
    }

    /**
     * @return Application web server instance.
     */
    public static synchronized WebServer getWebServer() {
        if (webServer == null) {
            webServer = new WebServer(Configuration.getInstance());
        }
        return webServer;
    }

    /**
     * Prints program usage.
     */
    private static void printUsage() {
        System.out.println("\n" + usage());
    }

    /**
     * @return Program usage.
     */
    static String usage() {
        return "Usage: java <VM options> -jar " + getWarFile().getName() +
                "\n\n" +
                "VM options:\n" +
                "-D" + ConfigurationFactory.CONFIG_VM_ARGUMENT + "=<config>" +
                "           Configuration file (REQUIRED)\n" +
                "-D" + EntryServlet.PURGE_CACHE_VM_ARGUMENT +
                "               Purge the cache\n" +
                "-D" + EntryServlet.PURGE_CACHE_VM_ARGUMENT + "=<identifier>" +
                "  Purge items related to an identifier from the cache\n" +
                "-D" + EntryServlet.PURGE_EXPIRED_FROM_CACHE_VM_ARGUMENT +
                "       Purge expired items from the cache\n" +
                "-D" + EntryServlet.CLEAN_CACHE_VM_ARGUMENT +
                "               Clean the cache\n" +
                "-D" + StandaloneEntry.LIST_FONTS_VM_OPTION +
                "                List fonts\n";
    }

}
