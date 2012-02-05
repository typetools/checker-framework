package checkers.fenum.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Basic alpha compositing rules for combining source and
 * destination colors to achieve blending and transparency
 * effects with graphics and images (see
 * {@link java.awt.AlphaComposite} for more details).
 * @see java.awt.AlphaComposite#CLEAR
 * @see java.awt.AlphaComposite#SRC
 * @see java.awt.AlphaComposite#DST
 * @see java.awt.AlphaComposite#SRC_OVER
 * @see java.awt.AlphaComposite#DST_OVER
 * @see java.awt.AlphaComposite#SRC_IN
 * @see java.awt.AlphaComposite#DST_IN
 * @see java.awt.AlphaComposite#SRC_OUT
 * @see java.awt.AlphaComposite#DST_OUT
 * @see java.awt.AlphaComposite#SRC_ATOP
 * @see java.awt.AlphaComposite#DST_ATOP
 * @see java.awt.AlphaComposite#XOR
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf(FenumTop.class)
public @interface AwtAlphaCompositingRule {}
