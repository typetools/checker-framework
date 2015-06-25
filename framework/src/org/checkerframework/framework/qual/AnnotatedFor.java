package org.checkerframework.framework.qual;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Target;

/**
 * Indicates that this class has been annotated for the given type system.
 * For example, <code>@AnnotatedFor({"nullness", "regex"})</code> indicates
 * that the class has been annotated with annotations such as
 * <tt>@Nullable</tt> and <tt>@Regex</tt>.
 * <p>
 *
 * Ordinarily, the <tt>-AuseConservativeDefaultsForUnannotatedCode</tt> command-line argument
 * causes unannotated locations to be defaulted using conservative library
 * annotations, and it suppresses all warnings.  The
 * <tt>-AuseConservativeDefaultsForUnannotatedCode</tt> command-line argument has no effect on
 * classes with a relevant <tt>@AnnotatedFor</tt> annotion:  any
 * unannotated location is defaulted normally (typically using the
 * CLIMB-to-top rule), and typechecking warnings are issued.
 * <p>
 *
 * <code>@AnnotatedFor</code>'s arguments are any string that may be passed
 * to the <tt>-processor</tt> command-line argument:  the fully-qualified
 * class name for the checker, or a shorthand for built-in checkers.  Using
 * the annotation with no arguments, as in
 * <code>@AnnotatedFor({})</code>, has no effect.
 * 
 * @checker_framework.manual #compiling-libraries Compiling partially-annotated librares
 */
@Target({TYPE, METHOD}) // permitting on PACKAGE would be too error-prone
public @interface AnnotatedFor {
    /**
     * @return the type systems for which the class has been annotated.
     * Legal arguments are any string that may be passed to the
     * <tt>-processor</tt> command-line argument:  the fully-qualified
     * class name for the checker, or a shorthand for built-in checkers.
     * @checker_framework.manual #shorthand-for-checkers Short names for built-in checkers
     */
    String[] value();
}
