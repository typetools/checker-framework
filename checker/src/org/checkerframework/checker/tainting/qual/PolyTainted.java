package org.checkerframework.checker.tainting.qual;

import java.lang.annotation.*;
import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Repeatable(MultiPolyTainted.class)
public @interface PolyTainted {
    /**
     * The name of the qualifier parameter to set.
     */
    String param() default SimpleQualifierParameterAnnotationConverter.PRIMARY_TARGET;
}
