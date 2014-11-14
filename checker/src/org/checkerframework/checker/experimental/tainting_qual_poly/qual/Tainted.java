package org.checkerframework.checker.experimental.tainting_qual_poly.qual;

import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;
import org.checkerframework.qualframework.poly.qual.Wildcard;

import java.lang.annotation.*;

/**
 * Tainted is the annotation to specify the tainted qualifier.
 *
 * <pre>
 *  @TaintingParam("aParam")
 *  class A {
 *  }
 *
 *  class B {
 *    @Tainted(param="aParam") A f1;
 *    @Tainted(param="aParam", wildcard=Wildcard.EXTENDS) A f2;
 *  }
 *
 *  is equivalent to:
 *  class A<<aParam>> {
 *
 *  }
 *  class B {
 *    A<<aParam=@Tainted>> a;
 *    A<<aParam=? extends @Tainted>> b;
 *  }
 *
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Repeatable(MultiTainted.class)
public @interface Tainted {
    String param() default SimpleQualifierParameterAnnotationConverter.PRIMARY_TARGET;
    Wildcard wildcard() default Wildcard.NONE;
}
