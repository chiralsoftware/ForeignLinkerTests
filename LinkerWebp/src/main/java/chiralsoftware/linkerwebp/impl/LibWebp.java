package chiralsoftware.linkerwebp.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import static java.lang.invoke.MethodHandles.insertArguments;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.util.Optional;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import jdk.incubator.foreign.CLinker;
import static jdk.incubator.foreign.CLinker.C_FLOAT;
import static jdk.incubator.foreign.CLinker.C_INT;
import static jdk.incubator.foreign.CLinker.C_LONG;
import static jdk.incubator.foreign.CLinker.C_POINTER;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.SymbolLookup;
import jdk.incubator.foreign.MemoryAddress;

/**
 * Interface to LibWebp. Note: there will be some changes with JDK 17. See:
 * https://blog.arkey.fr/2021/09/04/a-practical-look-at-jep-412-in-jdk17-with-libsodium/
 * for a good reference on that.
 */
public final class LibWebp {

    private static final Logger LOG = Logger.getLogger(LibWebp.class.getName());

    private static final String libraryPath = "/usr/lib/x86_64-linux-gnu/libwebp.so";
    private static final String libraryName = "webp";

    private final SymbolLookup symbolLookup;

    private static final LibWebp libWebp;

    /**
     * Unfortunately this has to be hard-coded
     */
    public static final int WEBP_ENCODER_ABI_VERSION = 0x020e;

    static {
        try {
            System.loadLibrary(libraryName);
            libWebp = new LibWebp();
        } catch (IOException ioe) {
            LOG.log(WARNING, "couldn't load libwebp", ioe);
            throw new RuntimeException(ioe);
        }
    }

    static LibWebp getInstance() {
        return libWebp;
    }

    public static enum ImageHint {
        DEFAULT, // default preset
        PICTURE, // digital picture, like portrait, inner shot
        PHOTO, // outdoor photograph, with natural lighting
        GRAPH // Discrete tone image (graph, map-tile etc).
    }

    public static enum Preset {
        DEFAULT("default preset"),
        PICTURE("digital picture, like portrait, inner shot"),
        PHOTO("outdoor photograph, with natural lighting"),
        DRAWING("hand or line drawing, with high-contrast details"),
        ICON("small-sized colorful images"),
        TEXT("text-like");

        private Preset(String s) {
            this.description = s;
        }

        public String description() {
            return description;
        }
        private final String description;
    }

    public static enum EncodingError {
        VP8_ENC_OK(null),
        VP8_ENC_ERROR_OUT_OF_MEMORY("memory error allocating objects"),
        VP8_ENC_ERROR_BITSTREAM_OUT_OF_MEMORY("memory error while flushing bits"),
        VP8_ENC_ERROR_NULL_PARAMETER("a pointer parameter is NULL"),
        VP8_ENC_ERROR_INVALID_CONFIGURATION("configuration is invalid"),
        VP8_ENC_ERROR_BAD_DIMENSION("picture has invalid width/height"),
        VP8_ENC_ERROR_PARTITION0_OVERFLOW("partition is bigger than 512k"),
        VP8_ENC_ERROR_PARTITION_OVERFLOW("partition is bigger than 16M"),
        VP8_ENC_ERROR_BAD_WRITE("error while flushing bytes"),
        VP8_ENC_ERROR_FILE_TOO_BIG("file is bigger than 4G"),
        VP8_ENC_ERROR_USER_ABORT("abort request by user");

        private EncodingError(String s) {
            message = s;
        }
        private final String message;

        /**
         * The associated message. This can be null in the case of VP8_ENC_OK
         */
        public String message() {
            return message;
        }
    }

