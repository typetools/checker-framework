package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that indicates that an annotation R is a precondition annotation, i.e., R is a
 * type-specialized version of {@link RequiresQualifier}. The value {@code qualifier} that is
 * necessary for a precondition specified with {@link RequiresQualifier} is specified here with the
 * value {@code qualifier}.
 *
 * <p>The annotation R that is meta-annotated as {@link PreconditionAnnotation} must have an element
 * called {@code value} that is an array of {@code String}s of the same format and with the same
 * meaning as the value {@code expression} in {@link RequiresQualifier}.
 *
 * <p>The established precondition P has type specified by the {@code qualifier} field of this
 * annotation. If the annotation P has arguments (elements), their values are copied from the
 * elements of annotation R with the same names. Different element names may be used in R and P, if
 * the element in R is annotated by {@link QualifierArgument} which gives the corresponding element
 * in P.
 *
 * <p>For example, the following code declares a precondition annotation for the {@link
 * org.checkerframework.common.value.qual.MinLen} qualifier:
 *
 * <pre><code>
 * {@literal @}PreconditionAnnotation(qualifier = MinLen.class)
 * {@literal @}Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
 * public {@literal @}interface RequiresMinLen {
 *   String[] value();
 *   {@literal @}QualifierArgument("value")
 *   int targetValue() default 0;
 * </code></pre>
 *
 * The {@code value} element holds the expressions to which the qualifier applies and {@code
 * targetValue} holds the value for the {@code value} argument of {@link
 * org.checkerframework.common.value.qual.MinLen}.
 *
 * <p>The following code then uses the annotation on a method that requires {@code field} to be
 * {@code @MinLen(2)} upon entry.
 *
 * <pre><code>
 * {@literal @}RequiresMinLen(value = "field", targetValue = 2")
 * public char getThirdCharacter() {
 *   return field.charAt(2);
 * }
 * </code></pre>
 *
 * @author Stefan Heule
 * @see RequiresQualifier
 * @see QualifierArgument
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PreconditionAnnotation {
    /** The qualifier that must be established as a precondition. */
    Class<? extends Annotation> qualifier();
}
