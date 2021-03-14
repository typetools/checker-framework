package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation applied to the declaration of a type qualifier. It specifies that the
 * annotation should be the upper bound for
 *
 * <ul>
 *   <li>all uses of a particular type, and
 *   <li>all uses of a particular kind of type.
 * </ul>
 *
 * An example is the declaration
 *
 * <pre><code>
 * {@literal @}DefaultFor(classes=String.class)
 * {@literal @}interface MyAnno {}
 * </code></pre>
 *
 * <p>The upper bound applies to every occurrence of the given classes and also to every occurrence
 * of the given type kinds.
 *
 * @checker_framework.manual #upper-bound-for-use Upper bound of qualifiers on uses of a given type
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface UpperBoundFor {
  /**
   * Returns {@link TypeKind}s of types that get an upper bound. The meta-annotated annotation is
   * the upper bound.
   *
   * @return {@link TypeKind}s of types that get an upper bound
   */
  TypeKind[] typeKinds() default {};

  /**
   * Returns {@link Class}es that should get an upper bound. The meta-annotated annotation is the
   * upper bound.
   *
   * @return {@link Class}es that get an upper bound
   */
  Class<?>[] types() default {};
}
