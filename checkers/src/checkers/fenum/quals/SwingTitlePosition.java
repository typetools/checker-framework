package checkers.fenum.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Justifications for the title text of @see java.swing.border.TitledBorder.
 * @see java.swing.border.TitledBorder#DEFAULT_POSITION
 * @see java.swing.border.TitledBorder#ABOVE_TOP
 * @see java.swing.border.TitledBorder#TOP
 * @see java.swing.border.TitledBorder#BELOW_TOP
 * @see java.swing.border.TitledBorder#ABOVE_BOTTOM
 * @see java.swing.border.TitledBorder#BOTTOM
 * @see java.swing.border.TitledBorder#BELOW_BOTTOM
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
// @Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf( { FenumTop.class } )
public @interface SwingTitlePosition {}
