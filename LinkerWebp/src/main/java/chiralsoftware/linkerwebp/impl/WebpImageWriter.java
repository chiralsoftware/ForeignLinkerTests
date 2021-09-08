package chiralsoftware.linkerwebp.impl;

import chiralsoftware.linkerwebp.Config;
import chiralsoftware.linkerwebp.Picture;
import chiralsoftware.linkerwebp.WebpWriterSpi;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;
import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import jdk.incubator.foreign.CLinker;
import static jdk.incubator.foreign.CLinker.C_INT;
import static jdk.incubator.foreign.CLinker.C_POINTER;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAccess;
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
    
    public void writeAdvanced(RenderedImage renderedImage) throws IOException {
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
            result = (Integer) libWebp.PictureImportRGBA.invoke(pictureSegment.address(), copied.address(), 
                    renderedImage.getWidth() * 3);
            LOG.info("ok we just did an invoke, result is: " + result);
            // now we should do an upcall !!!
            final MethodHandle writerMH =
                    MethodHandles.lookup().findStatic(WebpImageWriter.class, "myWriter", 
                            MethodType.methodType(int.class, MemoryAddress.class, int.class, MemoryAddress.class));
            LOG.info("I have the writeMH");
            final MemorySegment writerFunctionSegment =
                    CLinker.getInstance().upcallStub(writerMH, 
                            FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_POINTER));
            picture.setWriter(writerFunctionSegment.address().toRawLongValue());
            LOG.info("I set the writer, now time for encoding fun!");
            result = (Integer) libWebp.Encode.invoke(configSegment.address(), pictureSegment.address());
            LOG.info("Ok, what just happened? " + result);
        } catch(Throwable t) {
            throw new IOException("Oh no!", t);
        }
    }
    
    /** It's static for now so I can try it out */
    public static int myWriter(MemoryAddress data, int dataSize, MemoryAddress picturePointer) {
        LOG.info("I i need to write: " + dataSize + " bytes!");
        return 1; // write is always successful so far
    }

    @Override
    public void write(IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param) throws IOException {
        LOG.info("Ok let's write this image!");
        // regular JPEGs read in by ImageIO don't have rasters
        
        final RenderedImage renderedImage = image.getRenderedImage();
        if(true) {
            writeAdvanced(renderedImage);
            return;
        }
        
//        if(! image.hasRaster()) throw new IOException("the input image has no raster.");
        
        final Raster raster = renderedImage.getData();
        
        LOG.info("I got a raster like this: " + raster.getClass().getName());
        final DataBuffer dataBuffer = raster.getDataBuffer();
        LOG.info("the data buffer is this: "+ dataBuffer.getClass());
        final DataBufferByte dataBufferByte = (DataBufferByte) dataBuffer;
        LOG.info("it has this many banks: " + dataBufferByte.getNumBanks());
        final byte[] bytes = dataBufferByte.getData();
        LOG.info("it has this many bytes: "+ bytes.length);
        // let's copy the bytes into a native segment
        MemorySegment copied = MemorySegment.allocateNative(bytes.length);
        copied.asByteBuffer().put(bytes);
        // there are three bytes per pixel in this, so write it out
        // i should use: 
        // size_t WebPEncodeLosslessRGB(const uint8_t* rgb, int width, int height, int stride, uint8_t** output);
        final MemorySegment outputPointer = MemorySegment.allocateNative(8); // 64 bit pointers are 8 bytes
        final MemorySegment inputBytes = MemorySegment.ofArray(bytes);
        long size;
        try {
            size = (Long) libWebp.EncodeLosslessRGB.invoke(copied.address(), 
                    renderedImage.getWidth(), renderedImage.getHeight(),
                renderedImage.getWidth() * 3, outputPointer.address());
            LOG.info("the alleged size is: " + size);
        } catch(Throwable t) {
            throw new IOException("Oh no!", t);
        }
        final long imagePointer = MemoryAccess.getLong(outputPointer);
        final MemoryAddress pointerAddress = MemoryAddress.ofLong(imagePointer);
        final MemorySegment imageSegment = pointerAddress.asSegmentRestricted(size);
        // we need to get a MemoryAddress for the rgb, and then pass all this to Encode
        LOG.info("done with getting the image pointer, now let's write to a file");
        final ByteBuffer outputBytes = imageSegment.asByteBuffer();
        final FileChannel fc = new FileOutputStream("/tmp/myimage.webp").getChannel();
        fc.write(outputBytes);
        fc.close();
        LOG.info("ok it is saved!!");
        LOG.info("try to do a WebpFree");
        try {
            libWebp.Free.invoke(pointerAddress);
        } catch(Throwable t) {
            throw new IOException("couldn't free", t);
        }
        LOG.info("it's free!");
  }
    
    @Override
    public void setOutput(Object object) {
        super.setOutput(output);
        LOG.info("Need to output to this object: " + object);
        
    }
    
}
