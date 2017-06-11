package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.*;

public class FormatTest extends BaseTest {

    @Test
    public void testInferFormatWithIdentifier() {
        // AVI
        assertEquals(Format.AVI,
                Format.inferFormat(new Identifier("bla.avi")));
        assertEquals(Format.AVI,
                Format.inferFormat(new Identifier("bla.AVI")));
        // BMP
        assertEquals(Format.BMP,
                Format.inferFormat(new Identifier("bla.bmp")));
        // DCM
        assertEquals(Format.DCM,
                Format.inferFormat(new Identifier("bla.dcm")));
        // GIF
        assertEquals(Format.GIF,
                Format.inferFormat(new Identifier("bla.gif")));
        // JP2
        assertEquals(Format.JP2,
                Format.inferFormat(new Identifier("bla.jp2")));
        // JPG
        assertEquals(Format.JPG,
                Format.inferFormat(new Identifier("bla.jpg")));
        // MOV
        assertEquals(Format.MOV,
                Format.inferFormat(new Identifier("bla.mov")));
        // MP4
        assertEquals(Format.MP4,
                Format.inferFormat(new Identifier("bla.mp4")));
        // MPG
        assertEquals(Format.MPG,
                Format.inferFormat(new Identifier("bla.mpg")));
        // PDF
        assertEquals(Format.PDF,
                Format.inferFormat(new Identifier("bla.pdf")));
        // PNG
        assertEquals(Format.PNG,
                Format.inferFormat(new Identifier("bla.png")));
        // SGI
        assertEquals(Format.SGI,
                Format.inferFormat(new Identifier("bla.sgi")));
        // SID
        assertEquals(Format.SID,
                Format.inferFormat(new Identifier("bla.sid")));
        // TIF
        assertEquals(Format.TIF,
                Format.inferFormat(new Identifier("bla.tif")));
        // WEBM
        assertEquals(Format.WEBM,
                Format.inferFormat(new Identifier("bla.webm")));
        // WEBP
        assertEquals(Format.WEBP,
                Format.inferFormat(new Identifier("bla.webp")));
        // UNKNOWN
        assertEquals(Format.UNKNOWN,
                Format.inferFormat(new Identifier("bla.bogus")));
    }

    @Test
    public void testGetExtensions() {
        // AVI
        assertEquals(Arrays.asList("avi"), Format.AVI.getExtensions());
        // BMP
        assertEquals(Arrays.asList("bmp", "dib"), Format.BMP.getExtensions());
        // DCM
        assertEquals(Arrays.asList("dcm", "dic"), Format.DCM.getExtensions());
        // GIF
        assertEquals(Arrays.asList("gif"), Format.GIF.getExtensions());
        // JP2
        assertEquals(Arrays.asList("jp2", "j2k"), Format.JP2.getExtensions());
        // JPG
        assertEquals(Arrays.asList("jpg", "jpeg"), Format.JPG.getExtensions());
        // MOV
        assertEquals(Arrays.asList("mov"), Format.MOV.getExtensions());
        // MP4
        assertEquals(Arrays.asList("mp4", "m4v"), Format.MP4.getExtensions());
        // MPG
        assertEquals(Arrays.asList("mpg"), Format.MPG.getExtensions());
        // PDF
        assertEquals(Arrays.asList("pdf"), Format.PDF.getExtensions());
        // PNG
        assertEquals(Arrays.asList("png"), Format.PNG.getExtensions());
        // SGI
        assertEquals(Arrays.asList("sgi", "rgb", "rgba", "bw", "int", "inta"),
                Format.SGI.getExtensions());
        // SID
        assertEquals(Arrays.asList("sid"), Format.SID.getExtensions());
        // TIF
        assertEquals(Arrays.asList("tif", "ptif", "tiff"),
                Format.TIF.getExtensions());
        // WEBM
        assertEquals(Arrays.asList("webm"), Format.WEBM.getExtensions());
        // WEBP
        assertEquals(Arrays.asList("webp"), Format.WEBP.getExtensions());
        // UNKNOWN
        assertEquals(Arrays.asList("unknown"), Format.UNKNOWN.getExtensions());
    }

