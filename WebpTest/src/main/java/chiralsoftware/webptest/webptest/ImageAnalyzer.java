package chiralsoftware.webptest.webptest;

import chiralsoftware.linkerwebp.WebpUtils;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.SampleModel;
import java.io.File;
import static java.lang.System.out;
import java.util.Arrays;
import javax.imageio.ImageIO;

/**
 * Convenience to print out a bunch of information about a RenderedImage file
 * so we can test to cover various image types
 * Run:
 * java -classpath ForeignLinkerTests/WebpTest/target/WebpTest-1.0-SNAPSHOT.jar:ForeignLinkerTests/LinkerWebp/target/LinkerWebp-1.0-SNAPSHOT.jar chiralsoftware.webptest.webptest.ImageAnalyzer
 * 
 */
public class ImageAnalyzer {
    
    public static void main(String args[]) throws Exception {
        final String fileName = args.length == 0 ? "/tmp/test.jpg" : args[0];
        final File file = new File(fileName);
        if(! file.canRead()) {
            out.println("couldn't find file: " + file);
            return;
        }
        final BufferedImage bim = ImageIO.read(file);
        out.println("Image: " + file + ", " + bim.getWidth() + "x" + bim.getHeight());
        final ColorModel colorModel = bim.getColorModel();
        out.println("Color model: " + colorModel);
        out.println(" which is an instance of: " + colorModel.getClass());
        if(colorModel instanceof ComponentColorModel) {
            final ComponentColorModel ccm = (ComponentColorModel) colorModel;
            out.println("It is a ComponentColorModel");
        }
        final ColorSpace colorSpace = colorModel.getColorSpace();
        out.println("Color space: " + colorSpace + " which is an instance of: " + colorSpace.getClass());
        out.println("   and type: "  +
                WebpUtils.colorSpaceType(colorSpace.getType()) + " and has: " + colorSpace.getNumComponents() + " components");
        
        final SampleModel sampleModel = bim.getSampleModel();
        out.println("Sample model: " + sampleModel + ", which is an instance of: "+ sampleModel.getClass());
        if(sampleModel instanceof ComponentSampleModel) {
            final ComponentSampleModel csm = (ComponentSampleModel) sampleModel;
            out.println("ComponentSampleModel: with " + csm.getNumBands() + " bands, ");
            out.println(" and band offsets: " + Arrays.toString(csm.getBandOffsets()));
        }
        out.println("");
    }
    
}
