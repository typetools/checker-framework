package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that specifies if a declaration annotation should be inherited. This should not
 * be written on type annotations.
 *
 * <p>{@link java.lang.annotation.Inherited} applies only to declaration annotations on a class.
 * {@code InheritedAnnotation} applys to both classes and methods. (The Checker Framework also
 * respects {@code Inherited}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface InheritedAnnotation {}
