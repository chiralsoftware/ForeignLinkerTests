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
    
}
