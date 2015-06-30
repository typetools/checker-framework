package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A postcondition annotation to indicate that a method ensures certain
 * expressions to have a certain type qualifier once the method has successfully
 * terminated. The expressions for which the qualifier must hold after the
 * methods execution are indicated by {@code expression} and are specified using
 * a string. The qualifier is specified by {@code qualifier}.
 *
 * @author Stefan Heule
 * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
@InheritedAnnotation
public @interface EnsuresQualifier {
    /**
     * The Java expressions for which the qualifier holds after successful
     * method termination.
     *
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    String[] expression();

    /**
     * The qualifier that is guaranteed to hold on successfull termination of
     * the method.
     */
    Class<? extends Annotation> qualifier();
}
