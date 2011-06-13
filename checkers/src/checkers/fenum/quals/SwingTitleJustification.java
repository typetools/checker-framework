package checkers.fenum.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Vertical orientations for the title text of @see java.swing.border.TitledBorder.
 * @see java.swing.border.TitledBorder#DEFAULT_JUSTIFICATION
 * @see java.swing.border.TitledBorder#LEFT
 * @see java.swing.border.TitledBorder#CENTER
 * @see java.swing.border.TitledBorder#RIGHT
 * @see java.swing.border.TitledBorder#LEADING
 * @see java.swing.border.TitledBorder#TRAILING
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
// @Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf( { FenumTop.class } )
public @interface SwingTitleJustification {}
