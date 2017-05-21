package org.checkerframework.checker.fenum.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Basic alpha compositing rules for combining source and destination colors to achieve blending and
 * transparency effects with graphics and images (see {@link java.awt.AlphaComposite} for more
 * details).
 *
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
 * @checker_framework.manual #fenum-checker Fake Enum Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(FenumTop.class)
public @interface AwtAlphaCompositingRule {}
