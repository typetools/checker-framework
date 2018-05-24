package org.checkerframework.checker.signedness;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import org.checkerframework.checker.signedness.qual.Unsigned;

/**
 * Provides more static utility methods for unsigned values. These methods use Java packages not
 * included in Android. {@link SignednessUtil} has more methods.
 *
 * @checker_framework.manual #signedness-utilities Utility routines for manipulating unsigned values
 */
public class SignednessUtilExtra {
    private SignednessUtilExtra() {
        throw new Error("Do not instantiate");
    }

    /** Gets the unsigned width of a {@code Dimension}. */
    @SuppressWarnings("signedness")
    public static @Unsigned int dimensionUnsignedWidth(Dimension dim) {
        return dim.width;
    }

    /** Gets the unsigned height of a {@code Dimension}. */
    @SuppressWarnings("signedness")
    public static @Unsigned int dimensionUnsignedHeight(Dimension dim) {
        return dim.height;
    }

    /**
     * Sets rgb of BufferedImage b given unsigned ints. This method is a wrapper around {@link
     * java.awt.image.BufferedImage#setRGB(int, int, int, int, int[], int, int) setRGB(int, int,
     * int, int, int[], int, int)}, but assumes that the input should be interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static void setUnsignedRGB(
            BufferedImage b,
            int startX,
            int startY,
            int w,
            int h,
            @Unsigned int[] rgbArray,
            int offset,
            int scansize) {
        b.setRGB(startX, startY, w, h, rgbArray, offset, scansize);
    }

    /**
     * Gets rgb of BufferedImage b as unsigned ints. This method is a wrapper around {@link
     * java.awt.image.BufferedImage#getRGB(int, int, int, int, int[], int, int) getRGB(int, int,
     * int, int, int[], int, int)}, but assumes that the output should be interpreted as unsigned.
     */
    @SuppressWarnings("signedness")
    public static @Unsigned int[] getUnsignedRGB(
            BufferedImage b,
            int startX,
            int startY,
            int w,
            int h,
            @Unsigned int[] rgbArray,
            int offset,
            int scansize) {
        return b.getRGB(startX, startY, w, h, rgbArray, offset, scansize);
    }
}
