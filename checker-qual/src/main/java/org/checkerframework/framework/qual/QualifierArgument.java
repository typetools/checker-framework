package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to use on an element of a contract annotation to indicate that the element
 * specifies the value of an argument of the qualifier. A contract annotation is an annotation
 * declared with a {@link PreconditionAnnotation}, {@link PostconditionAnnotation}, or {@link
 * ConditionalPostconditionAnnotation} meta-annotation. The meta-annotation specifies the qualifier
 * taking the arguments.
 *
 * <p>For example, the following code declares a postcondition annotation for the {@link
 * org.checkerframework.common.value.qual.MinLen} qualifier, allowing to specify its value:
 *
 * <pre><code>
 * {@literal @}PostconditionAnnotation(qualifier = MinLen.class)
 * {@literal @}Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
 * public {@literal @}interface EnsuresMinLen {
 *   String[] value();
 *   {@literal @}QualifierArgument("value")
 *   int targetValue() default 0;
 * </code></pre>
 *
 * The {@code value} element holds the expressions to which the qualifier applies and {@code
 * targetValue} holds the value for the {@code value} argument of {@link
 * org.checkerframework.common.value.qual.MinLen}.
 *
 * @see PostconditionAnnotation
 * @see ConditionalPostconditionAnnotation
 * @see PreconditionAnnotation
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface QualifierArgument {
    /**
     * Specifies the name of the argument of the qualifier, that is passed the values held in the
     * annotated element. If the value is omitted or is empty, then the name of the annotated
     * element is used as the argument name.
     */
    String value() default "";
}
