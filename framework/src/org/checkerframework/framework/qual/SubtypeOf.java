package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation to specify all the qualifiers that the given qualifier is an immediate subtype
 * of. This provides a declarative way to specify the type qualifier hierarchy. (Alternatively, the
 * hierarchy can be defined procedurally by subclassing {@link
 * org.checkerframework.framework.type.QualifierHierarchy} or {@link
 * org.checkerframework.framework.type.TypeHierarchy}.)
 *
 * <p>Example:
 *
 * <pre> @SubtypeOf( { Nullable.class } )
 * public @interface NonNull { }
 * </pre>
 *
 * <p>If a qualified type is a subtype of the same type without any qualifier, then use {@code
 * Unqualified.class} in place of a type qualifier class. For example, to express that
 * {@code @Encrypted C} is a subtype of {@code C} (for every class {@code C}), and likewise for
 * {@code @Interned}, write:
 *
 * <pre><code> @SubtypeOf(Unqualified.class)
 * public @interface Encrypted { }
 *
 * &#64;SubtypeOf(Unqualified.class)
 * public @interface Interned { }
 * </code></pre>
 *
 * <p>For the top qualifier in the qualifier hierarchy (i.e., the qualifier that is a supertype of
 * all other qualifiers in the given hierarchy), use an empty set of values:
 *
 * <pre><code> @SubtypeOf( { } )
 * public @interface Nullable { }
 *
 * &#64;SubtypeOf( {} )
 * public @interface MaybeAliased { }
 * </code></pre>
 *
 * <p>Together, all the @SubtypeOf meta-annotations fully describe the type qualifier hierarchy.
 *
 * <p>No @SubtypeOf meta-annotation is needed on (or can be written on) the Unqualified
 * pseudo-qualifier, whose position in the hierarchy is inferred from the meta-annotations on the
 * explicit qualifiers.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface SubtypeOf {
    /** An array of the supertype qualifiers of the annotated qualifier */
    Class<? extends Annotation>[] value();
}
