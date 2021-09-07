package chiralsoftware.linkerwebp.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import static java.lang.invoke.MethodHandles.insertArguments;
import java.lang.invoke.MethodType;
import java.nio.ByteOrder;
import static java.nio.ByteOrder.nativeOrder;
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
import jdk.incubator.foreign.GroupLayout;
import jdk.incubator.foreign.LibraryLookup;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import static jdk.incubator.foreign.MemoryLayout.ofPaddingBits;
import static jdk.incubator.foreign.MemoryLayout.ofValueBits;

/**
 * Interface to LibWebp.
 * Note: there will be some changes with JDK 17. See:
 * https://blog.arkey.fr/2021/09/04/a-practical-look-at-jep-412-in-jdk17-with-libsodium/
 * for a good reference on that.
 */
public final class LibWebp {

    private static final Logger LOG = Logger.getLogger(LibWebp.class.getName());

    private static final String libraryPath = "/usr/lib/x86_64-linux-gnu/libwebp.so";

    private final LibraryLookup libraryLookup;

    private static final LibWebp libWebp;
    
    /** Unfortunately this has to be hard-coded */
    public static final int WEBP_ENCODER_ABI_VERSION = 0x020e;

    static {
        try {
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
  public String description() { return description; }
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
        VP8_ENC_ERROR_USER_ABORT("abort request by user"),
        VP8_ENC_ERROR_LAST("list terminator. always last");  // should never be used        

        private EncodingError(String s) {
            message = s;
        }
        private final String message;
        /** The associated message. This can be null in the case of VP8_ENC_OK */
        public String message() { return message; }
    }

    private LibWebp() throws IOException {

        LOG.info("Initializing the linker!");
        final Path path = Path.of(libraryPath);

        libraryLookup = LibraryLookup.ofPath(path);

        final CLinker cLinker = CLinker.getInstance();

        // int WebPGetInfo(const uint8_t* data, size_t data_size, int* width, int* height);
        GetInfo = loadMethodHandle(cLinker, libraryLookup, "WebPGetInfo",
                MethodType.methodType(int.class, MemoryAddress.class, long.class, MemoryAddress.class, MemoryAddress.class),
                FunctionDescriptor.of(CLinker.C_INT, C_POINTER, C_LONG, C_POINTER, C_POINTER));

        LOG.info("Cool, here is GetInfo: " + GetInfo);

        DecodeARGBInto = loadMethodHandle(cLinker, libraryLookup, "WebPDecodeARGBInto",
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
        EncodeLosslessRGB = loadMethodHandle(cLinker, libraryLookup, "WebPEncodeLosslessRGB",
                MethodType.methodType(long.class, // returns - size_t
                        MemoryAddress.class, // buffer with source bytes
                        int.class, int.class, // width and height
                        int.class, // stride
                        MemoryAddress.class // the location of the output buffer
                ),
                FunctionDescriptor.of(C_LONG, C_POINTER, C_INT, C_INT, C_INT, C_POINTER)
        );
        Free = loadMethodHandle(cLinker, libraryLookup, "WebPFree",
                MethodType.methodType(void.class, // returns void
                        MemoryAddress.class // pointer to be freed
                ),
                FunctionDescriptor.ofVoid(C_POINTER));

        // annoyingly, the WebPConfigPreset function we would like to access
        // is defined as an inline
        // int WebPConfigInitInternal(WebPConfig*, WebPPreset, float, int);
        ConfigInitInternal = loadMethodHandle(cLinker, libraryLookup, "WebPConfigInitInternal",
                MethodType.methodType(int.class, // returns false in case of error 
                        MemoryAddress.class, // WebPConfig *
                        int.class, // WebPPreset preset - the enum
                        float.class, // quality
                        int.class  // WEBP_ENCODER_ABI_VERSION
                        ),
                FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_FLOAT, C_INT));
        
        PictureInitInternal = loadMethodHandle(cLinker, libraryLookup, "WebPPictureInitInternal", 
                MethodType.methodType(int.class, MemoryAddress.class, // Picture 
                        int.class // WEBP_ENCODER_ABI_VERSION
                ), FunctionDescriptor.of(C_INT, C_POINTER, C_INT));
        
        PictureInit = insertArguments(PictureInitInternal, 1, WEBP_ENCODER_ABI_VERSION);
        
        ConfigPreset = insertArguments(ConfigInitInternal, 3, WEBP_ENCODER_ABI_VERSION);

        // this is kinda miraculous if this works
        ConfigInit = insertArguments(ConfigInitInternal, 1, 
                Preset.DEFAULT.ordinal(), 75f, WEBP_ENCODER_ABI_VERSION);
    }

    private MethodHandle loadMethodHandle(CLinker cLinker, LibraryLookup libraryLookup,
            String name, MethodType methodType, FunctionDescriptor functionDescriptor) throws IOException {
        final Optional<LibraryLookup.Symbol> optionalSymbol = libraryLookup.lookup(name);
        if (optionalSymbol.isEmpty())
            throw new IOException("couldn't library lookup for symbol: " + name);
        final MethodHandle mh = cLinker.downcallHandle(optionalSymbol.get(), methodType, functionDescriptor);
        LOG.info("loaded this method handle: " + mh + " for symbol: " + name);
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
    
    /** Calls PictureInitInternal but with the ABI version constant */
    public final MethodHandle PictureInit;

    public final MethodHandle ConfigInitInternal;
    
    /** 
     * Configure based on one of the preset image types. This is the
     * best way to use WebP. I hope this function isn't inline
    This function will initialize the configuration according to a predefined
    set of parameters (referred to by 'preset') and a given quality factor.
    This function can be called as a replacement to WebPConfigInit(). Will
    return false in case of error.
    static WEBP_INLINE int WebPConfigPreset(WebPConfig* config,
                                        WebPPreset preset, float quality) 

    * FIXME - use MethodHandles.insertArguments to create a ConfigInit
    * method that has the constant already bound to it! This is a cool use
     */
    public final MethodHandle ConfigPreset;
    
    /** Also an inline call t oConfigInitInternal, using the DEFAULT preset, and 75 quality level */
    public final MethodHandle ConfigInit;

}
