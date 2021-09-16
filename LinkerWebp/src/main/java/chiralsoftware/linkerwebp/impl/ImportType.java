package chiralsoftware.linkerwebp.impl;

import java.util.Arrays;

/**
 *
 */
public enum ImportType {
    
    RGB(new int[] { 0,1,2 }, false), 
    RGBX(new int[] { 0,1,2,3 }, false), 
    RGBA(new int[] { 0,1,2,3 }, true),
    BGR(new int[] { 2,1,0 }, false), 
    BGRX(new int[] { 3,2,1,0 }, false), 
    ABGR(new int[] { 3,2,1,0 }, false);

    private ImportType(int[] bandOffsets, boolean alpha) {
        this.bandOffsets = bandOffsets;
        this.alpha = alpha;
    }
    
    private final int[] bandOffsets;
    private final boolean alpha;
    
    private boolean offsetMatch(int[] offsets) {
        return Arrays.equals(offsets, bandOffsets);
    }
    
    /** Return which type of image format is used, or return null if none match */
    public static ImportType findType(int[] offsets, boolean alpha) {
        for(ImportType it : ImportType.values()) {
            if(it.alpha == alpha && it.offsetMatch(offsets)) return it;
        }
        return null;
    }
}
