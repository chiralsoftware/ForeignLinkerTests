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

    /**
     * Match the struct WebPConfig definition
     */
    public static final GroupLayout Config
            = MemoryLayout.ofStruct(
                    // Lossless encoding (0=lossy(default), 1=lossless).
                    ofValueBits(32, nativeOrder()).withName("lossless"),
                    // between 0 and 100. For lossy, 0 gives the smallest
                    // size and 100 the largest. For lossless, this
                    // parameter is the amount of effort put into the
                    // compression: 0 is the fastest but gives larger
                    // files compared to the slowest, but best, 100.
                    ofValueBits(32, nativeOrder()).withName("quality"),
                    // Hint for image type (lossless only for now).
                    // this is represented as an int i think?
                    ofValueBits(32, nativeOrder()).withName("image_hint"),
                    // Parameters related to lossy compression only:
                    // if non-zero, set the desired target size in bytes.
                    // Takes precedence over the 'compression' parameter.
                    ofValueBits(32, nativeOrder()).withName("target_size"),
                    // if non-zero, specifies the minimal distortion to
                    // try to achieve. Takes precedence over target_size.
                    ofValueBits(32, nativeOrder()).withName("target_PSNR"),
                    // maximum number of segments to use, in [1..4]
                    ofValueBits(32, nativeOrder()).withName("segments"),
                    // Spatial Noise Shaping. 0=off, 100=maximum.
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("sns_strength"),
                    // range: [0 = off .. 100 = strongest]
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("filter_strength"),
                    // range: [0 = off .. 7 = least sharp]
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("filter_sharpness"),
                    // filtering type: 0 = simple, 1 = strong (only used
                    // if filter_strength > 0 or autofilter > 0)
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("filter_type"),
                    // Auto adjust filter's strength [0 = off, 1 = on]
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("autofilter"),
                    // Algorithm for encoding the alpha plane (0 = none,
                    // 1 = compressed with WebP lossless). Default is 1
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("alpha_compression"),
                    // Predictive filtering method for alpha plane.
                    //  0: none, 1: fast, 2: best. Default if 1.
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("alpha_filtering"),
                    // Between 0 (smallest size) and 100 (lossless).
                    // Default is 100.
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("alpha_quality"),
                    // number of entropy-analysis passes (in [1..10]).
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("pass"),
                    // if true, export the compressed picture back.
                    // In-loop filtering is not applied.
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("show_compressed"),
                    // preprocessing filter (0=none, 1=segment-smooth)
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("preprocessing"),
                    // log2(number of token partitions) in [0..3]
                    // Default is set to 0 for easier progressive decoding.
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("partitions"),
                    // quality degradation allowed to fit the 512k limit on
                    // prediction modes coding (0: no degradation,
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("partition_limit"),
                    // if needed, use sharp (and slow) RGB->YUV conversion
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("use_sharp_yuv")
            ).withBitAlignment(64);

    public static final GroupLayout Picture = MemoryLayout.ofStruct(
            // To select between ARGB and YUVA input.
            ofValueBits(32, ByteOrder.nativeOrder()).withName("use_argb"),
            // Used if use_argb = 0
            // colorspace: should be YUVA420 or YUV420 for now (=Y'CbCr).
            ofValueBits(32, ByteOrder.nativeOrder()).withName("colorspace"),
            // width
            ofValueBits(32, ByteOrder.nativeOrder()).withName("width"),
            // height
            ofValueBits(32, ByteOrder.nativeOrder()).withName("height"),
            // pointers to uint8_t (unsigned byte) luma / chroma planes
            ofValueBits(32, ByteOrder.nativeOrder()).withName("y"),
            ofValueBits(32, ByteOrder.nativeOrder()).withName("u"),
            ofValueBits(32, ByteOrder.nativeOrder()).withName("v"),
            // luma/chroma strides.
            ofValueBits(32, ByteOrder.nativeOrder()).withName("y_stride"),
            ofValueBits(32, ByteOrder.nativeOrder()).withName("uv_stride"),
            ofPaddingBits(3 * 32), // padding for later use
            // pointer to the alpha plane uint8_t 
            ofValueBits(32, ByteOrder.nativeOrder()).withName("a"),
            ofValueBits(32, ByteOrder.nativeOrder()).withName("a_stride"),
            // Alternate ARGB input, recommended for lossless compression.
            //
            // Used if use_argb = 1.
            // Pointer to argb (32 bit) plane, uint32_t* argb
            ofValueBits(32, ByteOrder.nativeOrder()).withName("argb"),
            // This is stride in pixels units, not bytes.
            ofValueBits(32, ByteOrder.nativeOrder()).withName("argb_stride"),
            // Byte-emission hook, to store compressed bytes as they are ready.
            ofValueBits(32, ByteOrder.nativeOrder()).withName("writer"), // can be null
            ofValueBits(32, ByteOrder.nativeOrder()).withName("custom_ptr"), // *void
            ofValueBits(32, ByteOrder.nativeOrder()).withName("extra_info_type"), 
            ofValueBits(32, ByteOrder.nativeOrder()).withName("extra_info"), // pointer to extra info 
            ofValueBits(32, nativeOrder()).withName("stats"), // WebPAuxStats* stats
            // Error code for the latest error encountered during encoding
            ofValueBits(32, ByteOrder.nativeOrder()).withName("error_code"),
            ofValueBits(32, nativeOrder()).withName("progress_hook"), // WebPProgressHook
            ofValueBits(32, nativeOrder()).withName("user_data"), // void* user_data
            ofPaddingBits(32 * 3), // padding for later use
            ofPaddingBits(32), // *pad4
            ofPaddingBits(32), // *pad5
            ofPaddingBits(32 * 8), // pad6
            // PRIVATE FIELDS
            ofValueBits(32, nativeOrder()).withName("memoyr_"), // row chunk of memory for yuv
            ofValueBits(32, nativeOrder()).withName("memory_argb_"), // and for argb
            ofValueBits(32 * 2, nativeOrder()) // padding for later use
    ).withBitAlignment(64);

    public static enum ImageHint {
        DEFAULT, // default preset
        PICTURE, // digital picture, like portrait, inner shot
        PHOTO, // outdoor photograph, with natural lighting
        GRAPH, // Discrete tone image (graph, map-tile etc).
        LAST
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
