package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that indicates that an annotation E is a postcondition annotation, i.e., E is a
 * type-specialized version of {@link EnsuresQualifier} or of {@link EnsuresQualifier.List}.
 *
 * <ul>
 *   <li>If E is a type-specialized version of {@link EnsuresQualifier}, its {@code value} element
 *       must be an array of {@code String}s, analogous to {@link EnsuresQualifier#expression()}.
 *   <li>If E is a type-specialized version of {@link EnsuresQualifier.List}, its {@code value}
 *       element must be an array of postcondition annotations, analogous to {@link
 *       EnsuresQualifier.List#value()}.
 * </ul>
 *
 * <p>The established postcondition P has type specified by the {@code qualifier} field of this
 * annotation.
 *
 * <p>If the annotation E has elements annotated by {@link QualifierArgument}, their values are
 * copied to the arguments (elements) of annotation P with the same names. Different element names
 * may be used in E and P, if a {@link QualifierArgument} in E gives the name of the corresponding
 * element in P.
 *
 * <p>For example, the following code declares a postcondition annotation for the {@link
 * org.checkerframework.common.value.qual.MinLen} qualifier:
 *
 * <pre><code>
 * {@literal @}PostconditionAnnotation(qualifier = MinLen.class)
 * {@literal @}Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
 *  public {@literal @}interface EnsuresMinLen {
 *    String[] value();
 *    {@literal @}QualifierArgument("value")
 *    int targetValue() default 0;
 * </code></pre>
 *
 * The {@code value} element holds the expressions to which the qualifier applies and {@code
 * targetValue} holds the value for the {@code value} argument of {@link
 * org.checkerframework.common.value.qual.MinLen}.
 *
 * <p>The following code then uses the annotation on a method that ensures {@code field} to be
 * {@code @MinLen(2)} upon return.
 *
 * <pre><code>
 * {@literal @}EnsuresMinLen(value = "field", targetValue = 2")
 *  public void setField(String argument) {
 *    field = "(" + argument + ")";
 *  }
 * </code></pre>
 *
 * @see EnsuresQualifier
 * @see QualifierArgument
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface PostconditionAnnotation {
    /**
     * The qualifier that will be established as a postcondition.
     *
     * <p>This element is analogous to {@link EnsuresQualifier#qualifier()}.
     */
    Class<? extends Annotation> qualifier();
}
