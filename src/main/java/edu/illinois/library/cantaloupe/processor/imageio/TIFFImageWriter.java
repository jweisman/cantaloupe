package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.MetadataCopy;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import it.geosolutions.imageio.plugins.tiff.TIFFDirectory;
import it.geosolutions.imageio.plugins.tiff.TIFFField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * TIFF image writer using ImageIO, capable of taking both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s and writing them as
 * TIFFs.
 *
 * @see <a href="http://download.java.net/media/jai-imageio/javadoc/1.1/com/sun/media/imageio/plugins/tiff/package-summary.html">
 *     ImageIO TIFF Plugin Documentation</a>
 */
class TIFFImageWriter extends AbstractImageWriter {

    private static Logger logger = LoggerFactory.getLogger(TIFFImageWriter.class);

    TIFFImageWriter(OperationList opList) {
        super(opList);
    }

    TIFFImageWriter(OperationList opList,
                    Metadata sourceMetadata) {
        super(opList, sourceMetadata);
    }

    /**
     * No-op.
     *
     * @see {@link #addMetadata(Metadata, IIOMetadata)}
     */
    @Override
    protected void addMetadata(final IIOMetadataNode baseNode) {}

    /**
     * @param sourceMetadata
     * @param derivativeMetadata
     * @return
     * @throws IOException
     */
    private IIOMetadata addMetadata(final Metadata sourceMetadata,
                                    IIOMetadata derivativeMetadata)
            throws IOException {
        if (sourceMetadata instanceof TIFFMetadata) {
            final TIFFDirectory destDir =
                    TIFFDirectory.createFromMetadata(derivativeMetadata);

            for (TIFFField field : ((TIFFMetadata) sourceMetadata).getNativeMetadata()) {
                destDir.addTIFFField(field);
            }

            final TIFFField iptcField = (TIFFField) sourceMetadata.getIPTC();
            if (iptcField != null) {
                destDir.addTIFFField(iptcField);
            }

            final TIFFField xmpField =
                    ((TIFFMetadata) sourceMetadata).getXmpField();
            if (xmpField != null) {
                destDir.addTIFFField(xmpField);
            }

            final TIFFField exifField = (TIFFField) sourceMetadata.getEXIF();
            if (exifField != null) {
                destDir.addTIFFField(exifField);
            }

            derivativeMetadata = destDir.getAsMetadata();
        }
        return derivativeMetadata;
    }

    /**
     * @return Compression type in the javax.imageio vernacular. May return
     *         <code>null</code> to indicate no equivalent or no compression.
     */
    private String getImageIOType(Compression compression) {
        switch (compression) {
            case DEFLATE:
                return "ZLib";
            case JPEG:
                return "JPEG";
            case JPEG2000:
                return "JPEG2000";
            case LZW:
                return "LZW";
            case RLE:
                return "PackBits";
            default:
                return null;
        }
    }

    /**
     * @param writer Writer to obtain the default metadata from.
     * @param writeParam Write parameters on which to base the metadata.
     * @param image Image to apply the metadata to.
     * @return Image metadata with added metadata corresponding to any
     *         writer-specific operations applied.
     * @throws IOException
     */
    @Override
    protected IIOMetadata getMetadata(final ImageWriter writer,
                            final ImageWriteParam writeParam,
                            final RenderedImage image) throws IOException {
        IIOMetadata derivativeMetadata = writer.getDefaultImageMetadata(
                ImageTypeSpecifier.createFromRenderedImage(image),
                writeParam);
        for (final Operation op : opList) {
            if (op instanceof MetadataCopy && sourceMetadata != null) {
                derivativeMetadata = addMetadata(
                        sourceMetadata, derivativeMetadata);
            }
        }
        return derivativeMetadata;
    }

    /**
     * @param writer Writer to abtain parameters for
     * @return Write parameters respecting the application configuration.
     */
    private ImageWriteParam getWriteParam(ImageWriter writer) {
        final ImageWriteParam writeParam = writer.getDefaultWriteParam();

        Encode encode = (Encode) opList.getFirst(Encode.class);
        if (encode != null) {
            final Compression compression = encode.getCompression();
            if (compression != null) {
                final String type = getImageIOType(compression);
                if (type != null) {
                    writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    writeParam.setCompressionType(type);
                    logger.debug("Compression type: {}", type);
                }
            }
        }
        return writeParam;
    }

    /**
     * Writes a Java 2D {@link BufferedImage} to the given output stream.
     *
     * @param image Image to write
     * @param outputStream Stream to write the image to
     * @throws IOException
     */
    void write(BufferedImage image, final OutputStream outputStream)
            throws IOException {
        final Iterator<ImageWriter> it = ImageIO.getImageWritersByMIMEType(
                Format.TIF.getPreferredMediaType().toString());
        while (it.hasNext()) {
            final ImageWriter writer = it.next();
            if (writer instanceof it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriter) {
                final ImageWriteParam writeParam = getWriteParam(writer);
                final IIOMetadata metadata = getMetadata(writer, writeParam, image);
                final IIOImage iioImage = new IIOImage(image, null, metadata);

                try (ImageOutputStream os =
                             ImageIO.createImageOutputStream(outputStream)) {
                    writer.setOutput(os);
                    writer.write(metadata, iioImage, writeParam);
                    os.flush(); // http://stackoverflow.com/a/14489406
                } finally {
                    writer.dispose();
                }
                break;
            }
        }
    }

    /**
     * Writes a JAI {@link PlanarImage} to the given output stream.
     *
     * @param image Image to write
     * @param outputStream Stream to write the image to
     * @throws IOException
     */
    void write(PlanarImage image, OutputStream outputStream)
            throws IOException {
        final Iterator<ImageWriter> it = ImageIO.getImageWritersByMIMEType(
                Format.TIF.getPreferredMediaType().toString());
        while (it.hasNext()) {
            final ImageWriter writer = it.next();
            if (writer instanceof it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriter) {
                final ImageWriteParam writeParam = getWriteParam(writer);
                final IIOMetadata metadata = getMetadata(writer, writeParam, image);
                final IIOImage iioImage = new IIOImage(image, null, metadata);

                try (ImageOutputStream os =
                             ImageIO.createImageOutputStream(outputStream)) {
                    writer.setOutput(os);
                    writer.write(null, iioImage, writeParam);
                    os.flush(); // http://stackoverflow.com/a/14489406
                } finally {
                    writer.dispose();
                }
                break;
            }
        }
    }

}
