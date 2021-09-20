package chiralsoftware.linkerwebp.impl;

import chiralsoftware.linkerwebp.Config;
import chiralsoftware.linkerwebp.Picture;
import static chiralsoftware.linkerwebp.WebpUtils.colorSpaceType;
import chiralsoftware.linkerwebp.WebpWriterSpi;
import static java.awt.color.ColorSpace.TYPE_RGB;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import static java.lang.invoke.MethodHandles.insertArguments;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import jdk.incubator.foreign.CLinker;
import static jdk.incubator.foreign.CLinker.C_INT;
import static jdk.incubator.foreign.CLinker.C_POINTER;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import static jdk.incubator.foreign.MemorySegment.allocateNative;
import static jdk.incubator.foreign.ResourceScope.newImplicitScope;

/**
 * Write a BufferedImage to a webp format
 */
public final class WebpImageWriter extends ImageWriter {

    private static final Logger LOG = Logger.getLogger(WebpImageWriter.class.getName());
    
    private final LibWebp libWebp;
    
    public WebpImageWriter(WebpWriterSpi webpWriterSpi) {
        super(webpWriterSpi);
        libWebp = LibWebp.getInstance();
    }

    @Override
    public IIOMetadata getDefaultStreamMetadata(ImageWriteParam param) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType, ImageWriteParam param) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public IIOMetadata convertStreamMetadata(IIOMetadata inData, ImageWriteParam param) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public IIOMetadata convertImageMetadata(IIOMetadata inData, ImageTypeSpecifier imageType, ImageWriteParam param) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /** We are ignoring the ImageWriterParam for now */    
    public void write(IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param) throws IOException {
        final RenderedImage renderedImage = image.getRenderedImage();
        LOG.info("the sample model is: " + renderedImage.getSampleModel() + 
                ", which is class: " + renderedImage.getSampleModel().getClass());
        if(! (renderedImage.getSampleModel() instanceof ComponentSampleModel)) {
            throw new IIOException("sample model was of type: " + renderedImage.getSampleModel().getClass() + 
                    ", but this writer can only support type: " + ComponentSampleModel.class);
        }
        final ComponentSampleModel sampleModel = (ComponentSampleModel) renderedImage.getSampleModel();
        if(sampleModel.getNumBands() > 4 || sampleModel.getNumBands() < 3) 
            throw new IIOException("sampleModel.getNumBands() must be 3 or 4; it was: " +
                    sampleModel.getNumBands());
        LOG.finest("Band offsets: " + Arrays.toString(sampleModel.getBandOffsets()));

        final ColorModel colorModel = renderedImage.getColorModel();
        final boolean hasAlpha = colorModel.hasAlpha();
        if(hasAlpha && sampleModel.getNumBands() != 4) 
            throw new IIOException("the colorModel has alpha, but the number of bands is: " +
                    sampleModel.getNumBands() + ". it should be 4");
        if(! (colorModel instanceof ComponentColorModel)) 
            throw new IIOException("This writer expects a ComponentColorModel");
        
        LOG.finest("The colormodel is: " + colorModel + " which is class: " + 
                colorModel.getClass() + " and color space type: " + 
                colorSpaceType(colorModel.getColorSpace().getType()));
        if(renderedImage.getColorModel().getColorSpace().getType() != TYPE_RGB) {
            final int intType = renderedImage.getColorModel().getColorSpace().getType();
            throw new IIOException("The color type was: " + intType + " (" + colorSpaceType(intType) + 
                    "), but this writer only processes " + colorSpaceType(TYPE_RGB));
        }
        
        final Raster raster = renderedImage.getData();
        final DataBuffer dataBuffer = raster.getDataBuffer();
        final DataBufferByte dataBufferByte = (DataBufferByte) dataBuffer;
        LOG.finer("it has this many banks: " + dataBufferByte.getNumBanks());
        if(dataBufferByte.getNumBanks() != 1) 
            throw new IIOException("the dataBuffer contained: " + dataBufferByte.getNumBanks() + 
                    " banks, but this writer expects 1 bank");
        
        final byte[] bytes = dataBufferByte.getData();
        // let's copy the bytes into a native segment
        LOG.warning("we're copying byte arrays - fix this so we don't need to do that");
        MemorySegment copied = MemorySegment.allocateNative(bytes.length, newImplicitScope());
        copied.asByteBuffer().put(bytes);
        final MemorySegment configSegment = 
                allocateNative(Config.Config, newImplicitScope());
        try {
            int result = (Integer) libWebp.ConfigInit.invoke(configSegment.address());
            if(result != 1) 
                throw new IIOException("couldn't initialize the config segment: " + result);
            final Config myConfig = new Config(configSegment);
            LOG.fine("here is the config string: " + myConfig);
            final MemorySegment pictureSegment =
                    // implicit scope will be handled by the GC
                    allocateNative(Picture.Picture, newImplicitScope());
            result = (Integer) libWebp.PictureInit.invoke(pictureSegment.address());
            if(result != 1) 
                throw new IIOException("couldn't initialize Picture object: " +result);
            
            final Picture picture = new Picture(pictureSegment);
            libWebp.PictureInit.invoke(pictureSegment.address());
            picture.setUseArgb(renderedImage.getColorModel().hasAlpha() ? 1 : 0);
            picture.setWidth(renderedImage.getWidth());
            picture.setHeight(renderedImage.getHeight());
            result = (Integer) libWebp.PictureAlloc.invoke(pictureSegment.address());
            if(result != 1)
                throw new IIOException("picture alloc failed!");
            // which way we import data depends:
            // does it have alpha or not
            // three or four bands
            // RGB or BGR
            final MethodHandle importer =
                    switch(ImportType.findType(sampleModel.getBandOffsets(), hasAlpha)) {
                        case ABGR -> libWebp.PictureImportBGRA;
                        case BGR -> libWebp.PictureImportBGR;
                        case BGRX -> libWebp.PictureImportBGRX;
                        case RGB -> libWebp.PictureImportRGB;
                        case RGBA -> libWebp.PictureImportRGBA;
                        case RGBX -> libWebp.PictureImportRGBX;
                        default -> null;
                    };
            if(importer == null)
                throw new IIOException("couldn't find an importer for band offsets: " +  
                        Arrays.toString(sampleModel.getBandOffsets()) + " and alpha: "+ hasAlpha);
            result = (Integer) importer.invoke(pictureSegment.address(), copied.address(), 
                    renderedImage.getWidth() * sampleModel.getNumBands());
            LOG.info("ok we just did an invoke, result is: " + result);
            // now we should do an upcall !!!
            final MethodHandle writerMH =
                    MethodHandles.lookup().findStatic(WebpImageWriter.class, "myWriter", 
                            MethodType.methodType(int.class, 
                                    WritableByteChannel.class, MemoryAddress.class, int.class, MemoryAddress.class));
            // let's bind a parameter to this handle!
            final File testFile = new File("/tmp/test-out.webp");
            testFile.delete();
            final OutputStream os = new FileOutputStream(testFile);
            final WritableByteChannel channel = Channels.newChannel(os);
            final MethodHandle writerBound = insertArguments(writerMH, 0, channel);
            final MemoryAddress writerFunctionAddress =
                    CLinker.getInstance().upcallStub(writerBound, 
                            FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_POINTER), newImplicitScope());
            picture.setWriter(writerFunctionAddress.toRawLongValue());
            LOG.info("I set the writer, now time for encoding fun!");
            result = (Integer) libWebp.Encode.invoke(configSegment.address(), pictureSegment.address());
            LOG.info("Ok, what just happened? " + result);
        } catch(Throwable t) {
            throw new IOException("Oh no!", t);
        }
    }
    
    /** This can be static because we can bind any object necessary to the outputChannel
     parameter */
    public static int myWriter(WritableByteChannel channel, MemoryAddress data, int dataSize, MemoryAddress picturePointer) {
        final MemorySegment dataSegment = data.asSegment(dataSize, newImplicitScope());
        final ByteBuffer byteBuffer = dataSegment.asByteBuffer();
        try {
            channel.write(byteBuffer);
        } catch(IOException ioe) {
            LOG.log(WARNING,"caught: ", ioe);
            return 0;
        }
        return 1; // write is always successful so far
    }
    
    @Override
    public void setOutput(Object object) {
        super.setOutput(output);
        LOG.info("Need to output to this object: " + object);
        
    }
    
}
