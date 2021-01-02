package org.checkerframework.framework.testchecker.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.InheritedAnnotation;

/**
 * A conditional postcondition annotation to indicate that a method ensures certain expressions to
 * be {@link Odd} given a certain result (either true or false).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@ConditionalPostconditionAnnotation(qualifier = Odd.class)
@InheritedAnnotation
public @interface EnsuresOddIf {
    String[] expression();

    boolean result();
}
