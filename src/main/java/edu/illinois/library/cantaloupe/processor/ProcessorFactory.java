package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Used to obtain an instance of a {@link Processor} for a given source format,
 * as defined in the configuration.
 */
public class ProcessorFactory {

    public static Set<Processor> getAllProcessors() {
        return new HashSet<>(Arrays.asList(
                new FfmpegProcessor(),
                new GraphicsMagickProcessor(),
                new ImageMagickProcessor(),
                new JaiProcessor(),
                new Java2dProcessor(),
                new KakaduDemoProcessor(),
                new KakaduNativeProcessor(),
                new OpenJpegProcessor(),
                new PdfBoxProcessor()));
    }

    /**
     * Retrieves the best-match processor for the given source format. Its
     * source will not be set.
     *
     * @param sourceFormat Source format for which to retrieve a processor
     * @return An instance suitable for handling the given source format, based
     *         on configuration settings. The source is already set.
     */
    public Processor newProcessor(final Format sourceFormat)
            throws UnsupportedSourceFormatException,
            InitializationException,
            ReflectiveOperationException {
        String processorName = getAssignedProcessorName(sourceFormat);
        if (processorName == null) {
            processorName = getFallbackProcessorName();
            if (processorName == null) {
                throw new ClassNotFoundException("A fallback processor (" +
                        Key.PROCESSOR_FALLBACK + ") is not set.");
            }
        }

        // If the processor name contains a dot, assume it includes the package
        // name.
        final String className = processorName.contains(".") ?
                processorName :
                ProcessorFactory.class.getPackage().getName() + "." + processorName;

        try {
            final Class<?> class_ = Class.forName(className);
            final Processor processor =
                    (Processor) class_.getDeclaredConstructor().newInstance();

            InitializationException e = processor.getInitializationException();
            if (e != null) {
                throw e;
            }

            processor.setSourceFormat(sourceFormat);

            return processor;
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException(processorName + " does not exist", e);
        }
    }

    /**
     * @param format
     * @return Name of the processor assigned to the given format, or null if
     *         one is not set.
     */
    private String getAssignedProcessorName(Format format) {
        final String value = Configuration.getInstance().
                getString("processor." + format.getPreferredExtension());
        return (value != null && value.length() > 0) ? value : null;
    }

    private String getFallbackProcessorName() {
        return Configuration.getInstance().getString(Key.PROCESSOR_FALLBACK);
    }

}
