package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A precondition annotation to indicate that a method requires certain expressions to have a
 * certain qualifier at the time of the call to the method. The expressions for which the annotation
 * must hold after the method's execution are indicated by {@code expression} and are specified
 * using a string. The qualifier is specified by {@code qualifier}.
 *
 * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(RequiresQualifiers.class)
public @interface RequiresQualifier {
    /**
     * The Java expressions for which the annotation need to be present.
     *
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    String[] expression();

    /** The qualifier that is required. */
    Class<? extends Annotation> qualifier();
}
