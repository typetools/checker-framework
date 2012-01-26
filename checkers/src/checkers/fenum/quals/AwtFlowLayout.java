package checkers.fenum.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Line alignments in a flow layout (see
 * {@link java.awt.FlowLayout} for more details).
 * @see java.awt.FlowLayout#LEFT
 * @see java.awt.FlowLayout#CENTER
 * @see java.awt.FlowLayout#RIGHT
 * @see java.awt.FlowLayout#LEADING
 * @see java.awt.FlowLayout#TRAILING
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf(FenumTop.class)
public @interface AwtFlowLayout {}
