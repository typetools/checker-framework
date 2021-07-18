package org.checkerframework.checker.fenum.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Color space tags to identify the specific color space of a Color object or, via a ColorModel
 * object, of an Image, a BufferedImage, or a GraphicsDevice (see {@link java.awt.color.ColorSpace}
 * for more details).
 *
 * @see java.awt.color.ColorSpace#TYPE_XYZ
 * @see java.awt.color.ColorSpace#TYPE_Lab
 * @see java.awt.color.ColorSpace#TYPE_Luv
 * @see java.awt.color.ColorSpace#TYPE_YCbCr
 * @see java.awt.color.ColorSpace#TYPE_Yxy
 * @see java.awt.color.ColorSpace#TYPE_RGB
 * @see java.awt.color.ColorSpace#TYPE_GRAY
 * @see java.awt.color.ColorSpace#TYPE_HSV
 * @see java.awt.color.ColorSpace#TYPE_HLS
 * @see java.awt.color.ColorSpace#TYPE_CMYK
 * @see java.awt.color.ColorSpace#TYPE_CMY
 * @see java.awt.color.ColorSpace#TYPE_2CLR
 * @see java.awt.color.ColorSpace#TYPE_3CLR
 * @see java.awt.color.ColorSpace#TYPE_4CLR
 * @see java.awt.color.ColorSpace#TYPE_5CLR
 * @see java.awt.color.ColorSpace#TYPE_6CLR
 * @see java.awt.color.ColorSpace#TYPE_7CLR
 * @see java.awt.color.ColorSpace#TYPE_8CLR
 * @see java.awt.color.ColorSpace#TYPE_9CLR
 * @see java.awt.color.ColorSpace#TYPE_ACLR
 * @see java.awt.color.ColorSpace#TYPE_BCLR
 * @see java.awt.color.ColorSpace#TYPE_CCLR
 * @see java.awt.color.ColorSpace#TYPE_DCLR
 * @see java.awt.color.ColorSpace#TYPE_ECLR
 * @see java.awt.color.ColorSpace#TYPE_FCLR
 * @see java.awt.color.ColorSpace#CS_sRGB
 * @see java.awt.color.ColorSpace#CS_LINEAR_RGB
 * @see java.awt.color.ColorSpace#CS_CIEXYZ
 * @see java.awt.color.ColorSpace#CS_PYCC
 * @see java.awt.color.ColorSpace#CS_GRAY
 * @checker_framework.manual #fenum-checker Fake Enum Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(FenumTop.class)
public @interface AwtColorSpace {}
