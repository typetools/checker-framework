package org.checkerframework.checker.experimental.tainting_qual_poly.qual;

import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;
import org.checkerframework.qualframework.poly.qual.Wildcard;

import java.lang.annotation.*;

/**
 * Var is a qualifier parameter use.
 *
 * <pre>
 *  @TaintingParam("aParam")
 *  class A {
 *  }
 *
 *  @TaintingParam("bParam")
 *  class B {
 *    @Var(arg="bParam", param="aParam") A f1;
 *    @Var(arg="bParam", param="aParam", wildcard=Wildcard.EXTENDS) A f2;
 *  }
 *
 *  is equivalent to:
 *  class A<<aParam>> {
 *
 *  }
 *  class B<<bParam>> {
 *    A<<aParam=bParam>> a;
 *    A<<aParam=? extends bParam>> b;
 *  }
 *
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Repeatable(MultiVar.class)
public @interface Var {
    // Specify which parameter this @Var is a use for.
    String arg()  default SimpleQualifierParameterAnnotationConverter.PRIMARY_TARGET;
    // The name of the parameter to set.
    String param() default SimpleQualifierParameterAnnotationConverter.PRIMARY_TARGET;
    // Specify a wildcard with a bound.
    Wildcard wildcard() default Wildcard.NONE;
}