    private LibWebp() throws IOException {

        LOG.info("Initializing the linker!");
        final Path path = Path.of(libraryPath);

        symbolLookup = SymbolLookup.loaderLookup();

        final CLinker cLinker = CLinker.getInstance();

        // int WebPGetInfo(const uint8_t* data, size_t data_size, int* width, int* height);
        GetInfo = loadMethodHandle(cLinker, symbolLookup, "WebPGetInfo",
                MethodType.methodType(int.class, MemoryAddress.class, long.class, MemoryAddress.class, MemoryAddress.class),
                FunctionDescriptor.of(CLinker.C_INT, C_POINTER, C_LONG, C_POINTER, C_POINTER));

        LOG.info("Cool, here is GetInfo: " + GetInfo);

        DecodeARGBInto = loadMethodHandle(cLinker, symbolLookup, "WebPDecodeARGBInto",
                MethodType.methodType(MemoryAddress.class, // returns - pointer to output buffer
                        MemoryAddress.class, long.class, // input data and size 
                        MemoryAddress.class, long.class, // output buffer and size
                        int.class // output_stride
                ),
                FunctionDescriptor.of(C_POINTER,
                        C_POINTER, C_LONG,
                        C_POINTER, C_LONG,
                        C_INT)
        );

        // size_t WebPEncodeLosslessRGB(const uint8_t* rgb, int width, int height, int stride, uint8_t** output);
        EncodeLosslessRGB = loadMethodHandle(cLinker, symbolLookup, "WebPEncodeLosslessRGB",
                MethodType.methodType(long.class, // returns - size_t
                        MemoryAddress.class, // buffer with source bytes
                        int.class, int.class, // width and height
                        int.class, // stride
                        MemoryAddress.class // the location of the output buffer
                ),
                FunctionDescriptor.of(C_LONG, C_POINTER, C_INT, C_INT, C_INT, C_POINTER)
        );
        Free = loadMethodHandle(cLinker, symbolLookup, "WebPFree",
                MethodType.methodType(void.class, // returns void
                        MemoryAddress.class // pointer to be freed
                ),
                FunctionDescriptor.ofVoid(C_POINTER));

        // annoyingly, the WebPConfigPreset function we would like to access
        // is defined as an inline
        // int WebPConfigInitInternal(WebPConfig*, WebPPreset, float, int);
        ConfigInitInternal = loadMethodHandle(cLinker, symbolLookup, "WebPConfigInitInternal",
                MethodType.methodType(int.class, // returns false in case of error 
                        MemoryAddress.class, // WebPConfig *
                        int.class, // WebPPreset preset - the enum
                        float.class, // quality
                        int.class // WEBP_ENCODER_ABI_VERSION
                ),
                FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_FLOAT, C_INT));

        PictureInitInternal = loadMethodHandle(cLinker, symbolLookup, "WebPPictureInitInternal",
                MethodType.methodType(int.class, MemoryAddress.class, // Picture 
                        int.class // WEBP_ENCODER_ABI_VERSION
                ), FunctionDescriptor.of(C_INT, C_POINTER, C_INT));

        PictureInit = insertArguments(PictureInitInternal, 1, WEBP_ENCODER_ABI_VERSION);

        ConfigPreset = insertArguments(ConfigInitInternal, 3, WEBP_ENCODER_ABI_VERSION);

        // this is kinda miraculous if this works
        ConfigInit = insertArguments(ConfigInitInternal, 1,
                Preset.DEFAULT.ordinal(), 75f, WEBP_ENCODER_ABI_VERSION);
        
//        int WebPPictureAlloc(WebPPicture* picture)
        PictureAlloc = loadMethodHandle(cLinker, symbolLookup, "WebPPictureAlloc",
                MethodType.methodType(int.class, MemoryAddress.class),
                FunctionDescriptor.of(C_INT, C_POINTER));
        
        PictureImportRGB = loadMethodHandle(cLinker, symbolLookup, "WebPPictureImportRGB",
                MethodType.methodType(int.class, MemoryAddress.class, MemoryAddress.class, int.class),
                FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT));

        PictureImportRGBA = loadMethodHandle(cLinker, symbolLookup, "WebPPictureImportRGBA",
                MethodType.methodType(int.class, MemoryAddress.class, MemoryAddress.class, int.class),
                FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT));
        
        PictureImportRGBX = loadMethodHandle(cLinker, symbolLookup, "WebPPictureImportRGBX",
                MethodType.methodType(int.class, MemoryAddress.class, MemoryAddress.class, int.class),
                FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT));
        
        PictureImportBGR = loadMethodHandle(cLinker, symbolLookup, "WebPPictureImportBGR",
                MethodType.methodType(int.class, MemoryAddress.class, MemoryAddress.class, int.class),
                FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT));
        
        PictureImportBGRA = loadMethodHandle(cLinker, symbolLookup, "WebPPictureImportBGRA",
                MethodType.methodType(int.class, MemoryAddress.class, MemoryAddress.class, int.class),
                FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT));

        PictureImportBGRX = loadMethodHandle(cLinker, symbolLookup, "WebPPictureImportBGRX",
                MethodType.methodType(int.class, MemoryAddress.class, MemoryAddress.class, int.class),
                FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT));
        
