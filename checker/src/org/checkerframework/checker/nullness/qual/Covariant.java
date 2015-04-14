package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.*;

// TODO: move to org.checkerframework.framework.qual package.
/**
 * A marker annotation, written on a class declaration, that signifies that
 * one or more of the class's type parameters can be treated covariantly.
 * For example, if <tt>MyClass</tt> has a single type parameter that is
 * treated covariantly, and if <tt>B</tt> is a subtype of <tt>A</tt>, then
 * <tt>SomeClass&lt;B&gt;</tt> is a subtype of <tt>SomeClass&lt;B&gt;</tt>.
 * <p>
 *
 * Ordinarily, Java treats type parameters invariantly:
 * <tt>SomeClass&lt;B&gt;</tt> is unrelated to (neither a subtype nor a
 * supertype of) <tt>SomeClass&lt;A&gt;</tt>.
 * <p>
 *
 * It is only safe to mark a type parameter as covariant if the type
 * parameter is used in a read-only way:  values of that type are read from
 * but never modified.  This property is not checked; the
 * <tt>@Covariant</tt> is simply trusted.
 * <p>
 *
 * The argument to <tt>@Covariant</tt> is the zero-based indices of the 
 * type parameters that should be treated covariantly.
 *
 * @checker_framework.manual #covariant-type-parameters Covariant type parameters
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Covariant {
  int[] value();
}
