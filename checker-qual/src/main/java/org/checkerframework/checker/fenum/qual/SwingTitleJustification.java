package org.checkerframework.checker.fenum.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Vertical orientations for the title text of a {@link javax.swing.border.TitledBorder}.
 *
 * @see javax.swing.border.TitledBorder#DEFAULT_JUSTIFICATION
 * @see javax.swing.border.TitledBorder#LEFT
 * @see javax.swing.border.TitledBorder#CENTER
 * @see javax.swing.border.TitledBorder#RIGHT
 * @see javax.swing.border.TitledBorder#LEADING
 * @see javax.swing.border.TitledBorder#TRAILING
 * @checker_framework.manual #fenum-checker Fake Enum Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(FenumTop.class)
public @interface SwingTitleJustification {}
