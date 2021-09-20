package chiralsoftware.linkerwebp;

import chiralsoftware.linkerwebp.impl.LibWebp.ImageHint;
import static java.lang.System.lineSeparator;
import java.lang.invoke.VarHandle;
import static jdk.incubator.foreign.CLinker.C_INT;
import jdk.incubator.foreign.GroupLayout;
import static jdk.incubator.foreign.MemoryLayout.PathElement.groupElement;
import static jdk.incubator.foreign.MemoryLayout.paddingLayout;
import static jdk.incubator.foreign.MemoryLayout.structLayout;
import jdk.incubator.foreign.MemorySegment;

/**
 * Java representation of the Config struct. This class is a wrapper for the struct's 
 * MemorySegment. The getters and setters in this
 * class act on the wrapped MemorySegment.
 */
public final class Config {
    
    public Config(MemorySegment segment) {
        if(segment == null) throw new NullPointerException("segment can't be null");
        if(segment.byteSize() != Config.byteSize())
            throw new IllegalArgumentException("The memory segment size was: " + segment.byteSize() + 
                    ", but it should be: " + Config.byteSize());
        this.segment = segment;
    }
    
    /**
     * Match the struct WebPConfig definition
     */
    public static final GroupLayout Config = structLayout(
                    // Lossless encoding (0=lossy(default), 1=lossless).
                    C_INT.withName("lossless"),
                    // between 0 and 100. For lossy, 0 gives the smallest
                    // size and 100 the largest. For lossless, this
                    // parameter is the amount of effort put into the
                    // compression: 0 is the fastest but gives larger
                    // files compared to the slowest, but best, 100.
                    C_INT.withName("quality"),
                    // quality/speed trade-off (0=fast, 6=slower-better)
                    C_INT.withName("method"),
                    // Hint for image type (lossless only for now).
                    // this is represented as an int i think?
                    C_INT.withName("image_hint"),
                    // Parameters related to lossy compression only:
                    // if non-zero, set the desired target size in bytes.
                    // Takes precedence over the 'compression' parameter.
                    C_INT.withName("target_size"),
                    // if non-zero, specifies the minimal distortion to
                    // try to achieve. Takes precedence over target_size.
                    C_INT.withName("target_PSNR"),
                    // maximum number of segments to use, in [1..4]
                    C_INT.withName("segments"),
                    // Spatial Noise Shaping. 0=off, 100=maximum.
                    C_INT.withName("sns_strength"),
                    // range: [0 = off .. 100 = strongest]
                    C_INT.withName("filter_strength"),
                    // range: [0 = off .. 7 = least sharp]
                    C_INT.withName("filter_sharpness"),
                    // filtering type: 0 = simple, 1 = strong (only used
                    // if filter_strength > 0 or autofilter > 0)
                    C_INT.withName("filter_type"),
                    // Auto adjust filter's strength [0 = off, 1 = on]
                    C_INT.withName("autofilter"),
                    // Algorithm for encoding the alpha plane (0 = none,
                    // 1 = compressed with WebP lossless). Default is 1
                    C_INT.withName("alpha_compression"),
                    // Predictive filtering method for alpha plane.
                    //  0: none, 1: fast, 2: best. Default if 1.
                    C_INT.withName("alpha_filtering"),
                    // Between 0 (smallest size) and 100 (lossless).
                    // Default is 100.
                    C_INT.withName("alpha_quality"),
                    // number of entropy-analysis passes (in [1..10]).
                    C_INT.withName("pass"),
                    // if true, export the compressed picture back.
                    // In-loop filtering is not applied.
                    C_INT.withName("show_compressed"),
                    // preprocessing filter (0=none, 1=segment-smooth)
                    C_INT.withName("preprocessing"),
                    // log2(number of token partitions) in [0..3]
                    // Default is set to 0 for easier progressive decoding.
                    C_INT.withName("partitions"),
                    // quality degradation allowed to fit the 512k limit on
                    // prediction modes coding (0: no degradation,
                    C_INT.withName("partition_limit"),
                    C_INT.withName("emulate_jpeg_size"),
                    C_INT.withName("thread_level"),
                    C_INT.withName("low_memory"),
                    C_INT.withName("near_lossless"),
                    C_INT.withName("exact"),
                    C_INT.withName("use_delta_palette"),
                    C_INT.withName("use_sharp_yuv"),
                    paddingLayout(64) // padding for later use
            ).withBitAlignment(64);

    private final MemorySegment segment;
    