//        int WebPEncode(const WebPConfig* config, WebPPicture* picture)
        Encode = loadMethodHandle(cLinker, symbolLookup, "WebPEncode", 
                MethodType.methodType(int.class, MemoryAddress.class, MemoryAddress.class),
                FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER));
        
    }

    private MethodHandle loadMethodHandle(CLinker cLinker, SymbolLookup symbolLookup,
            String name, MethodType methodType, FunctionDescriptor functionDescriptor) throws IOException {
        final Optional<MemoryAddress> oSymbolAddress = symbolLookup.lookup(name);
        if (oSymbolAddress.isEmpty())
            throw new IOException("couldn't library lookup for symbol: " + name);
        final MethodHandle mh = cLinker.downcallHandle(oSymbolAddress.get(), methodType, functionDescriptor);
        return mh;
    }

    public final MethodHandle GetInfo;

    /**
     * uint8_t* WebPDecodeARGBInto(const uint8_t* data, size_t data_size,
     * uint8_t* output_buffer, int output_buffer_size, int output_stride);
     *
     */
    public final MethodHandle DecodeARGBInto;

    /**
     * size_t WebPEncodeLosslessRGB(const uint8_t* rgb, int width, int height,
     * int stride, uint8_t** output);
     *
     */
    public final MethodHandle EncodeLosslessRGB;

    /**
     * void WebPFree(void* ptr);
     */
    public final MethodHandle Free;

    private final MethodHandle PictureInitInternal;

    /**
     * Calls PictureInitInternal but with the ABI version constant
     */
    public final MethodHandle PictureInit;

    public final MethodHandle ConfigInitInternal;

    /**
     * Configure based on one of the preset image types. This is the best way to
     * use WebP. I hope this function isn't inline This function will initialize
     * the configuration according to a predefined set of parameters (referred
     * to by 'preset') and a given quality factor. This function can be called
     * as a replacement to WebPConfigInit(). Will return false in case of error.
     * static WEBP_INLINE int WebPConfigPreset(WebPConfig* config, WebPPreset
     * preset, float quality)      *
     * FIXME - use MethodHandles.insertArguments to create a ConfigInit method
     * that has the constant already bound to it! This is a cool use
     */
    public final MethodHandle ConfigPreset;

    /**
     * Also an inline call t oConfigInitInternal, using the DEFAULT preset, and
     * 75 quality level
     */
    public final MethodHandle ConfigInit;
    
    /** Convenience allocation / deallocation based on picture->width/height:
     * Allocate y/u/v buffers as per colorspace/width/height specification.
     * Note! This function will free the previous buffer if needed.
     * Returns false in case of memory error.
     * 
     * int WebPPictureAlloc(WebPPicture* picture); */
    public final MethodHandle PictureAlloc;
    
    /** Colorspace conversion function to import RGB samples.
     * Previous buffer will be free'd, if any.
     *rgb buffer should have a size of at least height * rgb_stride.
     * Returns false in case of memory error. */
    public final MethodHandle PictureImportRGB;
    
    /** int WebPPictureImportRGBA(WebPPicture* picture, const uint8_t* rgba, int rgba_stride); */
    public final MethodHandle PictureImportRGBA;
    
    /**  Same, but for RGBA buffer. Imports the RGB direct from the 32-bit format
     * input buffer ignoring the alpha channel. Avoids needing to copy the data
     * to a temporary 24-bit RGB buffer to import the RGB only.
<pre>WEBP_EXTERN int WebPPictureImportRGBX(
    WebPPicture* picture, const uint8_t* rgbx, int rgbx_stride);</pre> */
    public final MethodHandle PictureImportRGBX;

    /** Variants of the above, but taking BGR(A|X) input. 
     * <pre>WEBP_EXTERN int WebPPictureImportBGR(
    WebPPicture* picture, const uint8_t* bgr, int bgr_stride);
WEBP_EXTERN int WebPPictureImportBGRA(
    WebPPicture* picture, const uint8_t* bgra, int bgra_stride);
WEBP_EXTERN int WebPPictureImportBGRX(
    WebPPicture* picture, const uint8_t* bgrx, int bgrx_stride);</pre>
     */
    public final MethodHandle PictureImportBGR;
    public final MethodHandle PictureImportBGRA;
    public final MethodHandle PictureImportBGRX;
    
    /** Main encoding call, after config and picture have been initialized.
     * 'picture' must be less than 16384x16384 in dimension (cf WEBP_MAX_DIMENSION),
     * and the 'config' object must be a valid one.
     * Returns false in case of error, true otherwise.
     * In case of error, picture->error_code is updated accordingly.
     * 'picture' can hold the source samples in both YUV(A) or ARGB input, depending
     * on the value of 'picture->use_argb'. It is highly recommended to use
     * the former for lossy encoding, and the latter for lossless encoding
     * (when config.lossless is true). Automatic conversion from one format to
     * another is provided but they both incur some loss.
<pre>WEBP_EXTERN int WebPEncode(const WebPConfig* config, WebPPicture* picture);<pre>
 */
    public final MethodHandle Encode;

}
