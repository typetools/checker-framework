package org.checkerframework.checker.tainting.qual;

import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;
import org.checkerframework.qualframework.poly.qual.Wildcard;

import java.lang.annotation.*;

/**
 * Var is a qualifier parameter use.
 *
 * <pre>
 * {@code
 *
 *  {@literal @}TaintingParam("aParam")
 *  class A {
 *  }
 *
 *  {@literal @}TaintingParam("bParam")
 *  class B {
 *    {@literal @}Var(arg="bParam", param="aParam") A f1;
 *    {@literal @}Var(arg="bParam", param="aParam", wildcard=Wildcard.EXTENDS) A f2;
 *  }
 * }
 * </pre>
 *  is equivalent to:
 * <pre>
 *
 * {@code
 *  class A&laquo;aParam&raquo; {
 *
 *  }
 *  class B&laquo;bParam&raquo; {
 *    A&laquo;aParam=bParam&raquo; a;
 *    A&laquo;aParam=? extends bParam&raquo; b;
 *  }
 *
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Repeatable(MultiVar.class)
public @interface Var {
    /**
     * Which parameter this @Var is a use of.
     */
    String arg()  default SimpleQualifierParameterAnnotationConverter.PRIMARY_TARGET;

    /**
     * The name of the parameter to set.
     */
    String param() default SimpleQualifierParameterAnnotationConverter.PRIMARY_TARGET;

    /**
     * Specify that this use is a wildcard with a bound.
     */
    Wildcard wildcard() default Wildcard.NONE;
}
