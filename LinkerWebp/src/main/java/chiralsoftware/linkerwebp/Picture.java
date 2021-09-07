package chiralsoftware.linkerwebp;

import java.nio.ByteOrder;
import static java.nio.ByteOrder.nativeOrder;
import java.util.logging.Logger;
import jdk.incubator.foreign.GroupLayout;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import static jdk.incubator.foreign.MemoryLayout.PathElement.groupElement;
import static jdk.incubator.foreign.MemoryLayout.ofPaddingBits;
import static jdk.incubator.foreign.MemoryLayout.ofValueBits;
import jdk.incubator.foreign.MemorySegment;

/**
 * Represent the WebpPicture struct
 */
public final class Picture {

    private static final Logger LOG = Logger.getLogger(Picture.class.getName());
    
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

    public Picture(MemorySegment segment) {
        if(segment == null) throw new NullPointerException("segment can't be null");
        if(segment.byteSize() != Picture.byteSize()) 
            throw new IllegalArgumentException("segment size was: " + segment.byteSize() +
                    ", should have been: " + Picture.byteSize());
        this.segment = segment;
    }
    
    private final MemorySegment segment;
    
    /** Main flag for encoder selecting between ARGB or YUV input.
     * It is recommended to use ARGB input (*argb, argb_stride) for lossless
     * compression, and YUV input (*y, *u, *v, etc.) for lossy compression
     * since these are the respective native colorspace for these formats. */
    public void setUseArgb(int i) {
        Picture.varHandle(int.class, groupElement("use_argb")).set(segment, i);
    }

    /**  colorspace: should be YUV420 for now (=Y'CbCr). 
     FIXME: create an enum for WebPEncCSP*/
    public void setColorspace(int i) {
        Picture.varHandle(int.class, groupElement("colorspace")).set(segment, i);
    }
    
    /** */
    public void setWidth(int i) {
        Picture.varHandle(int.class, groupElement("width")).set(segment, i);
    }
    
    /** */
    public void setHeight(int i) {
        Picture.varHandle(int.class, groupElement("height")).set(segment, i);
    }
    
    /** Pointer to Y */
    public void setY(MemoryAddress m) {
        Picture.varHandle(MemoryAddress.class, groupElement("y")).set(segment, m);
    }
    /** Pointer to U */
    public void setU(MemoryAddress m) {
        Picture.varHandle(MemoryAddress.class, groupElement("u")).set(segment, m);
    }
    /** Pointer to V */
    public void setV(MemoryAddress m) {
        Picture.varHandle(MemoryAddress.class, groupElement("v")).set(segment, m);
    }
    
    /** Pointer to alpha plane  */
    public void setA(MemoryAddress m) {
        Picture.varHandle(MemoryAddress.class, groupElement("a")).set(segment, m);
    }

    /** stride of the alpha plane */
    public void setAStride(int i) {
        Picture.varHandle(int.class, groupElement("a_stride")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    /** */
    public void set(int i) {
        Picture.varHandle(int.class, groupElement("")).set(segment, i);
    }
    
    
}
