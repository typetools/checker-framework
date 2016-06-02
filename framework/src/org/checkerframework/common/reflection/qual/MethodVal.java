package org.checkerframework.common.reflection.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;

/**
 * This represents a set of {@link java.lang.reflect.Method Method} or
 * {@link java.lang.reflect.Constructor Constructor} values.  If an
 * expression's type has <code>@MethodVal</code>, then the expression's
 * run-time value is one of those values.
 * <p>
 *
 * Each of <code>@MethodVal</code>'s argument lists must be of equal length,
 * and { className[i], methodName[i], params[i] } represents one of the
 * <code>Method</code> or <code>Constructor</code> values in the set.
 *
 * @checker_framework.manual #methodval-and-classval-checkers MethodVal Checker
 */
@SubtypeOf({ UnknownMethod.class })
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface MethodVal {
    /** The <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1">binary name</a>
     * of the class that declares this method. */
    String[] className();

    /** The name of the method that this Method object represents.
     * Use <code>&lt;init&gt;</code> for constructors. */
    String[] methodName();

    /** The number of parameters to the method. */
    int[] params();
}
