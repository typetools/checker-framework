package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that indicates that an annotation E is a conditional postcondition annotation,
 * i.e., E is a type-specialized version of {@link EnsuresQualifierIf}. The value {@code qualifier}
 * that is necessary for a conditional postcondition specified with {@link EnsuresQualifierIf} is
 * specified here with the value {@code qualifier}.
 *
 * <p>The annotation E that is meta-annotated as {@link ConditionalPostconditionAnnotation} must
 * have an element called {@code expression} that is an array of {@code String}s of the same format
 * and with the same meaning as the value {@code expression} in {@link EnsuresQualifierIf}. E must
 * also have an element {@code result} with the same meaning as the element {@code result} in {@link
 * EnsuresQualifierIf}.
 *
 * <p>The established postcondition P has type specified by the {@code qualifier} field of this
 * annotation. If the annotation E has elements annotated by {@link QualifierArgument}, their values
 * are copied to the arguments (elements) of annotation P with the same names. Different element
 * names may be used in E and P, if a {@link QualifierArgument} in E gives the name of the
 * corresponding element in P.
 *
 * <p>For example, the following code declares a postcondition annotation for the {@link
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
 * @see EnsuresQualifier
 * @see QualifierArgument
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalPostconditionAnnotation {
    /** The qualifier that will be established as a postcondition. */
    Class<? extends Annotation> qualifier();
}
