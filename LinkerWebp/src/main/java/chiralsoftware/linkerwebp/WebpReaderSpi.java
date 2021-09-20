package chiralsoftware.linkerwebp;

import chiralsoftware.linkerwebp.impl.WebpImageReader;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Logger;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;

/**
 *
 */
public final class WebpReaderSpi extends ImageReaderSpi {

    private static final Logger LOG = Logger.getLogger(WebpReaderSpi.class.getName());
    
    public WebpReaderSpi() throws IOException {
        super();
    }
    
    private static final byte[] webpFirstHeader = { 'R', 'I', 'F', 'F' };
    private static final byte[] webpSecondHeader = { 'W', 'E', 'B', 'P' };

    @Override
    public boolean canDecodeInput(Object source) throws IOException {
//        LOG.info("can i decode this? " + source);
        if(source instanceof byte[] ba) {
            // the smallest possible webp file is 26 bytes:
            // https://github.com/mathiasbynens/small/blob/master/webp.webp
            // although ImageMagick says that particular file is corrupt
            if(ba.length < 26) return false;
            for(int i = 0; i < webpFirstHeader.length; i++)
                if(webpFirstHeader[i] != ba[i]) return false;
            for(int i = 0; i < webpSecondHeader.length; i++ ) {
                if(webpSecondHeader[i] != ba[i + 4 + webpFirstHeader.length])
                    return false;
            }
            // TODO: also check file length
            return true;
        }
        return false;
    }

    @Override
    public ImageReader createReaderInstance(Object extension) throws IOException {
        LOG.fine("Looking for a reader instance for this extension: " + extension);
        
        return new WebpImageReader(this);
    }
    
    @Override
    public Class[] getInputTypes() {
        return new Class[] { byte[].class };
    }

    @Override
    public String getDescription(Locale locale) {
        return "ImageIO module for reading and writing WebP images using Google's libwebp";
    }
    
}
