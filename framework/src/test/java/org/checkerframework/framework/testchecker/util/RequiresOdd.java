package org.checkerframework.framework.testchecker.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PreconditionAnnotation;

/**
 * A precondition annotation to indicate that a method requires certain expressions to be {@link
 * Odd}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@PreconditionAnnotation(qualifier = Odd.class)
public @interface RequiresOdd {
    String[] value();
}
