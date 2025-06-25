package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that specifies that a declaration annotation should be inherited. This should
 * not be written on type annotations. Unlike {@link java.lang.annotation.Inherited}, this
 * meta-annotation causes the declaration annotation to be inherited even if it is used to annotate
 * something other than a class.
 *
 * <p>The Checker Framework does respect {@link java.lang.annotation.Inherited} and will only
 * inherit declaration annotations on a class if the declaration annotation class is meta-annotated
 * with {@code InheritedAnnotation}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface InheritedAnnotation {}
