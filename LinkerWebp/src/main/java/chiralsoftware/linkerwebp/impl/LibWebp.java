package chiralsoftware.linkerwebp.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.util.Optional;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import jdk.incubator.foreign.CLinker;
import static jdk.incubator.foreign.CLinker.C_INT;
import static jdk.incubator.foreign.CLinker.C_LONG;
import static jdk.incubator.foreign.CLinker.C_POINTER;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.LibraryLookup;
import jdk.incubator.foreign.MemoryAddress;

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
