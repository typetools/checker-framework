package org.checkerframework.checker.experimental.tainting_qual_poly.qual;

import java.lang.annotation.*;

import org.checkerframework.checker.experimental.tainting_qual_poly.TaintingAnnotationConverter;
import org.checkerframework.qualframework.poly.qual.Wildcard;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Repeatable(MultiTainted.class)
public @interface Tainted {
    String param() default TaintingAnnotationConverter.PRIMARY_TARGET;
    Wildcard wildcard() default Wildcard.NONE;
}
