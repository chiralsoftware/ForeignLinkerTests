package chiralsoftware.linkerwebp;

import java.nio.ByteOrder;
import static java.nio.ByteOrder.nativeOrder;
import java.util.logging.Logger;
import static jdk.incubator.foreign.ValueLayout.OfInt.OfInt;
import jdk.incubator.foreign.GroupLayout;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import static jdk.incubator.foreign.MemoryLayout.PathElement.groupElement;
import static jdk.incubator.foreign.MemoryLayout.paddingLayout;
import jdk.incubator.foreign.MemorySegment;

/**
 * Represent the WebpPicture struct.
 * <h1>Hints on debugging struct alignment</h1>
 * Just copying the elements from the C struct will not produce correct alignment.
 * First, check the structure size:
 * <pre>    printf("size of Picture is: %lu\n", sizeof(struct WebPPicture));</pre>
 * If:
 * <pre>Picture.byteSize()</pre>
 * does not equal that number, the GroupLayout is not right and it will crash.
 * To figure this out, look at the size of PathElements using:
 * <pre>final StringBuilder sb = new StringBuilder();
        for(MemoryLayout m : Picture.memberLayouts()) {
            sb.append(m.name().orElse("(none)")).append(", size: ").
                    append(m.byteSize()).append(lineSeparator());
        }</pre>
        * And then compare this to the C struct by downloading and compiling libweb and then using:
 * 
 * <pre>pahole ./src/enc/.libs/libwebpencode_la-picture_enc.o<pre>
 * This will quickly reveal where the problem is.
 */
public final class Picture {

    private static final Logger LOG = Logger.getLogger(Picture.class.getName());
    
    public static final GroupLayout Picture = MemoryLayout.structLayout(
            // To select between ARGB and YUVA input.
            OfInt..withName("use_argb"),
            // Used if use_argb = 0
            // colorspace: should be YUVA420 or YUV420 for now (=Y'CbCr).
            C_INT.withName("colorspace"),
            // width
            C_INT.withName("width"),
            // height
            C_INT.withName("height"),
            // pointers to uint8_t (unsigned byte) luma / chroma planes
            C_POINTER.withName("y"),
            C_POINTER.withName("u"),
            C_POINTER.withName("v"),
            // luma/chroma strides.
            C_INT.withName("y_stride"),
            C_INT.withName("uv_stride"),
            // pointer to the alpha plane uint8_t 
            C_POINTER.withName("a"),
            C_INT.withName("a_stride"),
            paddingLayout(2 * 32), // padding for later use
            paddingLayout(4 * 8), // this showed up when using pahole
            // Alternate ARGB input, recommended for lossless compression.
            //
            // Used if use_argb = 1.
            // Pointer to argb (32 bit) plane, uint32_t* argb
            C_POINTER.withName("argb"),
            // This is stride in pixels units, not bytes.
            C_INT.withName("argb_stride"),
            paddingLayout(3 * 32), // padding for later use
            
            // OUTPUT
            // Byte-emission hook, to store compressed bytes as they are ready.
            C_POINTER.withName("writer"), // can be null
            C_POINTER.withName("custom_ptr"), // *void
            C_INT.withName("extra_info_type"), 
            paddingLayout(4*8), // from pahole
            C_POINTER.withName("extra_info"), // pointer to extra info 
            C_POINTER.withName("stats"), // WebPAuxStats* stats
            // Error code for the latest error encountered during encoding
            C_INT.withName("error_code"),
            paddingLayout(4*8), // from pahole
            C_POINTER.withName("progress_hook"), // WebPProgressHook
            C_POINTER.withName("user_data"), // void* user_data
            paddingLayout(32 * 3), // padding for later use
            paddingLayout(4*8), // from pahole
            paddingLayout(8*8), // *pad4
            paddingLayout(8*8), // *pad5
            paddingLayout(32 * 8), // pad6
            // PRIVATE FIELDS
            C_POINTER.withName("memoyr_"), // row chunk of memory for yuv
            C_POINTER.withName("memory_argb_"), // and for argb
            paddingLayout(8*8 * 2) // padding for later use
    ).withBitAlignment(64);
    
    static {
//        LOG.info("The size of the picture memory arrangment is: " + Picture.byteSize() + ", which should be 256");
//        if(Picture.byteSize() != 256)
//            LOG.warning("OH NO! WRONG PICTURE SIZE!");
//        LOG.info("The writer offset is: " + Picture.byteOffset(groupElement("writer")));
//        LOG.info(" attributes: " + Picture.attributes().collect(joining(", ")));
//        final StringBuilder sb = new StringBuilder();
//        for(MemoryLayout m : Picture.memberLayouts()) {
//            sb.append(m.name().orElse("(none)")).append(", size: ").
//                    append(m.byteSize()).append(lineSeparator());
//        }
//        out.println("The layout: ");
//        out.println(sb);
    }

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
    
    /** ARGB input (mostly used for input to lossless compression). Note that
     this is a pointer to an array of int*/
    public void setArgb(MemoryAddress m) {
        Picture.varHandle(MemoryAddress.class, groupElement("argb")).set(segment, m);
    }
    
    /** This is stride in pixels units, not bytes. */
    public void setArgbStride(int i) {
        Picture.varHandle(int.class, groupElement("argb_stride")).set(segment, i);
    }
    
    // OUTPUT
    
    /** Byte-emission hook, to store compressed bytes as they are ready.
     It has type WebPWriterFunction */
    public void setWriter(long a) {
        Picture.varHandle(long.class, groupElement("writer")).set(segment, a);
    }
    
    /** Can be used by the writer */
    public void setCustomPtr(int m) {
        Picture.varHandle(MemoryAddress.class, groupElement("custom_ptr")).set(segment, m);
    }
    
    /** map for extra information (only for lossy compression mode)
     * <ol>
     * <li>intra type
     * <li>segment
     * <li>quant
     * <li>intra-16 prediction mode,
     * <li>chroma prediction mode,
     * <li>bit cost,
     * <li>distortion
     * </ol>
 */
    public void setExtraInfoType(int i) {
        Picture.varHandle(int.class, groupElement("extra_info_type")).set(segment, i);
    }
    
    /** if not NULL, points to an array of size
     * ((width + 15) / 16) * ((height + 15) / 16) that
     * will be filled with a macroblock map, depending
     * on extra_info_type. */
    public void setExtraInfo(MemoryAddress m) {
        Picture.varHandle(MemoryAddress.class, groupElement("extra_info")).set(segment, m);
    }
    
    /// STATS AND REPORTS
    
    /** Pointer to side statistics (updated only if not NULL).
     Type is WebPAuxStats* */
    public void setStats(MemoryAddress m) {
        Picture.varHandle(MemoryAddress.class, groupElement("stats")).set(segment, m);
    }
    
    /** Error code for the latest error encountered during encoding,
     type is WebPEncodingError */
    public void setErrorCode(int i) {
        Picture.varHandle(int.class, groupElement("error_code")).set(segment, i);
    }
    
    /** If not NULL, report progress during encoding..
     Type is WebPProgressHook */
    public void setProgressHook(MemoryAddress m) {
        Picture.varHandle(MemoryAddress.class, groupElement("progress_hook")).set(segment, m);
    }
    
    /** this field is free to be set to any value and
     * used during callbacks (like progress-report e.g.).  */
    public void setUserData(MemoryAddress m) {
        Picture.varHandle(MemoryAddress.class, groupElement("user_data")).set(segment, m);
    }
    
    // other fields are private or unused
}
