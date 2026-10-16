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
 * hierarchy can be defined procedurally by subclassing {@code QualifierHierarchy} or {@code
 * TypeHierarchy}.)
 *
 * <p>Example:
 *
 * <pre> @SubtypeOf( { Nullable.class } )
 * public @interface NonNull {}
 * </pre>
 *
 * <p>For the top qualifier in the qualifier hierarchy (i.e., the qualifier that is a supertype of
 * all other qualifiers in the given hierarchy), use an empty set of values:
 *
 * <pre><code> @SubtypeOf( {} )
 * public @interface Nullable {}
 *
 * &#64;SubtypeOf( {} )
 * public @interface MaybeAliased {}
 * </code></pre>
 *
 * <p>Together, all the {@code @SubtypeOf} meta-annotations fully describe the type qualifier
 * hierarchy.
 *
 * @checker_framework.manual #creating-declarative-hierarchy Declaratively defining the qualifier
 *     hierarchy
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@AnnotatedFor("nullness")
public @interface SubtypeOf {
  /** An array of the supertype qualifiers of the annotated qualifier. */
  Class<? extends Annotation>[] value();
}
