package org.checkerframework.checker.tainting.qual;

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
 *  &#064;ClassTaintingParam("aParam")
 *  class A { }
 *
 *  is equivalent to:
 *  class A&laquo;aParam&raquo; { }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(MultiClassTaintingParam.class)
public @interface ClassTaintingParam {
    /**
     * The name of the qualifier parameter to declare.
     */
    String value() default SimpleQualifierParameterAnnotationConverter.PRIMARY_TARGET;
}
