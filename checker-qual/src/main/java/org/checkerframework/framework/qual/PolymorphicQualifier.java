package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that indicates that an annotation is a polymorphic type qualifier.
 *
 * <p>Any method written using a polymorphic type qualifier conceptually has two or more versions
 * &mdash; one version for each qualifier in the qualifier hierarchy. In each version of the method,
 * all instances of the polymorphic type qualifier are replaced by one of the other type qualifiers.
 *
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
@AnnotatedFor("nullness")
public @interface PolymorphicQualifier {
  /**
   * Indicates which type system this annotation refers to (optional, and usually unnecessary). When
   * multiple type hierarchies are supported by a single type system, then each polymorphic
   * qualifier needs to indicate which sub-hierarchy it belongs to. Do so by passing the top
   * qualifier from the given hierarchy.
   *
   * @return the top qualifier in the hierarchy of this qualifier
   */
  // We use the meaningless Annotation.class as default value and
  // then ensure there is a single top qualifier to use.
  Class<? extends Annotation> value() default Annotation.class;
}
