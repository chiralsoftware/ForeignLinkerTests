package chiralsoftware.linkerwebp.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Optional;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import jdk.incubator.foreign.CLinker;
import static jdk.incubator.foreign.CLinker.C_INT;
import static jdk.incubator.foreign.CLinker.C_LONG;
import static jdk.incubator.foreign.CLinker.C_POINTER;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.GroupLayout;
import jdk.incubator.foreign.LibraryLookup;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;

/**
 * Interface to LibWebp
 */
public final class LibWebp {

    private static final Logger LOG = Logger.getLogger(LibWebp.class.getName());
    
    private static final String libraryPath = "/usr/lib/x86_64-linux-gnu/libwebp.so";

    private final LibraryLookup libraryLookup; 
    
    private static final LibWebp libWebp;
    static {
        try {
            libWebp = new LibWebp();
        } catch(IOException ioe) {
            LOG.log(WARNING, "couldn't load libwebp", ioe);
            throw new RuntimeException(ioe);
        }
    }
    
    static LibWebp getInstance() {
        return libWebp;
    }
    
    /** Match the struct WebPConfig definition */
    static final GroupLayout Config =
            MemoryLayout.ofStruct(
                    // Lossless encoding (0=lossy(default), 1=lossless).
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("lossless"),
                    // between 0 and 100. For lossy, 0 gives the smallest
                          // size and 100 the largest. For lossless, this
                          // parameter is the amount of effort put into the
                          // compression: 0 is the fastest but gives larger
                          // files compared to the slowest, but best, 100.
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("quality"),
                    // Hint for image type (lossless only for now).
                    // this is represented as an int i think?
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("image_hint"),
                    // Parameters related to lossy compression only:
                    // if non-zero, set the desired target size in bytes.
                          // Takes precedence over the 'compression' parameter.
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("target_size"),

                    // if non-zero, specifies the minimal distortion to
                    // try to achieve. Takes precedence over target_size.
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("target_PSNR"),

                    // maximum number of segments to use, in [1..4]
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("segments"),

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
    
    static final GroupLayout Picture = MemoryLayout.ofStruct(
            // To select between ARGB and YUVA input.
            MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("use_argb"),
            
            // colorspace: should be YUVA420 or YUV420 for now (=Y'CbCr).
            MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("colorspace"),
            
            // width
            MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("width"),
            // height
            MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("height"),
            
            
            MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName(""),
            MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName(""),
            MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName(""),
            MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName(""),
            MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName(""),
            MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName(""),
            MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName(""),
    
    ).withBitAlignment(64);
    
    public static enum ImageHint {
        DEFAULT, // default preset
        PICTURE,  // digital picture, like portrait, inner shot
        PHOTO, // outdoor photograph, with natural lighting
        GRAPH, // Discrete tone image (graph, map-tile etc).
        LAST
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
    }
    
    private MethodHandle loadMethodHandle(CLinker cLinker, LibraryLookup libraryLookup,
            String name, MethodType methodType, FunctionDescriptor functionDescriptor) throws IOException {
        final Optional<LibraryLookup.Symbol> optionalSymbol = libraryLookup.lookup(name);
        if(optionalSymbol.isEmpty()) throw new IOException("couldn't library lookup for symbol: " + name);
        final MethodHandle mh = cLinker.downcallHandle(optionalSymbol.get(), methodType, functionDescriptor);
        LOG.info("loaded this method handle: "+ mh + " for symbol: " + name);
        return mh;
    }
    
    public final MethodHandle GetInfo;
    
    
    /** 
uint8_t* WebPDecodeARGBInto(const uint8_t* data, size_t data_size,
                            uint8_t* output_buffer, int output_buffer_size, int output_stride);

*/
    public final MethodHandle DecodeARGBInto;
    
    /** 
size_t WebPEncodeLosslessRGB(const uint8_t* rgb, int width, int height, int stride, uint8_t** output);
     
     */
    public final MethodHandle EncodeLosslessRGB;
    
    /** 
 void WebPFree(void* ptr);
     */
    public final MethodHandle Free;
    
}
