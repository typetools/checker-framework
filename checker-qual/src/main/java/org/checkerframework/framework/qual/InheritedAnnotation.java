package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that specifies if a declaration annotation should be inherited. This should not
 * be written on type annotations. Unlike, {@link java.lang.annotation.Inherited}, this
 * meta-annotation type will have cause the annotated type to be inherited even if it is used to
 * annotate something other than a class.
 *
 * <p>The Checker Framework does respect {@link java.lang.annotation.Inherited} and will only
 * inherit declaration annotations on a class if the class is annotated with an annotated type with
 * this meta-annotation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface InheritedAnnotation {}
