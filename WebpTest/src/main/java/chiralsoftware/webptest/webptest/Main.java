package chiralsoftware.webptest.webptest;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.File;
import static java.lang.System.out;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;

/**
 * Test the ImageIO WebP plugin
 */
public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());
    
    public static void main(String[] args) throws Throwable {
        LOG.info("Testing this out!");
        
//        final WebpReaderSpi spi = new WebpReaderSpi();
//        
//        LOG.info("info: " + spi.getDescription(Locale.getDefault()));
        
        final byte[] bytes = Files.readAllBytes(Path.of("/tmp/test.webp"));

        final Iterator<ImageReader> readers = ImageIO.getImageReaders(bytes);
        
        final ImageReader myReader = readers.next();
        
        
        myReader.setInput(bytes,false);
        final BufferedImage result = myReader.read(0);
        LOG.info("I got this result: " + result);
        final ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromRenderedImage(result);
        final ColorSpace colorSpace = imageTypeSpecifier.getColorModel().getColorSpace();
        LOG.info("Color space # of components: " +  colorSpace.getNumComponents() + ", type=" + colorSpace.getType());
        LOG.info("The ImageTypeSpecifier is: " + imageTypeSpecifier);
        LOG.info("ImageTypeSpecifier num of bands= " + imageTypeSpecifier.getNumBands());
        LOG.info("ImageTypeSpecifier num of components= " + imageTypeSpecifier.getNumComponents());
        ImageIO.write(result, "PNG", new File("/tmp/output.png"));
        
        LOG.info("and now for the next thing: write a WEBP!");
        
        final BufferedImage inputImage = ImageIO.read(new File("/tmp/test.jpg"));
        LOG.info("read an image, " + inputImage.getWidth() + "x" + inputImage.getHeight());
        
        ImageIO.write(inputImage, "webp", new File("/tmp/output.webp"));
        
        LOG.info("did it work?");
    }
    
}
