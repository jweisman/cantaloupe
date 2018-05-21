package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;

public class UnsupportedOutputFormatException extends IllegalArgumentException {

    public UnsupportedOutputFormatException() {
        super("Unsupported output format");
    }

    public UnsupportedOutputFormatException(String message) {
        super(message);
    }

    public UnsupportedOutputFormatException(Format format) {
        super("Unsupported output format: " + format.getName());
    }

    public UnsupportedOutputFormatException(Processor processor, Format format) {
        super(String.format("%s does not support the \"%s\" output format",
                processor.getClass().getSimpleName(),
                format.getName()));
    }

}
