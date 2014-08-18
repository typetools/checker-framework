package org.checkerframework.checker.experimental.tainting_qual_poly.qual;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Repeatable(MultiUntainted.class)
public @interface Untainted {
    String target() default "Main";
}

