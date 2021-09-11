package chiralsoftware.linkerwebp.impl;

import chiralsoftware.linkerwebp.Config;
import chiralsoftware.linkerwebp.Picture;
import chiralsoftware.linkerwebp.WebpUtils;
import chiralsoftware.linkerwebp.WebpWriterSpi;
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
    
    public void write(IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param) throws IOException {
        final RenderedImage renderedImage = image.getRenderedImage();
        LOG.info("the sample model is: " + renderedImage.getSampleModel() + 
                ", which is class: " + renderedImage.getSampleModel().getClass());
        if(! (renderedImage.getSampleModel() instanceof ComponentSampleModel)) {
            throw new IIOException("sample model was of type: " + renderedImage.getSampleModel().getClass() + 
                    ", but this writer can only support type: " + ComponentSampleModel.class);
        }
        final ComponentSampleModel sampleModel = (ComponentSampleModel) renderedImage.getSampleModel();
        LOG.info("Band offsets: " + Arrays.toString(sampleModel.getBandOffsets()));
        LOG.info("The colormodel is: " + renderedImage.getColorModel() + " which is class: " + 
                renderedImage.getColorModel() + " and color space type: " + 
                WebpUtils.colorSpaceType(renderedImage.getColorModel().getColorSpace().getType()));
        final Raster raster = renderedImage.getData();
        final DataBuffer dataBuffer = raster.getDataBuffer();
        final DataBufferByte dataBufferByte = (DataBufferByte) dataBuffer;
        LOG.info("it has this many banks: " + dataBufferByte.getNumBanks());
        final byte[] bytes = dataBufferByte.getData();
        LOG.info("it has this many bytes: "+ bytes.length);
        // let's copy the bytes into a native segment
        MemorySegment copied = MemorySegment.allocateNative(bytes.length);
        copied.asByteBuffer().put(bytes);
        final MemorySegment configSegment = 
                allocateNative(Config.Config);
        try {
            int result = (Integer) libWebp.ConfigInit.invoke(configSegment.address());
            LOG.info("cool i just called config. result=" + result);
            final Config myConfig = new Config(configSegment);
            LOG.info("here is the config string: " + myConfig);
            MemorySegment pictureSegment =
                    allocateNative(Picture.Picture);
            result = (Integer) libWebp.PictureInit.invoke(pictureSegment.address());
            LOG.info("Ok i init the picture, result is: "+ result);
            final Picture picture = new Picture(pictureSegment);
            libWebp.PictureInit.invoke(pictureSegment.address());
            LOG.info("now set the relevant fields in the picture");
            picture.setUseArgb(0);
            picture.setWidth(renderedImage.getWidth());
            picture.setHeight(renderedImage.getHeight());
            result = (Integer) libWebp.PictureAlloc.invoke(pictureSegment.address());
            LOG.info("the result is: " + result);
            // now i can import the RGB data
            // PictureImportRGBA(WebPPicture* picture, const uint8_t* rgba, int rgba_stride); 
            result = (Integer) libWebp.PictureImportRGB.invoke(pictureSegment.address(), copied.address(), 
                    renderedImage.getWidth() * 3);
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
            final MemorySegment writerFunctionSegment =
                    CLinker.getInstance().upcallStub(writerBound, 
                            FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_POINTER));
            picture.setWriter(writerFunctionSegment.address().toRawLongValue());
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
        final MemorySegment dataSegment = data.asSegmentRestricted(dataSize);
        final ByteBuffer byteBuffer = dataSegment.asByteBuffer();
        try {
            channel.write(byteBuffer);
            dataSegment.close();
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
