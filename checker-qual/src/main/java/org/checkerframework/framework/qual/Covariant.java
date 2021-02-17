package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker annotation, written on a class declaration, that signifies that one or more of the
 * class's type parameters can be treated covariantly. For example, if {@code MyClass} has a single
 * type parameter that is treated covariantly, and if {@code B} is a subtype of {@code A}, then
 * {@code SomeClass<B>} is a subtype of {@code SomeClass<A>}.
 *
 * <p>Ordinarily, Java treats type parameters invariantly: {@code SomeClass<B>} is unrelated to
 * (neither a subtype nor a supertype of) {@code SomeClass<A>}.
 *
 * <p>It is only safe to mark a type parameter as covariant if clients use the type parameter in a
 * read-only way: clients read values of that type but never modify them.
 *
 * <p>This property is not checked; the {@code @Covariant} is simply trusted.
 *
 * <p>Here is an example use:
 *
 * <pre>{@code @Covariant(0)
 * public interface Iterator<E extends @Nullable Object> { ... }
 * }</pre>
 *
 * @checker_framework.manual #covariant-type-parameters Covariant type parameters
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Covariant {
    /** The zero-based indices of the type parameters that should be treated covariantly. */
    int[] value();
}
