package org.checkerframework.checker.tainting.qual;

import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MethodTaintingParam declares a qualifier parameter on a method.
 *
 * <pre>
 * {@code
 *  {@literal @}MethodTaintingParam("aParam") void foo() { }
 *
 *  is equivalent to:
 *
 *  &laquo;aParam&raquo; void food() { }
 * }
 * </pre>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(MultiMethodTaintingParam.class)
public @interface MethodTaintingParam {
    /**
     * The name of the qualifier parameter to declare.
     */
    String value() default SimpleQualifierParameterAnnotationConverter.PRIMARY_TARGET;
}
