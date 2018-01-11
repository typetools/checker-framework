package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that indicates that an annotation is a conditional postcondition annotation,
 * i.e., a type-specialized version of {@link EnsuresQualifierIf}. The annotation that is annotated
 * as {@link ConditionalPostconditionAnnotation} must have a value called {@code expression} that is
 * an array of {@code String}s of the same format and with the same meaning as the value {@code
 * expression} in {@link EnsuresQualifierIf}, as well as a value {@code result} with the same
 * meaning as the value {@code result} in {@link EnsuresQualifierIf}.
 *
 * <p>The value {@code qualifier} that is necessary for a conditional postcondition specified with
 * {@link EnsuresQualifier} is hard-coded here with the value {@code qualifier}.
 *
 * <p>Additionally, the elements of the postcondition annotation (annotated by this meta-annotation)
 * can be used to specify values of arguments of the qualifier. Each such element must be annotated
 * by {@link QualifierArgument}, with a value specifying the name of the target qualifier argument.
 * If no value is specified, the name of the element is used. The element must have the same type as
 * the element with the specified name in the qualifier annotation.
 *
 * <p>* For example, the following code declares a postcondition annotation for the {@link
 * org.checkerframework.common.value.qual.MinLen} qualifier:
 *
 * <pre><code>
 * {@literal @}ConditionalPostconditionAnnotation(qualifier = MinLen.class)
 * {@literal @}Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
 * public {@literal @}interface EnsuresMinLen {
 *   String[] expression();
 *   boolean result();
 *   {@literal @}QualifierArgument("value")
 *   int targetValue() default 0;
 * </code></pre>
 *
 * The {@code expression} element holds the expressions to which the qualifier applies and {@code
 * targetValue} holds the value for the {@code value} argument of {@link
 * org.checkerframework.common.value.qual.MinLen}.
 *
 * <p>The following code then uses the annotation on a method that ensures {@code field} to be
 * {@code @MinLen(4)} upon returning {@code true}.
 *
 * <pre><code>
 * {@literal @}EnsuresMinLenIf(expression = "field", result = true, targetValue = 4")
 * public boolean isFieldBool() {
 *   return field == "true" || field == "false";
 * }
 * </code></pre>
 *
 * @author Stefan Heule
 * @see EnsuresQualifier
 * @see QualifierArgument
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalPostconditionAnnotation {
    /** The hard-coded qualifier for the postcondition. */
    Class<? extends Annotation> qualifier();
}
