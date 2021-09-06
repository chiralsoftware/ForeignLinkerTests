package chiralsoftware.linkerwebp;

import chiralsoftware.linkerwebp.impl.LibWebp;
import static java.lang.System.lineSeparator;
import java.lang.invoke.VarHandle;
import java.util.logging.Logger;
import static jdk.incubator.foreign.MemoryLayout.PathElement.groupElement;
import jdk.incubator.foreign.MemorySegment;

/**
 * Some convenient utilities for WebP
 */
public final class WebpUtils {

    private static final Logger LOG = Logger.getLogger(WebpUtils.class.getName());
    
    private WebpUtils() {
        throw new RuntimeException("don't instantiate this");
    }
    
    /** Read config data from a memory segment and return it as a string
     @return  null if the segment is null */
    public static String showConfig(MemorySegment segment) {
        if(segment == null) return null;
        if(! segment.isAlive()) return "segment not alive";
        if(segment.byteSize() != LibWebp.Config.byteSize())
            return "segment byte size: " + segment.byteSize() + " does not equal Picture byte size: " + 
                    LibWebp.Config.byteSize();
        // now let's read in the values
        final VarHandle lossless = LibWebp.Config.varHandle(int.class, groupElement("lossless"));
        final VarHandle quality = LibWebp.Config.varHandle(float.class, groupElement("quality"));
        final int losslessInt = (Integer) lossless.get(segment);
        final float qualityFloat = (Float) quality.get(segment);
        return "lossless=" + losslessInt + lineSeparator() +
                "quality=" + qualityFloat + lineSeparator();
    }
    
}
