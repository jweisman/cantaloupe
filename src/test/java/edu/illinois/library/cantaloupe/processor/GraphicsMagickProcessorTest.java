package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * For this to work, the GraphicsMagick binaries must be on the PATH.
 */
public class GraphicsMagickProcessorTest extends MagickProcessorTest {

    private static HashMap<Format, Set<Format>> supportedFormats;

    protected HashMap<Format, Set<Format>> getAvailableOutputFormats()
            throws IOException {
        if (supportedFormats == null) {
            final Set<Format> formats = new HashSet<>();
            final Set<Format> outputFormats = new HashSet<>();

            // retrieve the output of the `gm version` command, which contains a
            // list of all optional formats
            Runtime runtime = Runtime.getRuntime();
            String[] commands = {"gm", "version"};
            Process proc = runtime.exec(commands);
            BufferedReader stdInput = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));
            String s;
            boolean read = false;
            while ((s = stdInput.readLine()) != null) {
                s = s.trim();
                if (s.contains("Feature Support")) {
                    read = true;
                } else if (s.contains("Host type:")) {
                    break;
                }
                if (read) {
                    if (s.startsWith("JPEG-2000  ") && s.endsWith(" yes")) {
                        formats.add(Format.JP2);
                        outputFormats.add(Format.JP2);
                    }
                    if (s.startsWith("JPEG  ") && s.endsWith(" yes")) {
                        formats.add(Format.JPG);
                        outputFormats.add(Format.JPG);
                    }
                    if (s.startsWith("PNG  ") && s.endsWith(" yes")) {
                        formats.add(Format.PNG);
                        outputFormats.add(Format.PNG);
                    }
                    if (s.startsWith("Ghostscript") && s.endsWith(" yes")) {
                        formats.add(Format.PDF);
                    }
                    if (s.startsWith("TIFF  ") && s.endsWith(" yes")) {
                        formats.add(Format.TIF);
                        outputFormats.add(Format.TIF);
                    }
                    if (s.startsWith("WebP  ") && s.endsWith(" yes")) {
                        formats.add(Format.WEBP);
                        outputFormats.add(Format.WEBP);
                    }
                }
            }

            // add formats that are definitely available
            // (http://www.graphicsmagick.org/formats.html)
            formats.add(Format.BMP);
            formats.add(Format.DCM);
            formats.add(Format.GIF);

            supportedFormats = new HashMap<>();
            for (Format format : Format.values()) {
                supportedFormats.put(format, new HashSet<Format>());
            }
            for (Format format : formats) {
                supportedFormats.put(format, outputFormats);
            }
        }
        return supportedFormats;
    }

    protected GraphicsMagickProcessor newInstance() {
        return new GraphicsMagickProcessor();
    }

}
