package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.resource.iiif.ImageInfoUtil;

import java.awt.Dimension;

abstract class ImageInfoFactory {

    /** Will be used to calculate a maximum scale factor. */
    private static final int MIN_SIZE = 64;

    static ImageInfo newImageInfo(final String imageUri,
                                  final Processor processor,
                                  final Info cacheInfo)
            throws ProcessorException {
        // We want to use the orientation-aware full size, which takes the
        // embedded orientation into account.
        final Dimension virtualSize = cacheInfo.getOrientationSize();

        final ComplianceLevel complianceLevel = ComplianceLevel.getLevel(
                processor.getSupportedFeatures(),
                processor.getSupportedIiif1_1Qualities(),
                processor.getAvailableOutputFormats());

        final int minTileSize = ConfigurationFactory.getInstance().
                getInt(Key.IIIF_MIN_TILE_SIZE, 1024);

        // Find a tile width and height. If the image is not tiled,
        // calculate a tile size close to MIN_TILE_SIZE_CONFIG_KEY pixels.
        // Otherwise, use the smallest multiple of the tile size above
        // MIN_TILE_SIZE_CONFIG_KEY of image resolution 0.
        final Info.Image firstImage =
                cacheInfo.getImages().get(0);
        Dimension virtualTileSize = firstImage.getOrientationTileSize();

        if (cacheInfo.getImages().size() > 0) {
            if (!virtualTileSize.equals(virtualSize)) {
                virtualTileSize = ImageInfoUtil.smallestTileSize(virtualSize,
                        firstImage.getOrientationTileSize(),
                        minTileSize);
            }
        }

        // Create an Info instance, which will eventually be serialized
        // to JSON and sent as the response body.
        final ImageInfo imageInfo = new ImageInfo();
        imageInfo.id = imageUri;
        imageInfo.width = virtualSize.width;
        imageInfo.height = virtualSize.height;
        imageInfo.profile = complianceLevel.getUri();
        imageInfo.tileWidth = virtualTileSize.width;
        imageInfo.tileHeight = virtualTileSize.height;

        // scale factors
        int maxReductionFactor =
                ImageInfoUtil.maxReductionFactor(virtualSize, MIN_SIZE);
        for (int i = 0; i <= maxReductionFactor; i++) {
            imageInfo.scaleFactors.add((int) Math.pow(2, i));
        }

        // formats
        for (Format format : processor.getAvailableOutputFormats()) {
            imageInfo.formats.add(format.getPreferredExtension());
        }

        // qualities
        for (Quality quality : processor.getSupportedIiif1_1Qualities()) {
            imageInfo.qualities.add(quality.toString().toLowerCase());
        }

        return imageInfo;
    }

}
