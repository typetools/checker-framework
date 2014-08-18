package org.checkerframework.checker.experimental.tainting_qual_poly.qual;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(MultiTaintingParam.class)
public @interface TaintingParam {
    String value();
}
