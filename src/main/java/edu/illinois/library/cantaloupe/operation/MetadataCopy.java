package edu.illinois.library.cantaloupe.operation;

import java.awt.Dimension;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Encapsulates an operation that copies the metadata of a source image
 * into a derivative image.</p>
 *
 * <p>What constitutes "metadata" is left to the discretion of the image
 * reader and writer. In the future this class may be enhanced to distinguish
 * between EXIF, IPTC, XMP, etc.</p>
 */
public class MetadataCopy implements Operation {

    /**
     * Does nothing.
     */
    @Override
    public void freeze() {
        // no-op
    }

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return fullSize
     */
    @Override
    public Dimension getResultingSize(Dimension fullSize) {
        return fullSize;
    }

    /**
     * @return True.
     */
    @Override
    public boolean hasEffect() {
        return true;
    }

    /**
     * @param fullSize
     * @param opList
     * @return True.
     */
    @Override
    public boolean hasEffect(Dimension fullSize, OperationList opList) {
        return hasEffect();
    }

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return Single-entry map with key of <var>operation</var> pointing to
     *         <code>metadata_copy</code>.
     */
    @Override
    public Map<String, Object> toMap(Dimension fullSize) {
        final HashMap<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        return Collections.unmodifiableMap(map);
    }

    /**
     * @return The string <code>mdcopy</code>.
     */
    @Override
    public String toString() {
        return "mdcopy";
    }

}
