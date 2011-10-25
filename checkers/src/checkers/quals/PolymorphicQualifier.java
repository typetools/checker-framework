package checkers.quals;

import java.lang.annotation.*;

import checkers.util.QualifierPolymorphism;
import checkers.nullness.quals.PolyNull;
import checkers.interning.quals.PolyInterned;

/**
 * A meta-annotation that indicates that an annotation is a polymorphic type
 * qualifier.
 *
 * <p>
 * Any method written using a polymorphic type qualifier conceptually has
 * two or more versions &mdash; one version for each qualifier in the
 * qualifier hierarchy.  In each version of the method, all instances of
 * the polymorphic type qualifier are replaced by one of the other type
 * qualifiers.
 *
 * @see PolyNull
 * @see PolyInterned
 * @see QualifierPolymorphism
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PolymorphicQualifier {}
