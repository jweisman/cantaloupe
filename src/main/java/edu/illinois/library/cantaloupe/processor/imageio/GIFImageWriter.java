package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;
import org.w3c.dom.NodeList;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * GIF image writer using ImageIO, capable of taking both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s and writing them as
 * GIFs.
 *
 * @see <a href="http://docs.oracle.com/javase/7/docs/api/javax/imageio/package-summary.html#gif_plugin_notes">
 *     Writing GIF Images</a>
 * @see <a href="http://justsolve.archiveteam.org/wiki/GIF">GIF</a>
 */
class GIFImageWriter extends AbstractImageWriter {

    GIFImageWriter(OperationList opList) {
        super(opList);
    }

    GIFImageWriter(OperationList opList,
                   Metadata sourceMetadata) {
        super(opList, sourceMetadata);
    }

    @Override
    protected void addMetadata(final IIOMetadataNode baseTree)
            throws IOException {
        if (sourceMetadata instanceof GIFMetadata) {
            // GIF doesn't support EXIF or IPTC metadata -- only XMP.
            // The XMP node will be located at /ApplicationExtensions/
            // ApplicationExtension[@applicationID="XMP Data" @authenticationCode="XMP"]
            final byte[] xmp = sourceMetadata.getXMP();
            if (xmp != null) {
                // Get the /ApplicationExtensions node, creating it if it does
                // not exist.
                final NodeList appExtensionsList =
                        baseTree.getElementsByTagName("ApplicationExtensions");
                IIOMetadataNode appExtensions;
                if (appExtensionsList.getLength() > 0) {
                    appExtensions = (IIOMetadataNode) appExtensionsList.item(0);
                } else {
                    appExtensions = new IIOMetadataNode("ApplicationExtensions");
                    baseTree.appendChild(appExtensions);
                }

                // Create /ApplicationExtensions/ApplicationExtension
                final IIOMetadataNode appExtensionNode =
                        new IIOMetadataNode("ApplicationExtension");
                appExtensionNode.setAttribute("applicationID", "XMP Data");
                appExtensionNode.setAttribute("authenticationCode", "XMP");
                appExtensionNode.setUserObject(xmp);
                appExtensions.appendChild(appExtensionNode);
            }
        }
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
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(
                Format.GIF.getPreferredMediaType().toString());
        final ImageWriter writer = writers.next();
        final ImageWriteParam writeParam = writer.getDefaultWriteParam();
        final IIOMetadata metadata = getMetadata(writer, writeParam, image);
        final IIOImage iioImage = new IIOImage(image, null, metadata);

        try (ImageOutputStream os =
                     ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(os);
            writer.write(iioImage);
            os.flush();
        } finally {
            writer.dispose();
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
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(
                Format.GIF.getPreferredMediaType().toString());
        final ImageWriter writer = writers.next();

        // GIFWriter can't deal with a non-0,0 origin ("coordinate
        // out of bounds!")
        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add((float) -image.getMinX());
        pb.add((float) -image.getMinY());
        image = JAI.create("translate", pb);

        final ImageWriteParam writeParam = writer.getDefaultWriteParam();
        final IIOMetadata metadata = getMetadata(writer, writeParam, image);
        final IIOImage iioImage = new IIOImage(image, null, metadata);

        try (ImageOutputStream os = ImageIO.
                createImageOutputStream(outputStream)) {
            writer.setOutput(os);
            writer.write(iioImage);
            os.flush(); // http://stackoverflow.com/a/14489406
        } finally {
            writer.dispose();
        }
    }

}
