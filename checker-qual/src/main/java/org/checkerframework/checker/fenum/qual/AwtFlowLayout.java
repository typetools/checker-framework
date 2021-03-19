package org.checkerframework.checker.fenum.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Line alignments in a flow layout (see {@link java.awt.FlowLayout} for more details).
 *
 * @see java.awt.FlowLayout#LEFT
 * @see java.awt.FlowLayout#CENTER
 * @see java.awt.FlowLayout#RIGHT
 * @see java.awt.FlowLayout#LEADING
 * @see java.awt.FlowLayout#TRAILING
 * @checker_framework.manual #fenum-checker Fake Enum Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(FenumTop.class)
public @interface AwtFlowLayout {}
