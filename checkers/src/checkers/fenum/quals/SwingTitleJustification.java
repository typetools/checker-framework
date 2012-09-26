package checkers.fenum.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Vertical orientations for the title text of a
 * {@link javax.swing.border.TitledBorder}.
 * @see javax.swing.border.TitledBorder#DEFAULT_JUSTIFICATION
 * @see javax.swing.border.TitledBorder#LEFT
 * @see javax.swing.border.TitledBorder#CENTER
 * @see javax.swing.border.TitledBorder#RIGHT
 * @see javax.swing.border.TitledBorder#LEADING
 * @see javax.swing.border.TitledBorder#TRAILING
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf(FenumTop.class)
public @interface SwingTitleJustification {}
