package chiralsoftware.linkerwebp;

import static java.awt.color.ColorSpace.CS_CIEXYZ;
import static java.awt.color.ColorSpace.CS_GRAY;
import static java.awt.color.ColorSpace.CS_LINEAR_RGB;
import static java.awt.color.ColorSpace.CS_PYCC;
import static java.awt.color.ColorSpace.CS_sRGB;
import static java.awt.color.ColorSpace.TYPE_2CLR;
import static java.awt.color.ColorSpace.TYPE_3CLR;
import static java.awt.color.ColorSpace.TYPE_4CLR;
import static java.awt.color.ColorSpace.TYPE_5CLR;
import static java.awt.color.ColorSpace.TYPE_6CLR;
import static java.awt.color.ColorSpace.TYPE_7CLR;
import static java.awt.color.ColorSpace.TYPE_8CLR;
import static java.awt.color.ColorSpace.TYPE_9CLR;
import static java.awt.color.ColorSpace.TYPE_ACLR;
import static java.awt.color.ColorSpace.TYPE_BCLR;
import static java.awt.color.ColorSpace.TYPE_CCLR;
import static java.awt.color.ColorSpace.TYPE_DCLR;
import static java.awt.color.ColorSpace.TYPE_ECLR;
import static java.awt.color.ColorSpace.TYPE_FCLR;
import static java.awt.color.ColorSpace.TYPE_GRAY;
import static java.awt.color.ColorSpace.TYPE_HLS;
import static java.awt.color.ColorSpace.TYPE_HSV;
import static java.awt.color.ColorSpace.TYPE_Lab;
import static java.awt.color.ColorSpace.TYPE_Luv;
import static java.awt.color.ColorSpace.TYPE_RGB;
import static java.awt.color.ColorSpace.TYPE_XYZ;
import static java.awt.color.ColorSpace.TYPE_YCbCr;
import static java.awt.color.ColorSpace.TYPE_Yxy;
import java.util.logging.Logger;

/**
 * Some convenient utilities for WebP
 */
public final class WebpUtils {

    private static final Logger LOG = Logger.getLogger(WebpUtils.class.getName());
    
    private WebpUtils() {
        throw new RuntimeException("don't instantiate this");
    }
    
    public static String colorSpaceType(int i) {
        return switch(i) {
            case CS_CIEXYZ -> "CS_CIEXYZ";
            case CS_GRAY -> "CS_GRAY";
            case CS_LINEAR_RGB -> "CS_LINEAR_RGB";
            case CS_PYCC -> "CS_PYCC";
            case CS_sRGB -> "CS_sRGB";
            case TYPE_2CLR -> "TYPE_2CLR";
            case TYPE_3CLR -> "TYPE_3CLR";
            case TYPE_4CLR -> "TYPE_4CLR";
            case TYPE_5CLR -> "TYPE_5CLR";
            case TYPE_6CLR -> "TYPE_6CLR";
            case TYPE_7CLR -> "TYPE_7CLR";
            case TYPE_8CLR -> "TYPE_8CLR";
            case TYPE_9CLR -> "TYPE_9CLR";
            case TYPE_ACLR -> "TYPE_ACLR";
            case TYPE_BCLR -> "TYPE_BCLR";
            case TYPE_CCLR -> "TYPE_CCLR";
            case TYPE_DCLR -> "TYPE_DCLR";
            case TYPE_ECLR -> "TYPE_ECLR";
            case TYPE_FCLR -> "TYPE_FCLR";
            case TYPE_GRAY -> "TYPE_GRAY";
            case TYPE_HLS -> "TYPE_HLS";
            case TYPE_HSV -> "TYPE_HSV";
            case TYPE_Lab -> "TYPE_Lab";
            case TYPE_Luv -> "TYPE_Luv";
            case TYPE_RGB -> "TYPE_RGB";
            case TYPE_XYZ -> "TYPE_XYZ";
            case TYPE_YCbCr -> "TYPE_YCbCr";
            case TYPE_Yxy -> "TYPE_Yxy";
            default -> "invalid";
        };
    }
    
}
