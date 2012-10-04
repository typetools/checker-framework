package checkers.fenum.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Justifications for the title text of a
 * {@link javax.swing.border.TitledBorder}.
 * @see javax.swing.border.TitledBorder#DEFAULT_POSITION
 * @see javax.swing.border.TitledBorder#ABOVE_TOP
 * @see javax.swing.border.TitledBorder#TOP
 * @see javax.swing.border.TitledBorder#BELOW_TOP
 * @see javax.swing.border.TitledBorder#ABOVE_BOTTOM
 * @see javax.swing.border.TitledBorder#BOTTOM
 * @see javax.swing.border.TitledBorder#BELOW_BOTTOM
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf(FenumTop.class)
public @interface SwingTitlePosition {}
