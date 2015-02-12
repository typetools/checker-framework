package org.checkerframework.checker.regex.qual;

import org.checkerframework.checker.tainting.qual.MethodTaintingParam;
import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * MethodRegexParam declares a qualifier parameter on a method.
 *
 * @see MethodTaintingParam
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(MultiMethodRegexParam.class)
public @interface MethodRegexParam {
    /**
     * The name of the qualifier parameter to declare.
     */
    String value() default SimpleQualifierParameterAnnotationConverter.PRIMARY_TARGET;
}