    @Test
    public void testGetMediaTypes() {
        // AVI
        assertEquals(Arrays.asList(
                new MediaType("video/avi"),
                new MediaType("video/msvideo"),
                new MediaType("video/x-msvideo")),
                Format.AVI.getMediaTypes());
        // BMP
        assertEquals(Arrays.asList(
                new MediaType("image/bmp"),
                new MediaType("image/x-bmp"),
                new MediaType("image/x-ms-bmp")),
                Format.BMP.getMediaTypes());
        // DCM
        assertEquals(Arrays.asList(
                new MediaType("application/dicom")),
                Format.DCM.getMediaTypes());
        // GIF
        assertEquals(Arrays.asList(
                new MediaType("image/gif")),
                Format.GIF.getMediaTypes());
        // JP2
        assertEquals(Arrays.asList(
                new MediaType("image/jp2")),
                Format.JP2.getMediaTypes());
        // JPG
        assertEquals(Arrays.asList(
                new MediaType("image/jpeg")),
                Format.JPG.getMediaTypes());
        // MOV
        assertEquals(Arrays.asList(
                new MediaType("video/quicktime"),
                new MediaType("video/x-quicktime")),
                Format.MOV.getMediaTypes());
        // MP4
        assertEquals(Arrays.asList(
                new MediaType("video/mp4")),
                Format.MP4.getMediaTypes());
        // MPG
        assertEquals(Arrays.asList(
                new MediaType("video/mpeg")),
                Format.MPG.getMediaTypes());
        // PDF
        assertEquals(Arrays.asList(
                new MediaType("application/pdf")),
                Format.PDF.getMediaTypes());
        // PNG
        assertEquals(Arrays.asList(
                new MediaType("image/png")),
                Format.PNG.getMediaTypes());
        // SGI
        assertEquals(Arrays.asList(
                new MediaType("image/sgi")),
                Format.SGI.getMediaTypes());
        // SID
        assertEquals(Arrays.asList(
                new MediaType("image/x-mrsid"),
                new MediaType("image/x.mrsid"),
                new MediaType("image/x-mrsid-image")),
                Format.SID.getMediaTypes());
        // TIF
        assertEquals(Arrays.asList(
                new MediaType("image/tiff")),
                Format.TIF.getMediaTypes());
        // WEBM
        assertEquals(Arrays.asList(
                new MediaType("video/webm")),
                Format.WEBM.getMediaTypes());
        // WEBP
        assertEquals(Arrays.asList(
                new MediaType("image/webp")),
                Format.WEBP.getMediaTypes());
        // UNKNOWN
        assertEquals(Arrays.asList(
                new MediaType("unknown/unknown")),
                Format.UNKNOWN.getMediaTypes());
    }

    @Test
    public void testGetName() {
        assertEquals("AVI", Format.AVI.getName());
        assertEquals("BMP", Format.BMP.getName());
        assertEquals("DICOM", Format.DCM.getName());
        assertEquals("GIF", Format.GIF.getName());
        assertEquals("JPEG2000", Format.JP2.getName());
        assertEquals("JPEG", Format.JPG.getName());
        assertEquals("QuickTime", Format.MOV.getName());
        assertEquals("MPEG-4", Format.MP4.getName());
        assertEquals("MPEG", Format.MPG.getName());
        assertEquals("PDF", Format.PDF.getName());
        assertEquals("PNG", Format.PNG.getName());
        assertEquals("SGI", Format.SGI.getName());
        assertEquals("MrSID", Format.SID.getName());
        assertEquals("TIFF", Format.TIF.getName());
        assertEquals("WebM", Format.WEBM.getName());
        assertEquals("WebP", Format.WEBP.getName());
        assertEquals("Unknown", Format.UNKNOWN.getName());
    }

    @Test
    public void testGetPreferredExtension() {
        assertEquals("avi", Format.AVI.getPreferredExtension());
        assertEquals("bmp", Format.BMP.getPreferredExtension());
        assertEquals("dcm", Format.DCM.getPreferredExtension());
        assertEquals("gif", Format.GIF.getPreferredExtension());
        assertEquals("jp2", Format.JP2.getPreferredExtension());
        assertEquals("jpg", Format.JPG.getPreferredExtension());
        assertEquals("mov", Format.MOV.getPreferredExtension());
        assertEquals("mp4", Format.MP4.getPreferredExtension());
        assertEquals("mpg", Format.MPG.getPreferredExtension());
        assertEquals("pdf", Format.PDF.getPreferredExtension());
        assertEquals("png", Format.PNG.getPreferredExtension());
        assertEquals("sgi", Format.SGI.getPreferredExtension());
        assertEquals("sid", Format.SID.getPreferredExtension());
        assertEquals("tif", Format.TIF.getPreferredExtension());
        assertEquals("webm", Format.WEBM.getPreferredExtension());
        assertEquals("webp", Format.WEBP.getPreferredExtension());
        assertEquals("unknown", Format.UNKNOWN.getPreferredExtension());
    }

    @Test
    public void testGetPreferredMediaType() {
        assertEquals("video/avi",
                Format.AVI.getPreferredMediaType().toString());
        assertEquals("image/bmp",
                Format.BMP.getPreferredMediaType().toString());
        assertEquals("application/dicom",
                Format.DCM.getPreferredMediaType().toString());
        assertEquals("image/gif",
                Format.GIF.getPreferredMediaType().toString());
        assertEquals("image/jp2",
                Format.JP2.getPreferredMediaType().toString());
        assertEquals("image/jpeg",
                Format.JPG.getPreferredMediaType().toString());
        assertEquals("video/quicktime",
                Format.MOV.getPreferredMediaType().toString());
        assertEquals("video/mp4",
                Format.MP4.getPreferredMediaType().toString());
        assertEquals("video/mpeg",
                Format.MPG.getPreferredMediaType().toString());
        assertEquals("application/pdf",
                Format.PDF.getPreferredMediaType().toString());
        assertEquals("image/png",
                Format.PNG.getPreferredMediaType().toString());
        assertEquals("image/sgi",
                Format.SGI.getPreferredMediaType().toString());
        assertEquals("image/x-mrsid",
                Format.SID.getPreferredMediaType().toString());
        assertEquals("image/tiff",
                Format.TIF.getPreferredMediaType().toString());
        assertEquals("video/webm",
                Format.WEBM.getPreferredMediaType().toString());
        assertEquals("image/webp",
                Format.WEBP.getPreferredMediaType().toString());
        assertEquals("unknown/unknown",
                Format.UNKNOWN.getPreferredMediaType().toString());
    }

