package checkers.nullness.quals;

import java.lang.annotation.*;

/**
 * A marker annotation, written on a class declaration, that signifies that
 * one or more of the class's type parameters can be treated covariantly.
 * For example, if <tt>MyClass</tt> has a single type parameter that is
 * treated covariantly, and if <tt>A</tt> is a subtype of <tt>B</tt>, then
 * <tt>SomeClass&lt;A&gt;</tt> is a subtype of <tt>SomeClass&lt;B&gt;</tt>.
 * <p>
 *
 * Ordinarily, Java treats type parameters invariantly:
 * <tt>SomeClass&lt;A&gt;</tt> is unrelated to (neither a subtype nor a
 * supertype of) <tt>SomeClass&lt;B&gt;</tt>.
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
 * <!-- TODO: move to checkers.quals package. -->
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Covariant {
  int[] value();
}
