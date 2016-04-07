package org.checkerframework.framework.qual;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.CONSTRUCTOR;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that this class has been annotated for the given type system.
 * For example, <code>@AnnotatedFor({"nullness", "regex"})</code> indicates
 * that the class has been annotated with annotations such as
 * <code>@Nullable</code> and <code>@Regex</code>.  Has no effect unless the
 * <code>-AuseDefaultsForUncheckedCode=source</code> command-line argument
 * is supplied.
 * <p>
 *
 * Ordinarily, the <code>-AuseDefaultsForUncheckedCode=source</code> command-line argument
 * causes unannotated locations to be defaulted using unchecked code defaults,
 * and it suppresses all warnings. However, the
 * <code>-AuseDefaultsForUncheckedCode=source</code> command-line argument has no effect on
 * classes with a relevant <code>@AnnotatedFor</code> annotation:  any
 * unannotated location is defaulted normally (typically using the
 * CLIMB-to-top rule), and typechecking warnings are issued.
 * <p>
 *
 * <code>@AnnotatedFor</code>'s arguments are any string that may be passed
 * to the <code>-processor</code> command-line argument:  the fully-qualified
 * class name for the checker, or a shorthand for built-in checkers.  Using
 * the annotation with no arguments, as in
 * <code>@AnnotatedFor({})</code>, has no effect.
 *
 * @checker_framework.manual #compiling-libraries Compiling partially-annotated libraries
 */
@Target({TYPE, METHOD, CONSTRUCTOR, PACKAGE})
@Retention(RetentionPolicy.SOURCE)
public @interface AnnotatedFor {
    /**
     * @return the type systems for which the class has been annotated.
     * Legal arguments are any string that may be passed to the
     * <code>-processor</code> command-line argument:  the fully-qualified
     * class name for the checker, or a shorthand for built-in checkers.
     * @checker_framework.manual #shorthand-for-checkers Short names for built-in checkers
     */
    String[] value();
}