    @Test
    public void testGetType() {
        assertEquals(Format.Type.VIDEO, Format.AVI.getType());
        assertEquals(Format.Type.IMAGE, Format.BMP.getType());
        assertEquals(Format.Type.IMAGE, Format.DCM.getType());
        assertEquals(Format.Type.IMAGE, Format.GIF.getType());
        assertEquals(Format.Type.IMAGE, Format.JP2.getType());
        assertEquals(Format.Type.IMAGE, Format.JPG.getType());
        assertEquals(Format.Type.VIDEO, Format.MOV.getType());
        assertEquals(Format.Type.VIDEO, Format.MP4.getType());
        assertEquals(Format.Type.VIDEO, Format.MPG.getType());
        assertEquals(Format.Type.IMAGE, Format.PDF.getType());
        assertEquals(Format.Type.IMAGE, Format.PNG.getType());
        assertEquals(Format.Type.IMAGE, Format.SGI.getType());
        assertEquals(Format.Type.IMAGE, Format.SID.getType());
        assertEquals(Format.Type.IMAGE, Format.TIF.getType());
        assertEquals(Format.Type.VIDEO, Format.WEBM.getType());
        assertEquals(Format.Type.IMAGE, Format.WEBP.getType());
        assertNull(Format.UNKNOWN.getType());
    }

    @Test
    public void testIsImage() {
        assertFalse(Format.AVI.isImage());
        assertTrue(Format.BMP.isImage());
        assertTrue(Format.DCM.isImage());
        assertTrue(Format.GIF.isImage());
        assertTrue(Format.JP2.isImage());
        assertTrue(Format.JPG.isImage());
        assertFalse(Format.MOV.isImage());
        assertFalse(Format.MP4.isImage());
        assertFalse(Format.MPG.isImage());
        assertTrue(Format.PDF.isImage());
        assertTrue(Format.PNG.isImage());
        assertTrue(Format.SGI.isImage());
        assertTrue(Format.SID.isImage());
        assertTrue(Format.TIF.isImage());
        assertFalse(Format.WEBM.isImage());
        assertTrue(Format.WEBP.isImage());
        assertFalse(Format.UNKNOWN.isImage());
    }

    @Test
    public void testIsVideo() {
        assertTrue(Format.AVI.isVideo());
        assertFalse(Format.BMP.isVideo());
        assertFalse(Format.DCM.isVideo());
        assertFalse(Format.GIF.isVideo());
        assertFalse(Format.JP2.isVideo());
        assertFalse(Format.JPG.isVideo());
        assertTrue(Format.MOV.isVideo());
        assertTrue(Format.MP4.isVideo());
        assertTrue(Format.MPG.isVideo());
        assertFalse(Format.PDF.isVideo());
        assertFalse(Format.PNG.isVideo());
        assertFalse(Format.SGI.isVideo());
        assertFalse(Format.SID.isVideo());
        assertFalse(Format.TIF.isVideo());
        assertTrue(Format.WEBM.isVideo());
        assertFalse(Format.WEBP.isVideo());
        assertFalse(Format.UNKNOWN.isVideo());
    }

    @Test
    public void testSupportsTransparency() {
        assertFalse(Format.AVI.supportsTransparency());
        assertTrue(Format.BMP.supportsTransparency());
        assertFalse(Format.DCM.supportsTransparency());
        assertTrue(Format.GIF.supportsTransparency());
        assertTrue(Format.JP2.supportsTransparency());
        assertFalse(Format.JPG.supportsTransparency());
        assertFalse(Format.MP4.supportsTransparency());
        assertFalse(Format.MPG.supportsTransparency());
        assertFalse(Format.PDF.supportsTransparency());
        assertTrue(Format.PNG.supportsTransparency());
        assertTrue(Format.SGI.supportsTransparency());
        assertTrue(Format.SID.supportsTransparency());
        assertTrue(Format.TIF.supportsTransparency());
        assertFalse(Format.WEBM.supportsTransparency());
        assertTrue(Format.WEBP.supportsTransparency());
    }

    @Test
    public void testToMap() {
        Map<String,Object> map = Format.JPG.toMap();
        assertEquals("jpg", map.get("extension"));
        assertEquals("image/jpeg", map.get("media_type"));
    }

    @Test
    public void testToString() {
        for (Format format : Format.values()) {
            assertEquals(format.getPreferredExtension(),
                    format.toString());
        }
    }

}
