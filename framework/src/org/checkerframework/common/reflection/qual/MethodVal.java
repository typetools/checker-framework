package org.checkerframework.common.reflection.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * This represents a set of {@link java.lang.reflect.Method Method} or
 * {@link java.lang.reflect.Constructor Constructor} values.  If an
 * expression's type has <tt>@MethodVal</tt>, then the expression's
 * run-time value is one of those values.
 * <p>
 *
 * Each of <tt>@MethodVal</tt>'s argument lists must be of equal length,
 * and { className[i], methodName[i], params[i] } represents one of the
 * <tt>Method</tt> or <tt>Constructor</tt> values in the set.
 *
 * @checker_framework.manual #methodval-checker MethodVal Checker
 */
@TypeQualifier
@SubtypeOf({ UnknownMethod.class })
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE })
public @interface MethodVal {
    /** The <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1">binary name</a>
     * of the class that declares this method. */
    String[] className();

    /** The name of the method that this Method object represents.
     * Use <tt>&lt;init&gt;</tt> for constructors. */
    String[] methodName();

    /** The number of parameters to the method. */
    int[] params();
}

