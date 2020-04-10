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
@Repeatable(RequiresQualifier.List.class)
public @interface RequiresQualifier {
    /**
     * @return the Java expressions for which the annotation need to be present
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    String[] expression();

    /** @return the qualifier that is required */
    Class<? extends Annotation> qualifier();

    /**
     * A wrapper annotation that makes the {@link RequiresQualifier} annotation repeatable.
     *
     * <p>Programmers generally do not need to write this. It is created by Java when a programmer
     * writes more than one {@link RequiresQualifier} annotation at the same location.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
    @interface List {
        /** @return the repeatable annotations */
        RequiresQualifier[] value();
    }
}
