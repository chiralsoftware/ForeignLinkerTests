package chiralsoftware.linkerwebp;

import chiralsoftware.linkerwebp.impl.WebpImageReader;
import chiralsoftware.linkerwebp.impl.WebpImageWriter;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

/**
 * Implement a writer SPI
 */
public final class WebpWriterSpi extends ImageWriterSpi {
    
    public WebpWriterSpi() {
        super("Chiral Software, Inc", // vendorName
                "0.0.1", // version
                new String[] { "webp" }, // names
                new String[] { "webp" }, // suffixes
                new String[] { "image/webp" }, // MIMETypes
                WebpImageWriter.class.getName(), // writerClassName
                new Class[] {  ImageOutputStream.class, byte[].class }, // outputTypes
                new String[] { WebpImageReader.class.getName() }, // readerSpiNames
                false, // supportsStandardStreamMetadataFormat
                null, // nativeStreamMetadataFormatName
                null, // nativeStreamMetadataFormatClassName
                null, // extraStreamMetadataFormatNames,
                null, // extraStreamMetadataFormatClassNames
                false, // supportStandardImageMetadataFormat
                null, // nativeImageMeatadataFormatName
                null, // nativeImageMetadataFormatClassName
                null, // extraImageMetadataFormatNames,
                null // extraIamgeMetadataFormatClassNames
                );
    }

    @Override
    public boolean canEncodeImage(ImageTypeSpecifier type) {
        final int bands = type.getNumBands();
        return bands == 3 || bands == 4; // we do rgb or rgba
    }

    @Override
    public ImageWriter createWriterInstance(Object extension) throws IOException {
        return new WebpImageWriter(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "Chiral Software Webp Image Writer";
    }
    
}
