package org.checkerframework.framework.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to use on an element of a contract annotation to specify that the element specifies
 * the value of an argument of the qualifier. (The qualifier is specified in a meta-annotation
 * placed on the contract annotation).
 *
 * <p>If the name of the argument is not specified (or is ""), then the name of the annotated
 * element is used as the argument name.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QualifierArgument {
    /**
     * Specifies the name of the argument of the qualifier, that is passed the values specified in
     * the annotated element.
     */
    String value() default "";
}
