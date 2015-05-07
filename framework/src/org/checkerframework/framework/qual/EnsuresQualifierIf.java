package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A conditional postcondition annotation to indicate that a method ensures that
 * certain expressions have a certain qualifier once the method has finished,
 * and if the result is as indicated by {@code result}. The expressions for
 * which the annotation must hold after the methods execution are indicated by
 * {@code expression} and are specified using a string. The qualifier is
 * specified by {@code qualifier}.
 *
 * <p>
 * This annotation is only applicable to methods with a boolean return type.
 *
 * @author Stefan Heule
 * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
@InheritedAnnotation
public @interface EnsuresQualifierIf {
    /**
     * The Java expressions for which the qualifier holds if the method
     * terminates with return value {@link #result()}.
     *
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    String[] expression();

    /**
     * The qualifier that is guaranteed to hold if the method terminates with
     * return value {@link #result()}.
     */
    Class<? extends Annotation> qualifier();

    /**
     * The return value of the method that needs to hold for the postcondition
     * to hold.
     */
    boolean result();
}
