package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation to allow multiple conditional postcondition annotations.
 *
 * @see EnsuresQualifierIf
 * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@InheritedAnnotation
public @interface EnsuresQualifiersIf {
    EnsuresQualifierIf[] value();
}
