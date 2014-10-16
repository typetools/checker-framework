package org.checkerframework.checker.experimental.tainting_qual_poly.qual;

import java.lang.annotation.*;

import org.checkerframework.checker.experimental.tainting_qual_poly.TaintingAnnotationConverter;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(MultiMethodTaintingParam.class)
public @interface MethodTaintingParam {
    String value() default TaintingAnnotationConverter.PRIMARY_TARGET;
}
