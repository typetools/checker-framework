package org.checkerframework.checker.experimental.tainting_qual_poly.qual;

import java.lang.annotation.*;

import org.checkerframework.checker.experimental.tainting_qual_poly.TaintingAnnotationConverter;
import org.checkerframework.checker.experimental.tainting_qual_poly.TaintingChecker;
import org.checkerframework.framework.qual.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(MultiTaintingParam.class)
public @interface TaintingParam {
    String value() default TaintingAnnotationConverter.PRIMARY_TARGET;
}
