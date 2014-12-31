package org.checkerframework.checker.experimental.regex_qual_poly.qual;

import org.checkerframework.checker.experimental.tainting_qual_poly.qual.ClassTaintingParam;
import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * ClassRegexParam declares a regex qualifier parameter on a class.
 *
 * @see ClassTaintingParam
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(MultiClassRegexParam.class)
public @interface ClassRegexParam {
    String value() default SimpleQualifierParameterAnnotationConverter.PRIMARY_TARGET;
}
