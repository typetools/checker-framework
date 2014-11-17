package org.checkerframework.checker.experimental.tainting_qual_poly.qual;

import java.lang.annotation.*;

import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface MultiVar {
    Var[] value();
}
