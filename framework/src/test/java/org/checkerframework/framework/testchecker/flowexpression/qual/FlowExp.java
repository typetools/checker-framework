package org.checkerframework.framework.testchecker.flowexpression.qual;

import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@SubtypeOf({FETop.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface FlowExp {
    @JavaExpression
    String[] value() default {};
}