    /** Lossless encoding (0=lossy(default), 1=lossless). */
    public int getLossless() {
        return (Integer) Config.varHandle(int.class, groupElement("lossless")).get(segment);
    }
    
    /** between 0 and 100. For lossy, 0 gives the smallest
                          size and 100 the largest. For lossless, this
                          parameter is the amount of effort put into the
                          compression: 0 is the fastest but gives larger
                          files compared to the slowest, but best, 100.
 */
    public float getQuality() {
        return (Float) Config.varHandle(float.class, groupElement("quality")).get(segment);
    }
    
    /** quality/speed trade-off (0=fast, 6=slower-better) */
    public int getMethod() {
        return (Integer) Config.varHandle(int.class, groupElement("lossless")).get(segment);
    }
    
    /** Hint for image type (lossless only for now).  */
    public ImageHint getImageHint() {
        final int hintValue =
                (Integer) Config.varHandle(int.class, groupElement("image_hint")).get(segment);
        if(hintValue < 0 || hintValue >= ImageHint.values().length)
            throw new IllegalStateException("The image hint value: " + hintValue + " was out of range, which should be 0 to " +
                    (ImageHint.values().length - 1) + " inclusive");
        return ImageHint.values()[hintValue];
    }
    
    /** if non-zero, set the desired target size in bytes.
     Takes precedence over the 'compression' parameter. */
    public int getTargetSize() {
        return (Integer) Config.varHandle(int.class, groupElement("target_size")).get(segment);
    }
     
    /**  if non-zero, specifies the minimal distortion to try to achieve. Takes precedence over target_size. */
    public float getTargetPSNR() {
        return (Float) Config.varHandle(float.class, groupElement("target_PSNR")).get(segment);
    }
    
    /** maximum number of segments to use, in [1..4]  */
    public int getSegments() {
        return (Integer) Config.varHandle(int.class, groupElement("segments")).get(segment);
    }
    
    /**  Spatial Noise Shaping. 0=off, 100=maximum. */
    public int getSnsStrength() {
        return (Integer) Config.varHandle(int.class, groupElement("sns_strength")).get(segment);
    }

    /** range: [0 = off .. 100 = strongest] */
    public int getFilterStrength() {
        return (Integer) Config.varHandle(int.class, groupElement("filter_strength")).get(segment);
    }

    /** range: [0 = off .. 7 = least sharp] */
    public int getFilterSharpness() {
        return (Integer) Config.varHandle(int.class, groupElement("filter_sharpness")).get(segment);
    }
    
    /** filtering type: 0 = simple, 1 = strong (only used  if filter_strength > 0 or autofilter > 0) */
    public int getFilterType() {
        return (Integer) Config.varHandle(int.class, groupElement("filter_type")).get(segment);
    }
    
    /** Auto adjust filter's strength [0 = off, 1 = on] */
    public int getAutofilter() {
        return (Integer) Config.varHandle(int.class, groupElement("autofilter")).get(segment);
    }

    /** Algorithm for encoding the alpha plane (0 = none,
    1 = compressed with WebP lossless). Default is 1.
    */
    public int getAlphaCompression() {
        return (Integer) Config.varHandle(int.class, groupElement("alpha_compression")).get(segment);
    }

    /** Predictive filtering method for alpha plane.
     0: none, 1: fast, 2: best. Default if 1. */
    public int getAlphaFiltering() {
        return (Integer) Config.varHandle(int.class, groupElement("alpha_filtering")).get(segment);
    }

    /** Between 0 (smallest size) and 100 (lossless). Default is 100. */
    public int getAlphaQuality() {
        return (Integer) Config.varHandle(int.class, groupElement("alpha_quality")).get(segment);
    }

    /** number of entropy-analysis passes (in [1..10]). */
    public int getPass() {
        return (Integer) Config.varHandle(int.class, groupElement("pass")).get(segment);
    }

    /** if true, export the compressed picture back. In-loop filtering is not applied. */
    public int getShowCompressed() {
        return (Integer) Config.varHandle(int.class, groupElement("show_compressed")).get(segment);
    }

    /** preprocessing filter: 0=none, 1=segment-smooth, 2=pseudo-random dithering  */
    public int getPreprocessing() {
        return (Integer) Config.varHandle(int.class, groupElement("preprocessing")).get(segment);
    }

    /** log2(number of token partitions) in [0..3]. Default
     * is set to 0 for easier progressive decoding. */
    public int getPartitions() {
        return (Integer) Config.varHandle(int.class, groupElement("partitions")).get(segment);
    }

