package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A polymorphic type qualifier that varies over all type hierarchies. Writing {@code @PolyAll} is
 * equivalent to writing a polymorphic qualifier for every type system.
 *
 * <p>The {@code @PolyAll} annotation applies to every type qualifier hierarchy for which no
 * explicit qualifier is written. For example, a declaration like {@code @PolyAll @NonNull String s}
 * is polymorphic over every type system <em>except</em> the nullness type system, for which the
 * type is fixed at {@code @NonNull}.
 * <!-- TODO: uncomment when this is implemented:
 * <p>
 * The optional argument creates conceptually distinct polymorphic
 * qualifiers, such as {@code @PolyAll(1)} and {@code @PolyAll(2)}.
 * These two qualifierrs can vary independently.  When a method has
 * multiple occurrences of a single polymorphic qualifier, all of the
 * occurrences with the same argument (or with no argument) vary together.
 * -->
 *
 * <p>Implementation note: {@code @PolyAll} only works for a given type system if that type system
 * already has its own polymorphic qualifier, such as {@code @PolyNull} or {@code @PolyRegex}.
 * Therefore, every type system should define a polymorphic qualifier. Then, to support
 * {@code @PolyAll} in a type system, simply add it to the list of supported type qualifiers.
 *
 * @see org.checkerframework.checker.nullness.qual.PolyNull
 * @see org.checkerframework.checker.interning.qual.PolyInterned
 * @see org.checkerframework.framework.util.QualifierPolymorphism
 * @checker_framework.manual #polyall The @PolyAll qualifier applies to every type system
 */
@Documented
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@PolymorphicQualifier
public @interface PolyAll {
    // TODO: support multiple variables using an id, then uncomment some Javadoc
    // int value() default 0;
}
