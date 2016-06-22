package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.*;

// TODO: move to org.checkerframework.framework.qual package.
/**
 * A marker annotation, written on a class declaration, that signifies that
 * one or more of the class's type parameters can be treated covariantly.
 * For example, if {@code MyClass} has a single type parameter that is
 * treated covariantly, and if {@code B} is a subtype of {@code A}, then
 * {@code SomeClass<B>} is a subtype of {@code SomeClass<B>}.
 * <p>
 *
 * Ordinarily, Java treats type parameters invariantly:
 * {@code SomeClass<B>} is unrelated to (neither a subtype nor a
 * supertype of) {@code SomeClass<A>}.
 * <p>
 *
 * It is only safe to mark a type parameter as covariant if the type
 * parameter is used in a read-only way:  values of that type are read from
 * but never modified.  This property is not checked; the
 * {@code @Covariant} is simply trusted.
 * <p>
 *
 * @checker_framework.manual #covariant-type-parameters Covariant type parameters
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Covariant {
  /**
   * The zero-based indices of the type parameters that should be treated
   * covariantly.
   */
  int[] value();
}
