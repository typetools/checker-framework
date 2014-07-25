package org.checkerframework.checker.experimental.tainting_qual_poly.qual;

import java.lang.annotation.*;

import org.checkerframework.checker.experimental.tainting_qual_poly.TaintingChecker;
import org.checkerframework.framework.qual.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Repeatable(MultiWild.class)
public @interface Wild {
    String target() default "Main";
}
