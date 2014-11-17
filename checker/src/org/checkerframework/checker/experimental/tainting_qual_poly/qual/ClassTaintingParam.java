package org.checkerframework.checker.experimental.tainting_qual_poly.qual;

import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ClassTaintingParam declares a tainting qualifier parameter on a class.
 *
 * <pre>
 * {@code
 *  {@literal @}ClassTaintingParam("aParam")
 *  class A { }
 *
 *  is equivalent to:
 *  class A<<aParam>> { }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(MultiClassTaintingParam.class)
public @interface ClassTaintingParam {
    String value() default SimpleQualifierParameterAnnotationConverter.PRIMARY_TARGET;
}