    /** quality degradation allowed to fit the 512k limit
     * on prediction modes coding (0: no degradation,
       100: maximum possible degradation).  */
    public int getPartitionLimit() {
        return (Integer) Config.varHandle(int.class, groupElement("partition_limit")).get(segment);
    }

    /** If true, compression parameters will be remapped
     * to better match the expected output size from
     * JPEG compression. Generally, the output size will
     * be similar but the degradation will be lower. */
    public int getEmulateJpegSize() {
        return (Integer) Config.varHandle(int.class, groupElement("emulate_jpeg_size")).get(segment);
    }

    /**  If non-zero, try and use multi-threaded encoding. */
    public int getThreadLevel() {
        return (Integer) Config.varHandle(int.class, groupElement("thread_level")).get(segment);
    }

    /** If set, reduce memory usage (but increase CPU use). */
    public int getLowMemory() {
        return (Integer) Config.varHandle(int.class, groupElement("low_memory")).get(segment);
    }

    /** Near lossless encoding [0 = max loss .. 100 = off (default)]. */
    public int getNearLossless() {
        return (Integer) Config.varHandle(int.class, groupElement("near_lossless")).get(segment);
    }

    /** if non-zero, preserve the exact RGB values under
     * transparent area. Otherwise, discard this invisible
     * RGB information for better compression. The default
     * value is 0. */
    public int getExact() {
        return (Integer) Config.varHandle(int.class, groupElement("exact")).get(segment);
    }

    /** reserved for future lossless feature */
    public int getUseDeltaPalette() {
        return (Integer) Config.varHandle(int.class, groupElement("use_delta_palette")).get(segment);
    }

    /** if needed, use sharp (and slow) RGB->YUV conversion */
    public int getUseSharpYuv() {
        return (Integer) Config.varHandle(int.class, groupElement("use_sharp_yuv")).get(segment);
    }

    @Override
    public String toString() {
        return "Config{" + "segment=" + segment +
                "lossless=" + getLossless() + ", " +
                "quality=" + getQuality() + ", " + 
                "method=" + getMethod() + ", " + 
                "imageHint=" + getImageHint() + ", " + 
                "targetSize=" + getTargetSize() + ", " +
                "targetPSNR=" + getTargetPSNR() + ", " + 
                "segments=" + getSegments() + ", " + 
                "snsStrength=" + getSnsStrength() + ", " +
                "filterStrength=" + getFilterStrength() + ", " + 
                "filterSharpness=" + getFilterSharpness() + ", " + 
                "filterType=" + getFilterType() + ", " + 
                "autofilter=" + getAutofilter() + ", " + 
                "alphaCompression=" + getAlphaCompression() + ", " +
                "alphaFiltering=" + getAlphaFiltering() + ", " + 
                "alphaQuality=" + getAlphaQuality() + ", " +
                "pass=" + getPass() + ", " +
                "showCompressed=" + getShowCompressed() + ", " +
                "preprocessing=" + getPreprocessing() + ", " + 
                "partitions=" + getPartitions() + ", " + 
                "partitionLimit=" + getPartitionLimit() + ", " + 
                "emulateJpegSize=" + getEmulateJpegSize() + ", " + 
                "threadLevel=" + getThreadLevel() + ", " + 
                "lowMemory=" + getLowMemory() + ", " + 
                "nearLossless=" + getNearLossless() + ", " +
                "exact=" + getExact() + ", " + 
                "useDeltaPalette=" + getUseDeltaPalette() + ", " + 
                "useSharpYuv=" + getUseSharpYuv() +
                '}';
    }

    /** Read config data from a memory segment and return it as a string
     @return  null if the segment is null */
    public static String showConfig(MemorySegment segment) {
        if(segment == null) return null;
        if(segment.byteSize() != Config.byteSize())
            return "segment byte size: " + segment.byteSize() + " does not equal Picture byte size: " + 
                    Config.byteSize();
        // now let's read in the values
        final VarHandle lossless = Config.varHandle(int.class, groupElement("lossless"));
        final VarHandle quality = Config.varHandle(float.class, groupElement("quality"));
        final int losslessInt = (Integer) lossless.get(segment);
        final float qualityFloat = (Float) quality.get(segment);
        return "lossless=" + losslessInt + lineSeparator() +
                "quality=" + qualityFloat + lineSeparator();
    }
    
    
}
